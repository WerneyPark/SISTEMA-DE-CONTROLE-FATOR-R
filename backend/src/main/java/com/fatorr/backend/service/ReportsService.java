package com.fatorr.backend.service;

import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.domain.LaudoTributario;
import com.fatorr.backend.domain.OrganizacaoContabil;
import com.fatorr.backend.domain.Usuario;
import com.fatorr.backend.dto.FaturamentoHistoricoDTO;
import com.fatorr.backend.dto.ParametrosEmissaoLaudoDTO;
import com.fatorr.backend.dto.ProjecaoCenarioDTO;
import com.fatorr.backend.dto.SimulacaoTributariaDTO;
import com.fatorr.backend.dto.ValidacaoLaudoResponseDTO;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.repository.LaudoTributarioRepository;
import com.fatorr.backend.repository.OrganizacaoContabilRepository;
import com.fatorr.backend.repository.UsuarioRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportsService {

    private final CalculoFatorRService calculoFatorRService;
    private final EmpresaRepository empresaRepository;
    private final OrganizacaoContabilRepository organizacaoContabilRepository;
    private final UsuarioRepository usuarioRepository;
    private final LaudoTributarioRepository laudoTributarioRepository;

    private static final Locale LOCALE_BR = Locale.of("pt", "BR");
    private static final NumberFormat MOEDA_FORMAT = NumberFormat.getCurrencyInstance(LOCALE_BR);
    private static final NumberFormat PERCENTUAL_FORMAT = NumberFormat.getNumberInstance(LOCALE_BR);

    static {
        PERCENTUAL_FORMAT.setMinimumFractionDigits(2);
        PERCENTUAL_FORMAT.setMaximumFractionDigits(2);
    }

    private final Path storageDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "fatorr-laudos");

    /**
     * Emissão unificada de laudo (PDF ou Excel)
     */
    public byte[] emitirLaudo(ParametrosEmissaoLaudoDTO params, String usuarioId) {
        Empresa empresa = buscarEmpresa(params.getEmpresaId());
        YearMonth competencia = YearMonth.parse(params.getCompetencia());
        SimulacaoTributariaDTO simulacao = calculoFatorRService.simularTributacao(empresa.getId(), competencia);
        OrganizacaoContabil contabilidade = buscarContabilidade(empresa);
        Usuario usuario = buscarUsuario(usuarioId);

        List<ProjecaoCenarioDTO> projecoes = null;
        if (params.isIncluirProjecaoCenarios()) {
            projecoes = calculoFatorRService.simularProximosMeses(empresa.getId(), competencia);
        }

        String laudoId = UUID.randomUUID().toString();
        String hashInput = empresa.getCnpj() + "|" + params.getCompetencia() + "|" +
                params.getTipoLaudo() + "|" + simulacao.getPercentualFatorR() + "|" +
                laudoId + "|" + System.currentTimeMillis();
        String hashAutenticidade = calcularHashSha256(hashInput.getBytes(StandardCharsets.UTF_8));

        byte[] documentoBytes;
        String formato = params.getFormato() != null ? params.getFormato().toUpperCase() : "PDF";

        if ("EXCEL".equals(formato)) {
            documentoBytes = gerarExcelLaudo(empresa, contabilidade, usuario, simulacao, projecoes, params, hashAutenticidade);
            salvarRegistroLaudo(laudoId, empresa.getId().toString(), usuarioId, params.getCompetencia(),
                    params.getTipoLaudo(), hashAutenticidade, "xlsx", documentoBytes);
        } else {
            documentoBytes = gerarPdfLaudo(empresa, contabilidade, usuario, simulacao, projecoes, params, hashAutenticidade);
            salvarRegistroLaudo(laudoId, empresa.getId().toString(), usuarioId, params.getCompetencia(),
                    params.getTipoLaudo(), hashAutenticidade, "pdf", documentoBytes);
        }

        return documentoBytes;
    }

    /**
     * Download direto do Excel da competência
     */
    public byte[] emitirExcelCompetencia(String empresaId, String competenciaStr) {
        ParametrosEmissaoLaudoDTO params = ParametrosEmissaoLaudoDTO.builder()
                .empresaId(empresaId)
                .competencia(competenciaStr)
                .tipoLaudo("MEMORIA_CALCULO_12M")
                .formato("EXCEL")
                .incluirProjecaoCenarios(true)
                .incluirAssinaturaContador(true)
                .build();
        return emitirLaudo(params, "sistema");
    }

    /**
     * Recupera arquivo previamente gerado por ID do laudo
     */
    public byte[] obterArquivoLaudo(String laudoId) {
        LaudoTributario laudo = laudoTributarioRepository.findById(laudoId)
                .orElseThrow(() -> new IllegalArgumentException("Laudo não encontrado: " + laudoId));

        if (laudo.getCaminhoArquivo() != null) {
            Path path = Paths.get(laudo.getCaminhoArquivo());
            if (Files.exists(path)) {
                try {
                    return Files.readAllBytes(path);
                } catch (IOException e) {
                    log.error("Erro ao ler arquivo em disco para o laudo {}", laudoId, e);
                }
            }
        }

        // Se arquivo físico não for encontrado, regenera dinamicamente
        ParametrosEmissaoLaudoDTO params = ParametrosEmissaoLaudoDTO.builder()
                .empresaId(laudo.getEmpresaId())
                .competencia(laudo.getCompetencia())
                .tipoLaudo(laudo.getTipoLaudo())
                .formato("PDF")
                .incluirProjecaoCenarios(true)
                .incluirAssinaturaContador(true)
                .build();
        return emitirLaudo(params, laudo.getUsuarioId());
    }

    /**
     * Validação e auditoria pública de autenticidade documental por Hash SHA-256
     */
    public ValidacaoLaudoResponseDTO validarLaudo(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            return ValidacaoLaudoResponseDTO.builder().valido(false).build();
        }

        String hashSanitizado = hash.trim().toUpperCase();
        Optional<LaudoTributario> laudoOpt = laudoTributarioRepository.findByHashAutenticidade(hashSanitizado);

        if (laudoOpt.isEmpty()) {
            return ValidacaoLaudoResponseDTO.builder()
                    .valido(false)
                    .hashAutenticidade(hashSanitizado)
                    .build();
        }

        LaudoTributario laudo = laudoOpt.get();
        Empresa empresa = buscarEmpresa(laudo.getEmpresaId());
        OrganizacaoContabil contabilidade = buscarContabilidade(empresa);

        String nomeContador = "Responsável Técnico";
        String crcContador = contabilidade != null ? contabilidade.getCrc() : "Não informado";

        if (laudo.getUsuarioId() != null) {
            Usuario usuario = buscarUsuario(laudo.getUsuarioId());
            if (usuario != null && usuario.getNome() != null) {
                nomeContador = usuario.getNome();
            }
        }

        SimulacaoTributariaDTO simulacao = null;
        try {
            YearMonth ym = YearMonth.parse(laudo.getCompetencia());
            simulacao = calculoFatorRService.simularTributacao(empresa.getId(), ym);
        } catch (Exception e) {
            log.warn("Erro ao simular tributação para validação de laudo: {}", e.getMessage());
        }

        String dataFormatada = laudo.getCreatedAt() != null
                ? laudo.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        return ValidacaoLaudoResponseDTO.builder()
                .valido(true)
                .hashAutenticidade(laudo.getHashAutenticidade())
                .dataEmissao(dataFormatada)
                .empresaNome(empresa.getRazaoSocial())
                .empresaCnpj(empresa.getCnpj())
                .contadorResponsavel(nomeContador)
                .crcContador(crcContador)
                .competencia(laudo.getCompetencia())
                .anexoEnquadrado(simulacao != null ? simulacao.getAnexoAplicavel() : "Simples Nacional")
                .percentualFatorR(simulacao != null ? simulacao.getPercentualFatorR() : BigDecimal.ZERO)
                .build();
    }

    /**
     * Lista histórico de laudos da empresa
     */
    public List<LaudoTributario> listarHistorico(String empresaId) {
        Empresa empresa = buscarEmpresa(empresaId);
        return laudoTributarioRepository.findByEmpresaId(empresa.getId().toString());
    }

    // =========================================================================
    // MOTOR DE GERAÇÃO EM PDF COM OPENPDF
    // =========================================================================
    public byte[] gerarPdfLaudo(Empresa empresa, OrganizacaoContabil contabilidade, Usuario usuario,
                               SimulacaoTributariaDTO simulacao, List<ProjecaoCenarioDTO> projecoes,
                               ParametrosEmissaoLaudoDTO params, String hashAutenticidade) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 46, 46);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Adiciona Rodapé e Cabeçalho de página com paginação
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfPTable footer = new PdfPTable(1);
                    try {
                        footer.setTotalWidth(document.right() - document.left());
                        Font fontPag = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 116, 139));
                        PdfPCell cellPag = new PdfPCell(new Phrase("Pág. " + writer.getPageNumber(), fontPag));
                        cellPag.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        cellPag.setBorder(Rectangle.TOP);
                        cellPag.setBorderColor(new Color(226, 232, 240));
                        cellPag.setPaddingTop(5);
                        footer.addCell(cellPag);

                        footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 10, writer.getDirectContent());
                    } catch (Exception e) {
                        log.error("Erro ao desenhar rodapé no PDF", e);
                    }
                }
            });

            document.open();

            // 1. Cabeçalho Institucional Timbrado
            adicionarCabecalhoTimbrado(document, contabilidade, empresa, params);

            // 2. Resumo Executivo do Fator R
            adicionarResumoExecutivo(document, simulacao, params.getCompetencia());

            // 3. Memória de Cálculo Analítica dos Últimos 12 Meses
            adicionarMemoriaCalculo12M(document, simulacao);

            // 4. Estudo Comparativo de Regimes (DAS vs Lucro Presumido)
            adicionarComparativoRegimes(document, simulacao);

            // 5. Projeções Futuras (se habilitado)
            if (params.isIncluirProjecaoCenarios() && projecoes != null && !projecoes.isEmpty()) {
                adicionarProjecoesCenarios(document, projecoes);
            }

            // 6. Parecer Técnico e Conclusivo
            adicionarParecerTecnico(document, simulacao, params.getObservacaoPersonalizada());

            // 7. Bloco de Assinatura e Autenticidade Documental
            adicionarAssinaturaEAutenticidade(document, contabilidade, usuario, params.isIncluirAssinaturaContador(), hashAutenticidade);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Falha ao gerar PDF de laudo tributário", e);
            throw new RuntimeException("Erro ao gerar PDF de laudo tributário: " + e.getMessage(), e);
        }
    }

    private void adicionarCabecalhoTimbrado(Document doc, OrganizacaoContabil contabilidade, Empresa empresa, ParametrosEmissaoLaudoDTO params) throws Exception {
        Color primaryBlue = new Color(30, 58, 138); // #1E3A8A
        Color darkText = new Color(15, 23, 42); // #0F172A

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{55, 45});
        table.setSpacingAfter(12);

        // Coluna Esquerda: Organização Contábil
        Font fontOrgNome = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, primaryBlue);
        Font fontOrgMeta = FontFactory.getFont(FontFactory.HELVETICA, 8, darkText);

        String orgNome = contabilidade != null ? contabilidade.getNome() : "Organização Contábil Especializada";
        String orgCnpj = contabilidade != null ? contabilidade.getCnpj() : "00.000.000/0001-00";
        String orgCrc = contabilidade != null ? contabilidade.getCrc() : "CRC Ativo";

        PdfPCell cellOrg = new PdfPCell();
        cellOrg.setBorder(Rectangle.NO_BORDER);
        cellOrg.addElement(new Paragraph(orgNome.toUpperCase(), fontOrgNome));
        cellOrg.addElement(new Paragraph("CNPJ: " + orgCnpj + "  |  CRC: " + orgCrc, fontOrgMeta));
        cellOrg.addElement(new Paragraph("Auditoria Tributária e Gestão do Simples Nacional", fontOrgMeta));
        table.addCell(cellOrg);

        // Coluna Direita: Laudo e Dados da Empresa
        PdfPCell cellEmpresa = new PdfPCell();
        cellEmpresa.setBorder(Rectangle.NO_BORDER);
        cellEmpresa.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, primaryBlue);
        Paragraph pTitulo = new Paragraph("DOSSIÊ TRIBUTÁRIO & PARECER TÉCNICO", fontTitulo);
        pTitulo.setAlignment(Element.ALIGN_RIGHT);
        cellEmpresa.addElement(pTitulo);

        Font fontEmpresa = FontFactory.getFont(FontFactory.HELVETICA, 8, darkText);
        Paragraph pEmpresa = new Paragraph("Empresa: " + empresa.getRazaoSocial(), fontEmpresa);
        pEmpresa.setAlignment(Element.ALIGN_RIGHT);
        cellEmpresa.addElement(pEmpresa);

        Paragraph pCnpj = new Paragraph("CNPJ: " + empresa.getCnpj() + " | Comp.: " + params.getCompetencia(), fontEmpresa);
        pCnpj.setAlignment(Element.ALIGN_RIGHT);
        cellEmpresa.addElement(pCnpj);

        Paragraph pData = new Paragraph("Emissão: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fontEmpresa);
        pData.setAlignment(Element.ALIGN_RIGHT);
        cellEmpresa.addElement(pData);

        table.addCell(cellEmpresa);
        doc.add(table);

        // Linha divisória
        PdfPTable linha = new PdfPTable(1);
        linha.setWidthPercentage(100);
        PdfPCell cLinha = new PdfPCell();
        cLinha.setBorder(Rectangle.BOTTOM);
        cLinha.setBorderColor(primaryBlue);
        cLinha.setBorderWidth(2f);
        linha.addCell(cLinha);
        linha.setSpacingAfter(12);
        doc.add(linha);
    }

    private void adicionarResumoExecutivo(Document doc, SimulacaoTributariaDTO simulacao, String competencia) throws Exception {
        Color primaryBlue = new Color(30, 58, 138);
        Color lightBg = new Color(248, 250, 252);
        Color borderColor = new Color(226, 232, 240);

        Font fontSec = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, primaryBlue);
        Paragraph secHeader = new Paragraph("1. RESUMO EXECUTIVO DO ENQUADRAMENTO TRIBUTÁRIO", fontSec);
        secHeader.setSpacingAfter(6);
        doc.add(secHeader);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{25, 25, 25, 25});
        table.setSpacingAfter(12);

        boolean isAnexoIII = "Anexo III".equalsIgnoreCase(simulacao.getAnexoAplicavel());
        Color statusColor = isAnexoIII ? new Color(5, 150, 105) : new Color(217, 119, 6);

        adicionarCardMetrica(table, "Competência Avaliada", competencia, primaryBlue, lightBg, borderColor);
        adicionarCardMetrica(table, "Fator R Calculado",
                PERCENTUAL_FORMAT.format(simulacao.getPercentualFatorR()) + "%", statusColor, lightBg, borderColor);
        adicionarCardMetrica(table, "Enquadramento Simples",
                simulacao.getAnexoAplicavel().toUpperCase(), statusColor, lightBg, borderColor);
        adicionarCardMetrica(table, "Alíquota Efetiva SN",
                PERCENTUAL_FORMAT.format(simulacao.getAliquotaEfetivaSimplesNacional()) + "%", primaryBlue, lightBg, borderColor);

        doc.add(table);

        // Se houver alerta de proximidade
        if (simulacao.isAlertaProximidadeLimite() && simulacao.getMensagemAlerta() != null) {
            PdfPTable alertTable = new PdfPTable(1);
            alertTable.setWidthPercentage(100);
            PdfPCell cellAlert = new PdfPCell();
            cellAlert.setBackgroundColor(new Color(254, 243, 199)); // Amarelo/âmbar suave
            cellAlert.setBorderColor(new Color(245, 158, 11));
            cellAlert.setPadding(8);
            
            Font fontAlert = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(146, 64, 14));
            cellAlert.addElement(new Paragraph(simulacao.getMensagemAlerta(), fontAlert));
            alertTable.addCell(cellAlert);
            alertTable.setSpacingAfter(10);
            doc.add(alertTable);
        }
    }

    private void adicionarCardMetrica(PdfPTable table, String titulo, String valor, Color valColor, Color bg, Color border) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorderColor(border);
        cell.setPadding(6);

        Font fontTit = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(100, 116, 139));
        Font fontVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, valColor);

        Paragraph pTit = new Paragraph(titulo, fontTit);
        pTit.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pTit);

        Paragraph pVal = new Paragraph(valor, fontVal);
        pVal.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pVal);

        table.addCell(cell);
    }

    private void adicionarMemoriaCalculo12M(Document doc, SimulacaoTributariaDTO simulacao) throws Exception {
        Color primaryBlue = new Color(30, 58, 138);
        Color headerBg = new Color(241, 245, 249);
        Color borderColor = new Color(226, 232, 240);

        Font fontSec = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, primaryBlue);
        Paragraph secHeader = new Paragraph("2. MEMÓRIA DE CÁLCULO ANALÍTICA (JANELA DESLIZANTE DE 12 MESES)", fontSec);
        secHeader.setSpacingAfter(6);
        doc.add(secHeader);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{25, 25, 25, 25});
        table.setSpacingAfter(12);

        Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, primaryBlue);
        adicionarHeaderCell(table, "Competência", fHead, headerBg, borderColor);
        adicionarHeaderCell(table, "Faturamento Bruto (R$)", fHead, headerBg, borderColor);
        adicionarHeaderCell(table, "Folha de Pagamento (R$)", fHead, headerBg, borderColor);
        adicionarHeaderCell(table, "Razão Folha/Fat. (%)", fHead, headerBg, borderColor);

        Font fData = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(15, 23, 42));
        boolean zebra = false;

        List<FaturamentoHistoricoDTO> historico = simulacao.getHistoricoFaturamentos();
        if (historico != null && !historico.isEmpty()) {
            // Os 12 meses anteriores formam a base do Fator R
            for (FaturamentoHistoricoDTO item : historico) {
                Color rowBg = zebra ? new Color(248, 250, 252) : Color.WHITE;
                zebra = !zebra;

                BigDecimal ratio = BigDecimal.ZERO;
                if (item.getFaturamento() != null && item.getFaturamento().compareTo(BigDecimal.ZERO) > 0 && item.getFolhaPagamento() != null) {
                    ratio = item.getFolhaPagamento().divide(item.getFaturamento(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                }

                adicionarCell(table, item.getCompetencia().toString(), fData, rowBg, borderColor, Element.ALIGN_CENTER);
                adicionarCell(table, MOEDA_FORMAT.format(item.getFaturamento() != null ? item.getFaturamento() : BigDecimal.ZERO), fData, rowBg, borderColor, Element.ALIGN_RIGHT);
                adicionarCell(table, MOEDA_FORMAT.format(item.getFolhaPagamento() != null ? item.getFolhaPagamento() : BigDecimal.ZERO), fData, rowBg, borderColor, Element.ALIGN_RIGHT);
                adicionarCell(table, PERCENTUAL_FORMAT.format(ratio) + "%", fData, rowBg, borderColor, Element.ALIGN_RIGHT);
            }
        }

        // Linha de Totais dos 12 meses
        Font fTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, primaryBlue);
        Color totalBg = new Color(226, 232, 240);

        adicionarCell(table, "TOTAIS ACUMULADOS (RBT12 / FOLHA 12M)", fTotal, totalBg, borderColor, Element.ALIGN_LEFT);
        adicionarCell(table, MOEDA_FORMAT.format(simulacao.getTotalFaturamento12Meses()), fTotal, totalBg, borderColor, Element.ALIGN_RIGHT);
        adicionarCell(table, MOEDA_FORMAT.format(simulacao.getTotalFolha12Meses()), fTotal, totalBg, borderColor, Element.ALIGN_RIGHT);
        adicionarCell(table, PERCENTUAL_FORMAT.format(simulacao.getPercentualFatorR()) + "%", fTotal, totalBg, borderColor, Element.ALIGN_RIGHT);

        doc.add(table);
    }

    private void adicionarComparativoRegimes(Document doc, SimulacaoTributariaDTO sim) throws Exception {
        Color primaryBlue = new Color(30, 58, 138);
        Color headerBg = new Color(241, 245, 249);
        Color borderColor = new Color(226, 232, 240);

        Font fontSec = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, primaryBlue);
        Paragraph secHeader = new Paragraph("3. ESTUDO COMPARATIVO DE REGIMES TRIBUTÁRIOS (DAS vs LUCRO PRESUMIDO)", fontSec);
        secHeader.setSpacingAfter(6);
        doc.add(secHeader);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{40, 30, 30});
        table.setSpacingAfter(10);

        Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, primaryBlue);
        adicionarHeaderCell(table, "Tributo / Categoria", fHead, headerBg, borderColor);
        adicionarHeaderCell(table, "Simples Nacional (DAS)", fHead, headerBg, borderColor);
        adicionarHeaderCell(table, "Lucro Presumido", fHead, headerBg, borderColor);

        Font fData = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(15, 23, 42));

        adicionarRowComparativo(table, "PIS (0,65%)", "Incluso no DAS", MOEDA_FORMAT.format(sim.getValorPis()), fData, borderColor);
        adicionarRowComparativo(table, "COFINS (3,00%)", "Incluso no DAS", MOEDA_FORMAT.format(sim.getValorCofins()), fData, borderColor);
        adicionarRowComparativo(table, "ISS (5,00%)", "Incluso no DAS", MOEDA_FORMAT.format(sim.getValorIss()), fData, borderColor);
        adicionarRowComparativo(table, "IRPJ (15% sobre 32%)", "Incluso no DAS", MOEDA_FORMAT.format(sim.getValorIrpj()), fData, borderColor);
        if (sim.getValorAdicionalIrpj() != null && sim.getValorAdicionalIrpj().compareTo(BigDecimal.ZERO) > 0) {
            adicionarRowComparativo(table, "Adicional IRPJ (10%)", "Incluso no DAS", MOEDA_FORMAT.format(sim.getValorAdicionalIrpj()), fData, borderColor);
        }
        adicionarRowComparativo(table, "CSLL (9% sobre 32%)", "Incluso no DAS", MOEDA_FORMAT.format(sim.getValorCsll()), fData, borderColor);

        // Totais e alíquotas efetivas
        Font fTot = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, primaryBlue);
        Color totalBg = new Color(226, 232, 240);

        adicionarCell(table, "TOTAL MENSAL ESTIMADO", fTot, totalBg, borderColor, Element.ALIGN_LEFT);
        adicionarCell(table, MOEDA_FORMAT.format(sim.getValorEstimadoSimplesNacional()), fTot, totalBg, borderColor, Element.ALIGN_RIGHT);
        adicionarCell(table, MOEDA_FORMAT.format(sim.getValorEstimadoLucroPresumido()), fTot, totalBg, borderColor, Element.ALIGN_RIGHT);

        adicionarCell(table, "ALÍQUOTA EFETIVA TOTAL", fTot, Color.WHITE, borderColor, Element.ALIGN_LEFT);
        adicionarCell(table, PERCENTUAL_FORMAT.format(sim.getAliquotaEfetivaSimplesNacional()) + "%", fTot, Color.WHITE, borderColor, Element.ALIGN_RIGHT);
        adicionarCell(table, PERCENTUAL_FORMAT.format(sim.getAliquotaEfetivaLucroPresumido()) + "%", fTot, Color.WHITE, borderColor, Element.ALIGN_RIGHT);

        doc.add(table);

        // Caixa de Economia
        PdfPTable ecoTable = new PdfPTable(1);
        ecoTable.setWidthPercentage(100);
        PdfPCell cellEco = new PdfPCell();
        cellEco.setBackgroundColor(new Color(236, 253, 245)); // Verde menta suave
        cellEco.setBorderColor(new Color(16, 185, 129));
        cellEco.setPadding(8);

        Font fEco = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(6, 95, 70));
        String txtEco = String.format("Regime Tributário Mais Vantajoso: %s | Economia Mensal Estimada: %s (Aprox. %s/ano)",
                sim.getRegimeMaisVantajoso(),
                MOEDA_FORMAT.format(sim.getEconomiaEstimada()),
                MOEDA_FORMAT.format(sim.getEconomiaEstimada().multiply(new BigDecimal("12"))));
        cellEco.addElement(new Paragraph(txtEco, fEco));
        ecoTable.addCell(cellEco);
        ecoTable.setSpacingAfter(10);
        doc.add(ecoTable);
    }

    private void adicionarRowComparativo(PdfPTable table, String rubrica, String colSN, String colLP, Font font, Color border) {
        adicionarCell(table, rubrica, font, Color.WHITE, border, Element.ALIGN_LEFT);
        adicionarCell(table, colSN, font, Color.WHITE, border, Element.ALIGN_CENTER);
        adicionarCell(table, colLP, font, Color.WHITE, border, Element.ALIGN_RIGHT);
    }

    private void adicionarProjecoesCenarios(Document doc, List<ProjecaoCenarioDTO> projecoes) throws Exception {
        Color primaryBlue = new Color(30, 58, 138);
        Color headerBg = new Color(241, 245, 249);
        Color borderColor = new Color(226, 232, 240);

        Font fontSec = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, primaryBlue);
        Paragraph secHeader = new Paragraph("4. PROJEÇÃO DE CENÁRIOS FUTUROS (M+1 / M+2)", fontSec);
        secHeader.setSpacingAfter(6);
        doc.add(secHeader);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{22, 14, 18, 18, 13, 15});
        table.setSpacingAfter(12);

        Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, primaryBlue);
        adicionarHeaderCell(table, "Cenário Projetado", fHead, headerBg, borderColor);
        adicionarHeaderCell(table, "Mês", fHead, headerBg, borderColor);
        adicionarHeaderCell(table, "Fat. Estimado", fHead, headerBg, borderColor);
        adicionarHeaderCell(table, "Folha Estimada", fHead, headerBg, borderColor);
        adicionarHeaderCell(table, "Fator R", fHead, headerBg, borderColor);
        adicionarHeaderCell(table, "Anexo", fHead, headerBg, borderColor);

        Font fData = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(15, 23, 42));

        for (ProjecaoCenarioDTO p : projecoes) {
            if (p.getMeses() != null) {
                for (ProjecaoCenarioDTO.MesProjecaoDTO m : p.getMeses()) {
                    adicionarCell(table, p.getNomeCenario(), fData, Color.WHITE, borderColor, Element.ALIGN_LEFT);
                    adicionarCell(table, m.getCompetencia() != null ? m.getCompetencia().toString() : "-", fData, Color.WHITE, borderColor, Element.ALIGN_CENTER);
                    adicionarCell(table, MOEDA_FORMAT.format(m.getFaturamentoEstimado() != null ? m.getFaturamentoEstimado() : BigDecimal.ZERO), fData, Color.WHITE, borderColor, Element.ALIGN_RIGHT);
                    adicionarCell(table, MOEDA_FORMAT.format(m.getFolhaEstimada() != null ? m.getFolhaEstimada() : BigDecimal.ZERO), fData, Color.WHITE, borderColor, Element.ALIGN_RIGHT);
                    adicionarCell(table, PERCENTUAL_FORMAT.format(m.getPercentualFatorR() != null ? m.getPercentualFatorR() : BigDecimal.ZERO) + "%", fData, Color.WHITE, borderColor, Element.ALIGN_RIGHT);
                    adicionarCell(table, m.getAnexoAplicavel() != null ? m.getAnexoAplicavel() : "-", fData, Color.WHITE, borderColor, Element.ALIGN_CENTER);
                }
            }
        }

        doc.add(table);
    }

    private void adicionarParecerTecnico(Document doc, SimulacaoTributariaDTO sim, String obsPersonalizada) throws Exception {
        Color primaryBlue = new Color(30, 58, 138);

        Font fontSec = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, primaryBlue);
        Paragraph secHeader = new Paragraph("5. PARECER CONCLUSIVO E RECOMENDAÇÕES DA AUDITORIA FISCAL", fontSec);
        secHeader.setSpacingAfter(6);
        doc.add(secHeader);

        Font fontTexto = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(30, 41, 59));

        String textoLegal = "Com base no Artigo 18, §§ 5º-J, 5º-K e 5º-M da Lei Complementar nº 123/2006, regulamentada pela " +
                "Resolução CGSN nº 140/2018, as atividades de prestação de serviços enquadradas no § 5º-I são tributadas na forma do " +
                "Anexo III caso a razão entre a folha de salários (incluídos encargos e pró-labore) dos últimos 12 meses e a receita bruta " +
                "acumulada no mesmo período (Fator R) seja igual ou superior a 28,00%.";

        Paragraph p1 = new Paragraph(textoLegal, fontTexto);
        p1.setAlignment(Element.ALIGN_JUSTIFIED);
        p1.setSpacingAfter(6);
        doc.add(p1);

        String recomendacao;
        if ("Anexo III".equalsIgnoreCase(sim.getAnexoAplicavel())) {
            recomendacao = String.format("A empresa obteve Fator R de %s%%, garantindo tributação favorável no Anexo III. " +
                    "Recomenda-se a manutenção da massa salarial e pro-labores nos níveis correntes, monitorando os meses subsequentes para evitar " +
                    "queda abaixo do índice crítico de 28%%.", PERCENTUAL_FORMAT.format(sim.getPercentualFatorR()));
        } else {
            recomendacao = String.format("A empresa obteve Fator R de %s%%, ficando enquadrada no Anexo V. " +
                    "Recomenda-se o estudo de planejamento previdenciário e aumento de pró-labore dos sócios para elevar o índice a 28%%, " +
                    "o que possibilitará a transição para o Anexo III com consequente redução da carga fiscal do Simples Nacional.",
                    PERCENTUAL_FORMAT.format(sim.getPercentualFatorR()));
        }

        Paragraph p2 = new Paragraph(recomendacao, fontTexto);
        p2.setAlignment(Element.ALIGN_JUSTIFIED);
        p2.setSpacingAfter(6);
        doc.add(p2);

        if (obsPersonalizada != null && !obsPersonalizada.trim().isEmpty()) {
            Font fontObsTit = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, primaryBlue);
            Paragraph pObsTit = new Paragraph("Observações Personalizadas do Contador:", fontObsTit);
            pObsTit.setSpacingAfter(3);
            doc.add(pObsTit);

            Paragraph pObs = new Paragraph(obsPersonalizada.trim(), fontTexto);
            pObs.setAlignment(Element.ALIGN_JUSTIFIED);
            pObs.setSpacingAfter(6);
            doc.add(pObs);
        }
    }

    private void adicionarAssinaturaEAutenticidade(Document doc, OrganizacaoContabil contabilidade, Usuario usuario,
                                                  boolean incluirAssinatura, String hashAutenticidade) throws Exception {
        Color primaryBlue = new Color(30, 58, 138);

        if (incluirAssinatura) {
            PdfPTable tableAss = new PdfPTable(1);
            tableAss.setWidthPercentage(60);
            tableAss.setHorizontalAlignment(Element.ALIGN_CENTER);
            tableAss.setSpacingBefore(18);
            tableAss.setSpacingAfter(10);

            PdfPCell cAss = new PdfPCell();
            cAss.setBorder(Rectangle.TOP);
            cAss.setBorderColor(new Color(100, 116, 139));
            cAss.setHorizontalAlignment(Element.ALIGN_CENTER);
            cAss.setPaddingTop(5);

            String nomeContador = usuario != null && usuario.getNome() != null ? usuario.getNome() : "Contador Responsável Técnico";
            String crc = contabilidade != null ? contabilidade.getCrc() : "CRC/SP Regular";

            Font fNome = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, primaryBlue);
            Font fCrc = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 116, 139));

            Paragraph pNome = new Paragraph(nomeContador, fNome);
            pNome.setAlignment(Element.ALIGN_CENTER);
            cAss.addElement(pNome);

            Paragraph pCrc = new Paragraph("Contador - " + crc, fCrc);
            pCrc.setAlignment(Element.ALIGN_CENTER);
            cAss.addElement(pCrc);

            tableAss.addCell(cAss);
            doc.add(tableAss);
        }
    }

    private void adicionarHeaderCell(PdfPTable table, String text, Font font, Color bg, Color border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(border);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void adicionarCell(PdfPTable table, String text, Font font, Color bg, Color border, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(border);
        cell.setPadding(4);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    // =========================================================================
    // MOTOR DE EXPORTAÇÃO EXCEL (.XLSX) COM APACHE POI
    // =========================================================================
    public byte[] gerarExcelLaudo(Empresa empresa, OrganizacaoContabil contabilidade, Usuario usuario,
                                 SimulacaoTributariaDTO simulacao, List<ProjecaoCenarioDTO> projecoes,
                                 ParametrosEmissaoLaudoDTO params, String hashAutenticidade) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CreationHelper helper = workbook.getCreationHelper();

            // Estilos
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            byte[] rgbNavy = new byte[]{(byte) 30, (byte) 58, (byte) 138};
            headerStyle.setFillForegroundColor(new XSSFColor(rgbNavy, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont fontHead = workbook.createFont();
            fontHead.setBold(true);
            fontHead.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(fontHead);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            XSSFCellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(helper.createDataFormat().getFormat("R$ #,##0.00"));
            currencyStyle.setBorderBottom(BorderStyle.THIN);
            currencyStyle.setBorderTop(BorderStyle.THIN);

            XSSFCellStyle percentStyle = workbook.createCellStyle();
            percentStyle.setDataFormat(helper.createDataFormat().getFormat("0.00%"));
            percentStyle.setAlignment(HorizontalAlignment.RIGHT);
            percentStyle.setBorderBottom(BorderStyle.THIN);
            percentStyle.setBorderTop(BorderStyle.THIN);

            XSSFCellStyle boldTotalStyle = workbook.createCellStyle();
            XSSFFont fontBold = workbook.createFont();
            fontBold.setBold(true);
            boldTotalStyle.setFont(fontBold);
            boldTotalStyle.setDataFormat(helper.createDataFormat().getFormat("R$ #,##0.00"));
            boldTotalStyle.setBorderTop(BorderStyle.MEDIUM);
            boldTotalStyle.setBorderBottom(BorderStyle.DOUBLE);

            // -------------------------------------------------------------
            // ABA 1: Resumo e Fator R
            // -------------------------------------------------------------
            XSSFSheet sheet1 = workbook.createSheet("Resumo e Fator R");
            sheet1.setDisplayGridlines(true);

            Row r0 = sheet1.createRow(0);
            r0.createCell(0).setCellValue("DOSSIÊ TRIBUTÁRIO & MEMÓRIA DO FATOR R");
            sheet1.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            Row r1 = sheet1.createRow(1);
            r1.createCell(0).setCellValue("Empresa: " + empresa.getRazaoSocial() + " | CNPJ: " + empresa.getCnpj());
            sheet1.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));

            Row r2 = sheet1.createRow(2);
            r2.createCell(0).setCellValue("Competência: " + params.getCompetencia());
            sheet1.addMergedRegion(new CellRangeAddress(2, 2, 0, 3));

            int rowIdx1 = 4;
            Row rh1 = sheet1.createRow(rowIdx1++);
            rh1.createCell(0).setCellValue("Métrica / Indicador");
            rh1.createCell(1).setCellValue("Valor Apurado");
            rh1.getCell(0).setCellStyle(headerStyle);
            rh1.getCell(1).setCellStyle(headerStyle);

            criarLinhaExcelMoeda(sheet1, rowIdx1++, "Faturamento Acumulado 12 Meses (RBT12)", simulacao.getTotalFaturamento12Meses(), currencyStyle);
            criarLinhaExcelMoeda(sheet1, rowIdx1++, "Folha de Pagamento Acumulada 12 Meses", simulacao.getTotalFolha12Meses(), currencyStyle);
            criarLinhaExcelPercent(sheet1, rowIdx1++, "Fator R Calculado", simulacao.getPercentualFatorR().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP), percentStyle);
            criarLinhaExcelTexto(sheet1, rowIdx1++, "Enquadramento Simples Nacional", simulacao.getAnexoAplicavel());
            criarLinhaExcelPercent(sheet1, rowIdx1++, "Alíquota Efetiva Simples Nacional", simulacao.getAliquotaEfetivaSimplesNacional().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP), percentStyle);
            criarLinhaExcelMoeda(sheet1, rowIdx1++, "Valor Estimado Simples Nacional (DAS)", simulacao.getValorEstimadoSimplesNacional(), currencyStyle);
            criarLinhaExcelPercent(sheet1, rowIdx1++, "Alíquota Efetiva Lucro Presumido", simulacao.getAliquotaEfetivaLucroPresumido().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP), percentStyle);
            criarLinhaExcelMoeda(sheet1, rowIdx1++, "Valor Estimado Lucro Presumido", simulacao.getValorEstimadoLucroPresumido(), currencyStyle);
            criarLinhaExcelTexto(sheet1, rowIdx1++, "Regime Tributário Mais Vantajoso", simulacao.getRegimeMaisVantajoso());
            criarLinhaExcelMoeda(sheet1, rowIdx1++, "Economia Tributária Mensal Estimada", simulacao.getEconomiaEstimada(), boldTotalStyle);

            sheet1.autoSizeColumn(0);
            sheet1.autoSizeColumn(1);

            // -------------------------------------------------------------
            // ABA 2: Memoria de Calculo 12 Meses (COM FÓRMULAS DINÂMICAS)
            // -------------------------------------------------------------
            XSSFSheet sheet2 = workbook.createSheet("Memoria de Calculo 12 Meses");
            sheet2.setDisplayGridlines(true);

            Row rHead2 = sheet2.createRow(0);
            rHead2.createCell(0).setCellValue("Competência");
            rHead2.createCell(1).setCellValue("Faturamento Mensal (R$)");
            rHead2.createCell(2).setCellValue("Folha de Pagamento (R$)");
            rHead2.createCell(3).setCellValue("Proporção Mensal");
            for (int i = 0; i < 4; i++) rHead2.getCell(i).setCellStyle(headerStyle);

            List<FaturamentoHistoricoDTO> hist = simulacao.getHistoricoFaturamentos();
            int startRow = 1;
            int currentRow = startRow;

            if (hist != null) {
                for (FaturamentoHistoricoDTO h : hist) {
                    Row row = sheet2.createRow(currentRow);
                    row.createCell(0).setCellValue(h.getCompetencia().toString());
                    
                    Cell cFat = row.createCell(1);
                    cFat.setCellValue(h.getFaturamento() != null ? h.getFaturamento().doubleValue() : 0.0);
                    cFat.setCellStyle(currencyStyle);

                    Cell cFolha = row.createCell(2);
                    cFolha.setCellValue(h.getFolhaPagamento() != null ? h.getFolhaPagamento().doubleValue() : 0.0);
                    cFolha.setCellStyle(currencyStyle);

                    // Fórmula da proporção: =C{row}/B{row}
                    Cell cRatio = row.createCell(3);
                    int excelRowNum = currentRow + 1;
                    cRatio.setCellFormula("IF(B" + excelRowNum + ">0, C" + excelRowNum + "/B" + excelRowNum + ", 0)");
                    cRatio.setCellStyle(percentStyle);

                    currentRow++;
                }
            }

            // Linha de TOTAIS com fórmulas dinâmicas =SUM(...)
            Row rowTotal = sheet2.createRow(currentRow);
            rowTotal.createCell(0).setCellValue("TOTAIS ACUMULADOS (12M)");
            rowTotal.getCell(0).setCellStyle(boldTotalStyle);

            int firstRowExcel = startRow + 1;
            int lastRowExcel = currentRow; // última linha de dados

            Cell cellTotalFat = rowTotal.createCell(1);
            cellTotalFat.setCellFormula("SUM(B" + firstRowExcel + ":B" + lastRowExcel + ")");
            cellTotalFat.setCellStyle(boldTotalStyle);

            Cell cellTotalFolha = rowTotal.createCell(2);
            cellTotalFolha.setCellFormula("SUM(C" + firstRowExcel + ":C" + lastRowExcel + ")");
            cellTotalFolha.setCellStyle(boldTotalStyle);

            Cell cellFatorRTotal = rowTotal.createCell(3);
            int totalRowNum = currentRow + 1;
            cellFatorRTotal.setCellFormula("IF(B" + totalRowNum + ">0, C" + totalRowNum + "/B" + totalRowNum + ", 0)");
            cellFatorRTotal.setCellStyle(percentStyle);

            for (int i = 0; i < 4; i++) sheet2.autoSizeColumn(i);

            // -------------------------------------------------------------
            // ABA 3: Comparativo Regimes Fiscais
            // -------------------------------------------------------------
            XSSFSheet sheet3 = workbook.createSheet("Comparativo Regimes Fiscais");
            sheet3.setDisplayGridlines(true);

            Row rHead3 = sheet3.createRow(0);
            rHead3.createCell(0).setCellValue("Discriminação do Tributo");
            rHead3.createCell(1).setCellValue("Simples Nacional (R$)");
            rHead3.createCell(2).setCellValue("Lucro Presumido (R$)");
            for (int i = 0; i < 3; i++) rHead3.getCell(i).setCellStyle(headerStyle);

            int rIdx3 = 1;
            criarLinhaExcelComparativo(sheet3, rIdx3++, "PIS (0,65%)", 0, simulacao.getValorPis().doubleValue(), currencyStyle);
            criarLinhaExcelComparativo(sheet3, rIdx3++, "COFINS (3,00%)", 0, simulacao.getValorCofins().doubleValue(), currencyStyle);
            criarLinhaExcelComparativo(sheet3, rIdx3++, "ISS (5,00%)", 0, simulacao.getValorIss().doubleValue(), currencyStyle);
            criarLinhaExcelComparativo(sheet3, rIdx3++, "IRPJ (15% sobre Presunção 32%)", 0, simulacao.getValorIrpj().doubleValue(), currencyStyle);
            if (simulacao.getValorAdicionalIrpj() != null && simulacao.getValorAdicionalIrpj().compareTo(BigDecimal.ZERO) > 0) {
                criarLinhaExcelComparativo(sheet3, rIdx3++, "Adicional de IRPJ (10%)", 0, simulacao.getValorAdicionalIrpj().doubleValue(), currencyStyle);
            }
            criarLinhaExcelComparativo(sheet3, rIdx3++, "CSLL (9% sobre Presunção 32%)", 0, simulacao.getValorCsll().doubleValue(), currencyStyle);

            // Linha de Total com fórmula dinâmica no Lucro Presumido
            Row rTotal3 = sheet3.createRow(rIdx3);
            rTotal3.createCell(0).setCellValue("TOTAL TRIBUTÁRIO MENSAL");
            rTotal3.getCell(0).setCellStyle(boldTotalStyle);

            Cell cTotSN = rTotal3.createCell(1);
            cTotSN.setCellValue(simulacao.getValorEstimadoSimplesNacional().doubleValue());
            cTotSN.setCellStyle(boldTotalStyle);

            Cell cTotLP = rTotal3.createCell(2);
            cTotLP.setCellFormula("SUM(C2:C" + rIdx3 + ")");
            cTotLP.setCellStyle(boldTotalStyle);
            rIdx3++;

            // Linha de Economia Estimada
            Row rEco3 = sheet3.createRow(rIdx3);
            rEco3.createCell(0).setCellValue("ECONOMIA ESTIMADA MENSAL");
            rEco3.getCell(0).setCellStyle(boldTotalStyle);
            rEco3.createCell(1).setCellValue(simulacao.getRegimeMaisVantajoso());
            Cell cEcoVal = rEco3.createCell(2);
            int totRowExcel = rIdx3;
            cEcoVal.setCellFormula("ABS(B" + totRowExcel + "-C" + totRowExcel + ")");
            cEcoVal.setCellStyle(boldTotalStyle);

            for (int i = 0; i < 3; i++) sheet3.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Falha ao gerar planilha Excel de laudo tributário", e);
            throw new RuntimeException("Erro ao gerar planilha Excel: " + e.getMessage(), e);
        }
    }

    private void criarLinhaExcelMoeda(XSSFSheet sheet, int rowIdx, String label, BigDecimal valor, CellStyle style) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        Cell c = row.createCell(1);
        c.setCellValue(valor != null ? valor.doubleValue() : 0.0);
        c.setCellStyle(style);
    }

    private void criarLinhaExcelPercent(XSSFSheet sheet, int rowIdx, String label, BigDecimal valor, CellStyle style) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        Cell c = row.createCell(1);
        c.setCellValue(valor != null ? valor.doubleValue() : 0.0);
        c.setCellStyle(style);
    }

    private void criarLinhaExcelTexto(XSSFSheet sheet, int rowIdx, String label, String texto) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(texto != null ? texto : "");
    }

    private void criarLinhaExcelComparativo(XSSFSheet sheet, int rowIdx, String rubrica, double valSN, double valLP, CellStyle style) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(rubrica);
        Cell c1 = row.createCell(1);
        if (valSN == 0) {
            c1.setCellValue("Incluso no DAS");
        } else {
            c1.setCellValue(valSN);
            c1.setCellStyle(style);
        }
        Cell c2 = row.createCell(2);
        c2.setCellValue(valLP);
        c2.setCellStyle(style);
    }

    // =========================================================================
    // UTILITÁRIOS E PERSISTÊNCIA
    // =========================================================================
    public String calcularHashSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular SHA-256", e);
        }
    }

    private void salvarRegistroLaudo(String id, String empresaId, String usuarioId, String competencia,
                                     String tipoLaudo, String hash, String ext, byte[] conteudo) {
        String caminhoCompleto = null;
        try {
            if (!Files.exists(storageDirectory)) {
                Files.createDirectories(storageDirectory);
            }
            Path arquivo = storageDirectory.resolve(id + "." + ext);
            Files.write(arquivo, conteudo);
            caminhoCompleto = arquivo.toAbsolutePath().toString();
        } catch (Exception e) {
            log.warn("Não foi possível persistir arquivo físico do laudo no disco: {}", e.getMessage());
        }

        LaudoTributario laudo = new LaudoTributario();
        laudo.setId(id);
        laudo.setEmpresaId(empresaId);
        laudo.setUsuarioId(usuarioId != null ? usuarioId : "sistema");
        laudo.setCompetencia(competencia);
        laudo.setTipoLaudo(tipoLaudo != null ? tipoLaudo : "DOSSIE_COMPLETO");
        laudo.setHashAutenticidade(hash);
        laudo.setCaminhoArquivo(caminhoCompleto);
        laudo.setCreatedAt(LocalDateTime.now());

        laudoTributarioRepository.save(laudo);
    }

    private Empresa buscarEmpresa(String empresaId) {
        try {
            Long id = Long.parseLong(empresaId);
            return empresaRepository.findById(id)
                    .or(() -> empresaRepository.findByUuidId(empresaId))
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada: " + empresaId));
        } catch (NumberFormatException e) {
            return empresaRepository.findByUuidId(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada: " + empresaId));
        }
    }

    private OrganizacaoContabil buscarContabilidade(Empresa empresa) {
        if (empresa.getOrganizacaoContabilId() != null) {
            Optional<OrganizacaoContabil> orgOpt = organizacaoContabilRepository.findById(empresa.getOrganizacaoContabilId());
            if (orgOpt.isPresent()) {
                return orgOpt.get();
            }
        }
        List<OrganizacaoContabil> orgs = organizacaoContabilRepository.findAll();
        if (!orgs.isEmpty()) {
            return orgs.get(0);
        }
        return new OrganizacaoContabil("org-default", "Organização Contábil Modelo", "12ABC345000199", "CRC-SP 123456/O", LocalDateTime.now(), LocalDateTime.now());
    }

    private Usuario buscarUsuario(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank() || "sistema".equalsIgnoreCase(usuarioId)) {
            return null;
        }
        try {
            Long id = Long.parseLong(usuarioId);
            return usuarioRepository.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            return usuarioRepository.findAll().stream()
                    .filter(u -> usuarioId.equals(u.getUuidId()))
                    .findFirst()
                    .orElse(null);
        }
    }
}
