package com.fatorr.backend.service;

import com.fatorr.backend.domain.EncerramentoCompetencia;
import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.domain.FaixaTributaria;
import com.fatorr.backend.domain.FaturamentoMensal;
import com.fatorr.backend.domain.FolhaDetalhada;
import com.fatorr.backend.domain.FolhaPagamentoMensal;
import com.fatorr.backend.domain.ParametroTributario;
import com.fatorr.backend.dto.ImportFolhaResponseDTO;
import com.fatorr.backend.dto.ProLaboreIdealDTO;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.repository.EncerramentoCompetenciaRepository;
import com.fatorr.backend.repository.FaturamentoMensalRepository;
import com.fatorr.backend.repository.FolhaDetalhadaRepository;
import com.fatorr.backend.repository.FolhaPagamentoMensalRepository;
import com.fatorr.backend.repository.ParametroTributarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollService {

    private final FolhaDetalhadaRepository folhaDetalhadaRepository;
    private final FolhaPagamentoMensalRepository folhaPagamentoMensalRepository;
    private final FaturamentoMensalRepository faturamentoRepository;
    private final EmpresaRepository empresaRepository;
    private final TenantResolverService tenantResolverService;
    private final ParametroTributarioRepository parametroRepository;
    private final EncerramentoCompetenciaRepository encerramentoRepository;

    // Constantes Previdenciárias e Fator R (Teto 2026)
    private static final BigDecimal FATOR_R_ALVO = new BigDecimal("0.28");
    private static final BigDecimal TETO_BASE_INSS_2026 = new BigDecimal("8258.74");
    private static final BigDecimal TETO_VALOR_INSS_2026 = new BigDecimal("908.86");
    private static final BigDecimal ALIQUOTA_INSS_SOCIO = new BigDecimal("0.11");

    // ==========================================
    // 1. LISTAGEM E BUSCA DE FOLHAS DETALHADAS
    // ==========================================

    @Transactional(readOnly = true)
    public List<FolhaDetalhada> listFolhas(String empresaId) {
        Empresa empresa = tenantResolverService.resolveEmpresa(empresaId);
        String uuid = empresa.getUuidId() != null ? empresa.getUuidId() : String.valueOf(empresa.getId());
        String numId = String.valueOf(empresa.getId());

        List<FolhaDetalhada> folhas = new ArrayList<>(folhaDetalhadaRepository.findByEmpresaId(uuid));
        if (!uuid.equals(numId)) {
            List<FolhaDetalhada> porNum = folhaDetalhadaRepository.findByEmpresaId(numId);
            for (FolhaDetalhada f : porNum) {
                if (folhas.stream().noneMatch(existente -> existente.getCompetencia().equals(f.getCompetencia()))) {
                    folhas.add(f);
                }
            }
        }

        folhas.sort(Comparator.comparing(FolhaDetalhada::getCompetencia).reversed());
        return folhas;
    }

    @Transactional(readOnly = true)
    public Optional<FolhaDetalhada> getFolhaByCompetencia(String empresaId, String competencia) {
        Empresa empresa = tenantResolverService.resolveEmpresa(empresaId);
        String compFormatada = normalizarCompetencia(competencia);
        String uuid = empresa.getUuidId() != null ? empresa.getUuidId() : String.valueOf(empresa.getId());

        Optional<FolhaDetalhada> opt = folhaDetalhadaRepository.findByEmpresaIdAndCompetencia(uuid, compFormatada);
        if (opt.isEmpty() && empresa.getId() != null) {
            opt = folhaDetalhadaRepository.findByEmpresaIdAndCompetencia(String.valueOf(empresa.getId()), compFormatada);
        }
        return opt;
    }

    // ==========================================
    // 2. SALVAR E SINCRONIZAR FOLHA DETALHADA
    // ==========================================

    @Transactional
    public FolhaDetalhada saveFolha(FolhaDetalhada folha) {
        if (folha.getEmpresaId() == null || folha.getEmpresaId().isBlank()) {
            throw new IllegalArgumentException("Identificador da empresa é obrigatório");
        }
        if (folha.getCompetencia() == null || folha.getCompetencia().isBlank()) {
            throw new IllegalArgumentException("Competência é obrigatória (ex: YYYY-MM)");
        }

        Empresa empresa = tenantResolverService.resolveEmpresa(folha.getEmpresaId());
        String canonicalUuid = empresa.getUuidId() != null ? empresa.getUuidId() : String.valueOf(empresa.getId());
        String compNormalizada = normalizarCompetencia(folha.getCompetencia());
        YearMonth ymComp = YearMonth.parse(compNormalizada);

        if (encerramentoRepository.findByEmpresaIdAndCompetencia(empresa.getId(), ymComp)
                .map(EncerramentoCompetencia::isEncerrado).orElse(false)) {
            throw new IllegalStateException("A competência " + compNormalizada + " já está encerrada. Reabra o período no Simulador do Fator R.");
        }

        folha.setEmpresaId(canonicalUuid);
        folha.setCompetencia(compNormalizada);

        // Trata valores nulos com BigDecimal.ZERO
        folha.setValorSalarios(folha.getValorSalarios() != null ? folha.getValorSalarios() : BigDecimal.ZERO);
        folha.setValorProLabore(folha.getValorProLabore() != null ? folha.getValorProLabore() : BigDecimal.ZERO);
        folha.setValorDecimoTerceiro(folha.getValorDecimoTerceiro() != null ? folha.getValorDecimoTerceiro() : BigDecimal.ZERO);
        folha.setValorInssPatronal(folha.getValorInssPatronal() != null ? folha.getValorInssPatronal() : BigDecimal.ZERO);
        folha.setValorFgts(folha.getValorFgts() != null ? folha.getValorFgts() : BigDecimal.ZERO);

        // Recalcula total da folha detalhada
        BigDecimal total = folha.getValorSalarios()
                .add(folha.getValorProLabore())
                .add(folha.getValorDecimoTerceiro())
                .add(folha.getValorInssPatronal())
                .add(folha.getValorFgts())
                .setScale(2, RoundingMode.HALF_UP);
        folha.setValorTotal(total);

        if (folha.getOrigem() == null || folha.getOrigem().isBlank()) {
            folha.setOrigem("MANUAL");
        }

        // Verifica existência para update ou insert
        Optional<FolhaDetalhada> existenteOpt = folhaDetalhadaRepository.findByEmpresaIdAndCompetencia(canonicalUuid, compNormalizada);
        if (existenteOpt.isPresent()) {
            FolhaDetalhada existente = existenteOpt.get();
            existente.setValorSalarios(folha.getValorSalarios());
            existente.setValorProLabore(folha.getValorProLabore());
            existente.setValorDecimoTerceiro(folha.getValorDecimoTerceiro());
            existente.setValorInssPatronal(folha.getValorInssPatronal());
            existente.setValorFgts(folha.getValorFgts());
            existente.setValorTotal(total);
            existente.setOrigem(folha.getOrigem());
            if (folha.getXmlChaveRecibo() != null) {
                existente.setXmlChaveRecibo(folha.getXmlChaveRecibo());
            }
            existente.setUpdatedAt(LocalDateTime.now());
            folha = folhaDetalhadaRepository.save(existente);
        } else {
            if (folha.getId() == null || folha.getId().isBlank()) {
                folha.setId(UUID.randomUUID().toString());
            }
            folha.setCreatedAt(LocalDateTime.now());
            folha.setUpdatedAt(LocalDateTime.now());
            folha = folhaDetalhadaRepository.save(folha);
        }

        // Sincronização automática com FolhaPagamentoMensal (para cálculo imediato do Fator R)
        sincronizarFolhaMensal(empresa, compNormalizada, total);

        return folha;
    }

    @Transactional
    public void deleteFolha(String id) {
        Optional<FolhaDetalhada> opt = folhaDetalhadaRepository.findById(id);
        if (opt.isPresent()) {
            FolhaDetalhada f = opt.get();
            Empresa empresa = tenantResolverService.resolveEmpresa(f.getEmpresaId());
            YearMonth ym = YearMonth.parse(f.getCompetencia());
            if (encerramentoRepository.findByEmpresaIdAndCompetencia(empresa.getId(), ym)
                    .map(EncerramentoCompetencia::isEncerrado).orElse(false)) {
                throw new IllegalStateException("A competência " + f.getCompetencia() + " já está encerrada. Reabra o período no Simulador do Fator R.");
            }
            try {
                List<FolhaPagamentoMensal> existentes = folhaPagamentoMensalRepository.findByEmpresaIdAndCompetencia(empresa.getId(), ym);
                if (!existentes.isEmpty()) {
                    folhaPagamentoMensalRepository.deleteAll(existentes);
                }
            } catch (Exception e) {
                log.warn("Erro ao limpar FolhaPagamentoMensal na exclusão da FolhaDetalhada: {}", e.getMessage());
            }
            folhaDetalhadaRepository.deleteById(id);
        }
    }

    private void sincronizarFolhaMensal(Empresa empresa, String competencia, BigDecimal valorTotal) {
        YearMonth ym = YearMonth.parse(competencia);
        List<FolhaPagamentoMensal> mensais = folhaPagamentoMensalRepository.findByEmpresaIdAndCompetencia(empresa.getId(), ym);

        FolhaPagamentoMensal folhaMensal;
        if (!mensais.isEmpty()) {
            folhaMensal = mensais.get(0);
            folhaMensal.setValor(valorTotal);
            if (mensais.size() > 1) {
                for (int i = 1; i < mensais.size(); i++) {
                    folhaPagamentoMensalRepository.delete(mensais.get(i));
                }
            }
        } else {
            folhaMensal = new FolhaPagamentoMensal();
            folhaMensal.setEmpresa(empresa);
            folhaMensal.setCompetencia(ym);
            folhaMensal.setValor(valorTotal);
        }
        folhaPagamentoMensalRepository.save(folhaMensal);
        log.info("FolhaPagamentoMensal sincronizada para empresa {} na competência {} com valor R$ {}",
                empresa.getId(), ym, valorTotal);
    }

    // ==========================================
    // 3. PARSER XML ESOCIAL (S-1200 / S-1210)
    // ==========================================

    @Transactional
    public ImportFolhaResponseDTO importarEsocialXml(MultipartFile file, String empresaId) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo XML está vazio");
        }

        Empresa empresa = tenantResolverService.resolveEmpresa(empresaId);
        byte[] xmlBytes = file.getBytes();

        // Parser DOM seguro contra XXE
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));
        doc.getDocumentElement().normalize();

        // 1. Extração da competência
        String competencia = extrairTextoTag(doc, "perApur");
        if (competencia == null || competencia.isBlank()) {
            competencia = extrairTextoTag(doc, "competencia");
        }
        if (competencia == null || competencia.isBlank()) {
            competencia = YearMonth.now().toString();
        }
        competencia = normalizarCompetencia(competencia);

        // 2. Extração de chave / recibo
        String chaveRecibo = extrairAtributoOuTag(doc, "evtRemun", "Id");
        if (chaveRecibo == null || chaveRecibo.isBlank()) {
            chaveRecibo = extrairAtributoOuTag(doc, "evtPgtos", "Id");
        }
        if (chaveRecibo == null || chaveRecibo.isBlank()) {
            chaveRecibo = extrairTextoTag(doc, "nrRecibo");
        }
        if (chaveRecibo == null || chaveRecibo.isBlank()) {
            chaveRecibo = extrairTextoTag(doc, "xmlChaveRecibo");
        }
        if (chaveRecibo == null || chaveRecibo.isBlank()) {
            chaveRecibo = "REC-ESOCIAL-" + System.currentTimeMillis();
        }

        // 3. Extração e Discriminação de Trabalhadores e Rubricas
        BigDecimal salarios = BigDecimal.ZERO;
        BigDecimal proLabore = BigDecimal.ZERO;
        BigDecimal decimoTerceiro = BigDecimal.ZERO;
        BigDecimal inssPatronal = BigDecimal.ZERO;
        BigDecimal fgts = BigDecimal.ZERO;

        int totalColaboradores = 0;
        int totalSocios = 0;

        // Verifica tags diretas consolidadas (caso exportação ERP em formato XML direto)
        BigDecimal tagSalarios = extrairValorDecimal(doc, "valorSalarios");
        BigDecimal tagProLabore = extrairValorDecimal(doc, "valorProLabore");
        BigDecimal tag13 = extrairValorDecimal(doc, "valorDecimoTerceiro");
        BigDecimal tagInss = extrairValorDecimal(doc, "valorInssPatronal");
        BigDecimal tagFgts = extrairValorDecimal(doc, "valorFgts");

        if (tagSalarios.compareTo(BigDecimal.ZERO) > 0 || tagProLabore.compareTo(BigDecimal.ZERO) > 0) {
            salarios = tagSalarios;
            proLabore = tagProLabore;
            decimoTerceiro = tag13;
            inssPatronal = tagInss;
            fgts = tagFgts;
            totalColaboradores = salarios.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0;
            totalSocios = proLabore.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0;
        } else {
            // Processa estrutura de eventos eSocial S-1200 / S-1210
            NodeList trabalhadores = buscarElementosPorNomeLocal(doc, "ideTrabalhador");
            if (trabalhadores.getLength() == 0) {
                trabalhadores = buscarElementosPorNomeLocal(doc, "ideBenef");
            }

            if (trabalhadores.getLength() > 0) {
                for (int i = 0; i < trabalhadores.getLength(); i++) {
                    Element trab = (Element) trabalhadores.item(i);
                    boolean isSocio = false;

                    // Verifica codCateg (categorias 7xx: 701, 711, 712, 721, 722, 723, etc. = sócios/diretores)
                    String codCateg = extrairTextoDescendente(trab, "codCateg");
                    if (codCateg != null && codCateg.startsWith("7")) {
                        isSocio = true;
                        totalSocios++;
                    } else {
                        totalColaboradores++;
                    }

                    // Percorre itens de remuneração
                    NodeList itensRemun = buscarElementosDescendentes(trab, "itensRemun");
                    if (itensRemun.getLength() == 0) {
                        itensRemun = buscarElementosDescendentes(trab, "infoPgto");
                    }

                    for (int j = 0; j < itensRemun.getLength(); j++) {
                        Element item = (Element) itensRemun.item(j);
                        BigDecimal valor = extrairValorDescendente(item, "vrRubr");
                        if (valor.compareTo(BigDecimal.ZERO) == 0) {
                            valor = extrairValorDescendente(item, "vrLiq");
                        }
                        if (valor.compareTo(BigDecimal.ZERO) == 0) {
                            valor = extrairValorDescendente(item, "valor");
                        }

                        String codRubr = extrairTextoDescendente(item, "codRubr");
                        String natRubr = extrairTextoDescendente(item, "natRubr");
                        String dscRubr = extrairTextoDescendente(item, "dscRubr");

                        boolean rubricaProLabore = isRubricaProLabore(codRubr, natRubr, dscRubr);
                        boolean rubrica13 = isRubrica13(codRubr, natRubr, dscRubr);

                        if (rubrica13) {
                            decimoTerceiro = decimoTerceiro.add(valor);
                        } else if (isSocio || rubricaProLabore) {
                            proLabore = proLabore.add(valor);
                        } else {
                            salarios = salarios.add(valor);
                        }
                    }
                }
            } else {
                // Fallback: varre rubricas soltas em itensRemun
                NodeList itensRemun = buscarElementosPorNomeLocal(doc, "itensRemun");
                for (int i = 0; i < itensRemun.getLength(); i++) {
                    Element item = (Element) itensRemun.item(i);
                    BigDecimal valor = extrairValorDescendente(item, "vrRubr");
                    String codRubr = extrairTextoDescendente(item, "codRubr");
                    String natRubr = extrairTextoDescendente(item, "natRubr");
                    String dscRubr = extrairTextoDescendente(item, "dscRubr");

                    if (isRubricaProLabore(codRubr, natRubr, dscRubr)) {
                        proLabore = proLabore.add(valor);
                    } else if (isRubrica13(codRubr, natRubr, dscRubr)) {
                        decimoTerceiro = decimoTerceiro.add(valor);
                    } else {
                        salarios = salarios.add(valor);
                    }
                }
            }

            // Extração de encargos (INSS e FGTS) se disponíveis
            BigDecimal vrFgts = extrairValorDecimal(doc, "vrFgts");
            if (vrFgts.compareTo(BigDecimal.ZERO) == 0) vrFgts = extrairValorDecimal(doc, "valorFgts");
            if (vrFgts.compareTo(BigDecimal.ZERO) == 0) vrFgts = extrairValorDecimal(doc, "fgts");
            fgts = vrFgts;

            BigDecimal vrInss = extrairValorDecimal(doc, "vrCp");
            if (vrInss.compareTo(BigDecimal.ZERO) == 0) vrInss = extrairValorDecimal(doc, "vrInssPatronal");
            if (vrInss.compareTo(BigDecimal.ZERO) == 0) vrInss = extrairValorDecimal(doc, "valorInssPatronal");
            inssPatronal = vrInss;
        }

        if (totalColaboradores == 0 && salarios.compareTo(BigDecimal.ZERO) > 0) totalColaboradores = 1;
        if (totalSocios == 0 && proLabore.compareTo(BigDecimal.ZERO) > 0) totalSocios = 1;

        FolhaDetalhada novaFolha = new FolhaDetalhada();
        novaFolha.setEmpresaId(empresa.getUuidId() != null ? empresa.getUuidId() : String.valueOf(empresa.getId()));
        novaFolha.setCompetencia(competencia);
        novaFolha.setValorSalarios(salarios.setScale(2, RoundingMode.HALF_UP));
        novaFolha.setValorProLabore(proLabore.setScale(2, RoundingMode.HALF_UP));
        novaFolha.setValorDecimoTerceiro(decimoTerceiro.setScale(2, RoundingMode.HALF_UP));
        novaFolha.setValorInssPatronal(inssPatronal.setScale(2, RoundingMode.HALF_UP));
        novaFolha.setValorFgts(fgts.setScale(2, RoundingMode.HALF_UP));
        novaFolha.setOrigem("IMPORTACAO_ESOCIAL");
        novaFolha.setXmlChaveRecibo(chaveRecibo);

        FolhaDetalhada salva = saveFolha(novaFolha);
        BigDecimal encargos = salva.getValorInssPatronal().add(salva.getValorFgts());

        return ImportFolhaResponseDTO.builder()
                .sucesso(true)
                .mensagem("Eventos do eSocial processados e integrados com sucesso.")
                .empresaId(salva.getEmpresaId())
                .competencia(salva.getCompetencia())
                .totalColaboradores(totalColaboradores)
                .totalSocios(totalSocios)
                .valorSalarios(salva.getValorSalarios())
                .valorProLabore(salva.getValorProLabore())
                .valorDecimoTerceiro(salva.getValorDecimoTerceiro())
                .valorInssPatronal(salva.getValorInssPatronal())
                .valorFgts(salva.getValorFgts())
                .valorEncargos(encargos)
                .valorTotalFolha(salva.getValorTotal())
                .folhaGerada(salva)
                .xmlChaveRecibo(salva.getXmlChaveRecibo())
                .origem("IMPORTACAO_ESOCIAL")
                .build();
    }

    private boolean isRubricaProLabore(String codRubr, String natRubr, String dscRubr) {
        if (natRubr != null) {
            String nr = natRubr.trim();
            if (nr.equals("3501") || nr.equals("3505") || nr.equals("3508") || nr.equals("3509") || nr.equals("3520")) {
                return true;
            }
        }
        String texto = (safeStr(codRubr) + " " + safeStr(dscRubr)).toUpperCase();
        return texto.contains("PROLABORE") || texto.contains("PRO-LABORE") || texto.contains("PRO LABORE")
                || texto.contains("PRÓ-LABORE") || texto.contains("RETIRADA");
    }

    private boolean isRubrica13(String codRubr, String natRubr, String dscRubr) {
        if (natRubr != null) {
            String nr = natRubr.trim();
            if (nr.startsWith("50") || nr.equals("5001") || nr.equals("5002") || nr.equals("5003")) {
                return true;
            }
        }
        String texto = (safeStr(codRubr) + " " + safeStr(dscRubr)).toUpperCase();
        return texto.contains("13") || texto.contains("DECIMO") || texto.contains("DÉCIMO") || texto.contains("NATALINA");
    }

    private String safeStr(String s) {
        return s == null ? "" : s;
    }

    // ==========================================
    // 4. PARSER CSV / EXCEL (.XLSX) DE ERPS
    // ==========================================

    @Transactional
    public ImportFolhaResponseDTO importarCsvOuExcel(MultipartFile file, String empresaId) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de folha está vazio");
        }

        Empresa empresa = tenantResolverService.resolveEmpresa(empresaId);
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        List<FolhaDetalhada> folhasProcessadas = new ArrayList<>();

        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            folhasProcessadas = parseExcel(file.getInputStream(), empresa);
        } else {
            folhasProcessadas = parseCsv(file.getInputStream(), empresa);
        }

        if (folhasProcessadas.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma linha válida de folha de pagamento foi encontrada no arquivo");
        }

        // Salva e sincroniza cada competência lida
        FolhaDetalhada ultimaSalva = null;
        BigDecimal somaSalarios = BigDecimal.ZERO;
        BigDecimal somaProLabore = BigDecimal.ZERO;
        BigDecimal somaEncargos = BigDecimal.ZERO;
        BigDecimal somaTotal = BigDecimal.ZERO;

        for (FolhaDetalhada f : folhasProcessadas) {
            f.setOrigem("IMPORTACAO_CSV");
            ultimaSalva = saveFolha(f);
            somaSalarios = somaSalarios.add(ultimaSalva.getValorSalarios());
            somaProLabore = somaProLabore.add(ultimaSalva.getValorProLabore());
            somaEncargos = somaEncargos.add(ultimaSalva.getValorInssPatronal().add(ultimaSalva.getValorFgts()));
            somaTotal = somaTotal.add(ultimaSalva.getValorTotal());
        }

        return ImportFolhaResponseDTO.builder()
                .sucesso(true)
                .mensagem(folhasProcessadas.size() + " competência(s) de folha de pagamento importadas com sucesso.")
                .empresaId(ultimaSalva.getEmpresaId())
                .competencia(ultimaSalva.getCompetencia())
                .totalColaboradores(somaSalarios.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0)
                .totalSocios(somaProLabore.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0)
                .valorSalarios(ultimaSalva.getValorSalarios())
                .valorProLabore(ultimaSalva.getValorProLabore())
                .valorDecimoTerceiro(ultimaSalva.getValorDecimoTerceiro())
                .valorInssPatronal(ultimaSalva.getValorInssPatronal())
                .valorFgts(ultimaSalva.getValorFgts())
                .valorEncargos(ultimaSalva.getValorInssPatronal().add(ultimaSalva.getValorFgts()))
                .valorTotalFolha(ultimaSalva.getValorTotal())
                .folhaGerada(ultimaSalva)
                .origem("IMPORTACAO_CSV")
                .build();
    }

    private List<FolhaDetalhada> parseExcel(InputStream is, Empresa empresa) throws Exception {
        List<FolhaDetalhada> lista = new ArrayList<>();
        Workbook workbook = WorkbookFactory.create(is);
        Sheet sheet = workbook.getSheetAt(0);

        if (sheet.getPhysicalNumberOfRows() < 2) {
            return lista;
        }

        Row headerRow = sheet.getRow(0);
        Map<String, Integer> colunas = mapearColunasHeader(headerRow);

        DataFormatter formatter = new DataFormatter();
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String comp = getCellValue(row, colunas.get("competencia"), formatter);
            if (comp == null || comp.isBlank()) continue;

            FolhaDetalhada f = new FolhaDetalhada();
            f.setEmpresaId(empresa.getUuidId() != null ? empresa.getUuidId() : String.valueOf(empresa.getId()));
            f.setCompetencia(normalizarCompetencia(comp));
            f.setValorSalarios(parseBigDecimal(getCellValue(row, colunas.get("salarios"), formatter)));
            f.setValorProLabore(parseBigDecimal(getCellValue(row, colunas.get("prolabore"), formatter)));
            f.setValorDecimoTerceiro(parseBigDecimal(getCellValue(row, colunas.get("decimoterceiro"), formatter)));
            f.setValorInssPatronal(parseBigDecimal(getCellValue(row, colunas.get("inss"), formatter)));
            f.setValorFgts(parseBigDecimal(getCellValue(row, colunas.get("fgts"), formatter)));

            lista.add(f);
        }

        workbook.close();
        return lista;
    }

    private List<FolhaDetalhada> parseCsv(InputStream is, Empresa empresa) throws Exception {
        List<FolhaDetalhada> lista = new ArrayList<>();
        byte[] bytes = is.readAllBytes();

        // Detecta separador (; ou ,)
        String primeiraLinha = "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            primeiraLinha = reader.readLine();
        }
        if (primeiraLinha == null) return lista;

        char delimiter = primeiraLinha.chars().filter(c -> c == ';').count() > primeiraLinha.chars().filter(c -> c == ',').count() ? ';' : ',';

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            CSVParser parser = format.parse(reader);
            Map<String, Integer> headerMap = parser.getHeaderMap();
            Map<String, String> colunasMapeadas = mapearColunasCsv(headerMap.keySet());

            for (CSVRecord record : parser) {
                String comp = getCsvVal(record, colunasMapeadas.get("competencia"));
                if (comp == null || comp.isBlank()) continue;

                FolhaDetalhada f = new FolhaDetalhada();
                f.setEmpresaId(empresa.getUuidId() != null ? empresa.getUuidId() : String.valueOf(empresa.getId()));
                f.setCompetencia(normalizarCompetencia(comp));
                f.setValorSalarios(parseBigDecimal(getCsvVal(record, colunasMapeadas.get("salarios"))));
                f.setValorProLabore(parseBigDecimal(getCsvVal(record, colunasMapeadas.get("prolabore"))));
                f.setValorDecimoTerceiro(parseBigDecimal(getCsvVal(record, colunasMapeadas.get("decimoterceiro"))));
                f.setValorInssPatronal(parseBigDecimal(getCsvVal(record, colunasMapeadas.get("inss"))));
                f.setValorFgts(parseBigDecimal(getCsvVal(record, colunasMapeadas.get("fgts"))));

                lista.add(f);
            }
        }
        return lista;
    }

    private Map<String, Integer> mapearColunasHeader(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) continue;
            String text = normalizarTexto(formatter.formatCellValue(cell));

            if (text.contains("competencia") || text.contains("periodo") || text.contains("mesano") || text.contains("mes")) {
                map.putIfAbsent("competencia", c);
            } else if (text.contains("salario") || text.contains("ordenado") || text.contains("remuneracao") || text.contains("provento")) {
                map.putIfAbsent("salarios", c);
            } else if (text.contains("prolabore") || text.contains("pro_labore") || text.contains("retirada") || text.contains("socio")) {
                map.putIfAbsent("prolabore", c);
            } else if (text.contains("decimo") || text.contains("13")) {
                map.putIfAbsent("decimoterceiro", c);
            } else if (text.contains("inss") || text.contains("cpp") || text.contains("patronal")) {
                map.putIfAbsent("inss", c);
            } else if (text.contains("fgts")) {
                map.putIfAbsent("fgts", c);
            }
        }
        return map;
    }

    private Map<String, String> mapearColunasCsv(Set<String> headers) {
        Map<String, String> map = new HashMap<>();
        for (String h : headers) {
            String text = normalizarTexto(h);
            if (text.contains("competencia") || text.contains("periodo") || text.contains("mesano") || text.contains("mes")) {
                map.putIfAbsent("competencia", h);
            } else if (text.contains("salario") || text.contains("ordenado") || text.contains("remuneracao") || text.contains("provento")) {
                map.putIfAbsent("salarios", h);
            } else if (text.contains("prolabore") || text.contains("pro_labore") || text.contains("retirada") || text.contains("socio")) {
                map.putIfAbsent("prolabore", h);
            } else if (text.contains("decimo") || text.contains("13")) {
                map.putIfAbsent("decimoterceiro", h);
            } else if (text.contains("inss") || text.contains("cpp") || text.contains("patronal")) {
                map.putIfAbsent("inss", h);
            } else if (text.contains("fgts")) {
                map.putIfAbsent("fgts", h);
            }
        }
        return map;
    }

    private String getCellValue(Row row, Integer colIndex, DataFormatter formatter) {
        if (colIndex == null) return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        return formatter.formatCellValue(cell);
    }

    private String getCsvVal(CSVRecord record, String colName) {
        if (colName == null || !record.isMapped(colName)) return null;
        return record.get(colName);
    }

    // ====================================================
    // 5. CALCULADORA DE PRÓ-LABORE MÍNIMO IDEAL (FATOR R)
    // ====================================================

    @Transactional(readOnly = true)
    public ProLaboreIdealDTO calcularProLaboreIdeal(String empresaId, String competencia, BigDecimal faturamentoEstimado) {
        Empresa empresa = tenantResolverService.resolveEmpresa(empresaId);
        Long empresaNumericId = empresa.getId();
        YearMonth competenciaYM = YearMonth.parse(normalizarCompetencia(competencia));

        // Busca histórico dos últimos 12 meses anteriores à competência
        YearMonth dataFim = competenciaYM.minusMonths(1);
        YearMonth dataInicio = competenciaYM.minusMonths(12);

        List<FaturamentoMensal> faturamentos = faturamentoRepository.findUltimos12Meses(empresaNumericId, dataInicio, dataFim);
        List<FolhaPagamentoMensal> folhas = folhaPagamentoMensalRepository.findUltimos12Meses(empresaNumericId, dataInicio, dataFim);

        BigDecimal totalFaturamento12 = faturamentos.stream()
                .map(FaturamentoMensal::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFolha12 = folhas.stream()
                .map(FolhaPagamentoMensal::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Faturamento do mês da simulação
        BigDecimal faturamentoMes = faturamentoEstimado;
        if (faturamentoMes == null || faturamentoMes.compareTo(BigDecimal.ZERO) <= 0) {
            List<FaturamentoMensal> doMes = faturamentoRepository.findByEmpresaIdAndCompetencia(empresaNumericId, competenciaYM);
            faturamentoMes = doMes.stream()
                    .map(FaturamentoMensal::getValor)
                    .reduce(BigDecimal::add)
                    .orElse(totalFaturamento12.compareTo(BigDecimal.ZERO) > 0
                            ? totalFaturamento12.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO);
        }

        // RBT12: se for zero (empresa nova), anualiza faturamentoMes
        BigDecimal rbt12 = totalFaturamento12;
        if (rbt12.compareTo(BigDecimal.ZERO) == 0 && faturamentoMes.compareTo(BigDecimal.ZERO) > 0) {
            rbt12 = faturamentoMes.multiply(new BigDecimal("12"));
        }

        // Fator R Atual
        BigDecimal fatorRAtual = BigDecimal.ZERO;
        String anexoAtual = "Anexo V";
        if (rbt12.compareTo(BigDecimal.ZERO) > 0) {
            fatorRAtual = totalFolha12.divide(rbt12, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            if (fatorRAtual.compareTo(new BigDecimal("28")) >= 0) {
                anexoAtual = "Anexo III";
            }
        }

        // Cálculo da folha necessária para Fator R >= 28%
        BigDecimal folhaAlvo = rbt12.multiply(FATOR_R_ALVO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal proLaboreNecessario = BigDecimal.ZERO;

        if (totalFolha12.compareTo(folhaAlvo) < 0) {
            proLaboreNecessario = folhaAlvo.subtract(totalFolha12).setScale(2, RoundingMode.HALF_UP);
        }

        // Fator R Projetado
        BigDecimal fatorRProjetado = new BigDecimal("28.00");
        if (proLaboreNecessario.compareTo(BigDecimal.ZERO) == 0 && fatorRAtual.compareTo(BigDecimal.ZERO) > 0) {
            fatorRProjetado = fatorRAtual.setScale(2, RoundingMode.HALF_UP);
        }

        // Cálculo Previdenciário (INSS do Sócio - 11% com teto 2026 de R$ 908,86 sobre base R$ 8.258,74)
        BigDecimal inssSocio = BigDecimal.ZERO;
        if (proLaboreNecessario.compareTo(BigDecimal.ZERO) > 0) {
            if (proLaboreNecessario.compareTo(TETO_BASE_INSS_2026) >= 0) {
                inssSocio = TETO_VALOR_INSS_2026;
            } else {
                inssSocio = proLaboreNecessario.multiply(ALIQUOTA_INSS_SOCIO).setScale(2, RoundingMode.HALF_UP);
                if (inssSocio.compareTo(TETO_VALOR_INSS_2026) > 0) {
                    inssSocio = TETO_VALOR_INSS_2026;
                }
            }
        }

        // Cálculo do IRPF Retido na Fonte (IRRF) sobre Pró-Labore Adicional (Base = Pró-labore - INSS)
        BigDecimal baseIrpf = proLaboreNecessario.subtract(inssSocio);
        BigDecimal irpfSocio = calcularIrpfRetido(baseIrpf);
        BigDecimal custoTributarioTotal = inssSocio.add(irpfSocio).setScale(2, RoundingMode.HALF_UP);

        // Alíquotas Efetivas Simples Nacional: Anexo V vs Anexo III
        ParametroTributario params = obterParametros(empresaNumericId);
        BigDecimal aliqEfetivaV = calcularAliquotaEfetivaSimples(rbt12, "V", params);
        BigDecimal aliqEfetivaIII = calcularAliquotaEfetivaSimples(rbt12, "III", params);

        // Economia Tributária Simples Nacional = (Aliq V - Aliq III) * Faturamento do Mês
        BigDecimal diferencaAliquotas = aliqEfetivaV.subtract(aliqEfetivaIII);
        if (diferencaAliquotas.compareTo(BigDecimal.ZERO) < 0) {
            diferencaAliquotas = BigDecimal.ZERO;
        }
        BigDecimal economiaSimplesNacional = faturamentoMes.multiply(diferencaAliquotas).setScale(2, RoundingMode.HALF_UP);

        // Saldo Líquido de Vantagem = Economia Simples Nacional - Custo Tributário Adicional (INSS + IRPF)
        BigDecimal saldoLiquidoVantagem = economiaSimplesNacional.subtract(custoTributarioTotal).setScale(2, RoundingMode.HALF_UP);
        boolean vantajoso = saldoLiquidoVantagem.compareTo(BigDecimal.ZERO) > 0;

        String recomendacao;
        if (proLaboreNecessario.compareTo(BigDecimal.ZERO) == 0) {
            recomendacao = "A empresa já atinge a margem de 28% no Fator R (" + fatorRAtual.setScale(2, RoundingMode.HALF_UP) + "%), estando enquadrada no Anexo III sem necessidade de aporte de pró-labore adicional.";
        } else if (vantajoso) {
            recomendacao = String.format("Altamente Vantajoso! Retirar pró-labore de R$ %,.2f gera economia no DAS de R$ %,.2f contra um custo de R$ %,.2f em impostos pessoais (INSS + IRPF), resultando em lucro líquido extra de R$ %,.2f no mês.",
                    proLaboreNecessario, economiaSimplesNacional, custoTributarioTotal, saldoLiquidoVantagem);
        } else {
            recomendacao = String.format("Atenção: Para esta faixa de faturamento, o custo dos encargos da pessoa física (R$ %,.2f) supera a economia gerada no Simples Nacional (R$ %,.2f). Saldo negativo de R$ %,.2f.",
                    custoTributarioTotal, economiaSimplesNacional, saldoLiquidoVantagem);
        }

        ProLaboreIdealDTO.CustoTributarioAdicionalDTO custoDTO = ProLaboreIdealDTO.CustoTributarioAdicionalDTO.builder()
                .inssSocio(inssSocio)
                .irpfSocio(irpfSocio)
                .custoTotal(custoTributarioTotal)
                .build();

        return ProLaboreIdealDTO.builder()
                .empresaId(empresa.getUuidId() != null ? empresa.getUuidId() : String.valueOf(empresa.getId()))
                .razaoSocial(empresa.getRazaoSocial())
                .competencia(competenciaYM.toString())
                .rbt12(rbt12.setScale(2, RoundingMode.HALF_UP))
                .folha12Atual(totalFolha12.setScale(2, RoundingMode.HALF_UP))
                .fatorRAtual(fatorRAtual.setScale(2, RoundingMode.HALF_UP))
                .anexoAtual(anexoAtual)
                .faturamentoPrevisto(faturamentoMes.setScale(2, RoundingMode.HALF_UP))
                .folhaAtualSemProLabore(totalFolha12.setScale(2, RoundingMode.HALF_UP))
                .proLaboreMinimoParaAnexo3(proLaboreNecessario)
                .fatorRProjetado(fatorRProjetado)
                .anexoProjetado("Anexo III")
                .custoTributarioAdicional(custoDTO)
                .inssSocio(inssSocio)
                .irpfSocio(irpfSocio)
                .custoTributarioTotal(custoTributarioTotal)
                .aliquotaEfetivaAnexoV(aliqEfetivaV.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP))
                .aliquotaEfetivaAnexoIII(aliqEfetivaIII.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP))
                .economiaSimplesNacional(economiaSimplesNacional)
                .saldoLiquidoVantagem(saldoLiquidoVantagem)
                .vantajoso(vantajoso)
                .mensagemRecomendacao(recomendacao)
                .build();
    }

    private BigDecimal calcularIrpfRetido(BigDecimal baseIrpf) {
        if (baseIrpf == null || baseIrpf.compareTo(new BigDecimal("2259.20")) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal imposto;
        if (baseIrpf.compareTo(new BigDecimal("2826.65")) <= 0) {
            // Faixa 2: 7.5% - 169.44
            imposto = baseIrpf.multiply(new BigDecimal("0.075")).subtract(new BigDecimal("169.44"));
        } else if (baseIrpf.compareTo(new BigDecimal("3751.05")) <= 0) {
            // Faixa 3: 15.0% - 381.44
            imposto = baseIrpf.multiply(new BigDecimal("0.15")).subtract(new BigDecimal("381.44"));
        } else if (baseIrpf.compareTo(new BigDecimal("4664.68")) <= 0) {
            // Faixa 4: 22.5% - 662.77
            imposto = baseIrpf.multiply(new BigDecimal("0.225")).subtract(new BigDecimal("662.77"));
        } else {
            // Faixa 5: 27.5% - 896.00
            imposto = baseIrpf.multiply(new BigDecimal("0.275")).subtract(new BigDecimal("896.00"));
        }

        if (imposto.compareTo(BigDecimal.ZERO) < 0) {
            imposto = BigDecimal.ZERO;
        }
        return imposto.setScale(2, RoundingMode.HALF_UP);
    }

    private ParametroTributario obterParametros(Long empresaId) {
        return parametroRepository.findByAtivoTrueAndEmpresaId(empresaId)
                .orElseGet(() -> parametroRepository.findByAtivoTrueAndEmpresaIsNull().orElse(null));
    }

    private BigDecimal calcularAliquotaEfetivaSimples(BigDecimal rbt12, String anexo, ParametroTributario params) {
        if (params != null && params.getFaixas() != null && !params.getFaixas().isEmpty()) {
            final String anexoBusca = anexo.toUpperCase().contains("III") ? "III" : "V";
            final String anexoBuscaExtenso = "Anexo " + anexoBusca;
            FaixaTributaria faixa = params.getFaixas().stream()
                    .filter(f -> f.getAnexo() != null && (f.getAnexo().equalsIgnoreCase(anexoBusca) || f.getAnexo().equalsIgnoreCase(anexoBuscaExtenso)))
                    .filter(f -> f.getLimiteSuperior() == null || rbt12.compareTo(f.getLimiteSuperior()) <= 0)
                    .min(Comparator.comparing(FaixaTributaria::getNumeroFaixa))
                    .orElse(null);
            if (faixa != null) {
                if (rbt12.compareTo(BigDecimal.ZERO) == 0) {
                    return faixa.getAliquotaNominal();
                }
                BigDecimal imposto = rbt12.multiply(faixa.getAliquotaNominal()).subtract(faixa.getValorDeduzir());
                if (imposto.compareTo(BigDecimal.ZERO) < 0) imposto = BigDecimal.ZERO;
                return imposto.divide(rbt12, 4, RoundingMode.HALF_UP);
            }
        }
        return calcularAliquotaEfetivaOficial(rbt12, anexo);
    }

    private BigDecimal calcularAliquotaEfetivaOficial(BigDecimal rbt12, String anexo) {
        boolean isAnexo3 = anexo.toUpperCase().contains("III");

        // Alíquotas e Deduções Oficiais da LC 123/2006
        BigDecimal aliqNominal;
        BigDecimal deducao;

        if (isAnexo3) {
            if (rbt12.compareTo(new BigDecimal("180000.00")) <= 0) {
                aliqNominal = new BigDecimal("0.0600");
                deducao = BigDecimal.ZERO;
            } else if (rbt12.compareTo(new BigDecimal("360000.00")) <= 0) {
                aliqNominal = new BigDecimal("0.1120");
                deducao = new BigDecimal("9360.00");
            } else if (rbt12.compareTo(new BigDecimal("720000.00")) <= 0) {
                aliqNominal = new BigDecimal("0.1350");
                deducao = new BigDecimal("17640.00");
            } else if (rbt12.compareTo(new BigDecimal("1800000.00")) <= 0) {
                aliqNominal = new BigDecimal("0.1600");
                deducao = new BigDecimal("35640.00");
            } else if (rbt12.compareTo(new BigDecimal("3600000.00")) <= 0) {
                aliqNominal = new BigDecimal("0.2100");
                deducao = new BigDecimal("125640.00");
            } else {
                aliqNominal = new BigDecimal("0.3300");
                deducao = new BigDecimal("648000.00");
            }
        } else {
            // Anexo V
            if (rbt12.compareTo(new BigDecimal("180000.00")) <= 0) {
                aliqNominal = new BigDecimal("0.1550");
                deducao = BigDecimal.ZERO;
            } else if (rbt12.compareTo(new BigDecimal("360000.00")) <= 0) {
                aliqNominal = new BigDecimal("0.1800");
                deducao = new BigDecimal("4500.00");
            } else if (rbt12.compareTo(new BigDecimal("720000.00")) <= 0) {
                aliqNominal = new BigDecimal("0.1950");
                deducao = new BigDecimal("9900.00");
            } else if (rbt12.compareTo(new BigDecimal("1800000.00")) <= 0) {
                aliqNominal = new BigDecimal("0.2050");
                deducao = new BigDecimal("17100.00");
            } else if (rbt12.compareTo(new BigDecimal("3600000.00")) <= 0) {
                aliqNominal = new BigDecimal("0.2300");
                deducao = new BigDecimal("62100.00");
            } else {
                aliqNominal = new BigDecimal("0.3050");
                deducao = new BigDecimal("540000.00");
            }
        }

        if (rbt12.compareTo(BigDecimal.ZERO) == 0) {
            return aliqNominal;
        }

        BigDecimal imposto = rbt12.multiply(aliqNominal).subtract(deducao);
        if (imposto.compareTo(BigDecimal.ZERO) < 0) {
            imposto = BigDecimal.ZERO;
        }
        return imposto.divide(rbt12, 4, RoundingMode.HALF_UP);
    }

    // ==========================================
    // UTILS AUXILIARES (NORMALIZAÇÃO E XML)
    // ==========================================

    private String normalizarCompetencia(String comp) {
        if (comp == null || comp.isBlank()) {
            return YearMonth.now().toString();
        }
        String limpo = comp.trim().replace("/", "-");
        if (limpo.matches("^\\d{4}-\\d{2}$")) {
            return limpo;
        }
        if (limpo.matches("^\\d{2}-\\d{4}$")) {
            String[] parts = limpo.split("-");
            return parts[1] + "-" + parts[0];
        }
        if (limpo.matches("^\\d{6}$")) {
            // YYYYMM ou MMYYYY
            int primeiro = Integer.parseInt(limpo.substring(0, 4));
            if (primeiro >= 2000 && primeiro <= 2100) {
                return limpo.substring(0, 4) + "-" + limpo.substring(4, 6);
            } else {
                return limpo.substring(2, 6) + "-" + limpo.substring(0, 2);
            }
        }
        return limpo;
    }

    private String normalizarTexto(String s) {
        if (s == null) return "";
        String semAcentos = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcentos.toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    private BigDecimal parseBigDecimal(String s) {
        if (s == null || s.isBlank()) {
            return BigDecimal.ZERO;
        }
        String limpo = s.trim().replaceAll("[R$\\s]", "");
        if (limpo.contains(",") && limpo.contains(".")) {
            // Ex: 1.234,56
            limpo = limpo.replace(".", "").replace(",", ".");
        } else if (limpo.contains(",")) {
            // Ex: 1234,56
            limpo = limpo.replace(",", ".");
        }
        try {
            return new BigDecimal(limpo).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String extrairTextoTag(Document doc, String tagName) {
        NodeList nl = buscarElementosPorNomeLocal(doc, tagName);
        if (nl.getLength() > 0) {
            return nl.item(0).getTextContent().trim();
        }
        return null;
    }

    private BigDecimal extrairValorDecimal(Document doc, String tagName) {
        String texto = extrairTextoTag(doc, tagName);
        return parseBigDecimal(texto);
    }

    private String extrairAtributoOuTag(Document doc, String tagName, String attrName) {
        NodeList nl = buscarElementosPorNomeLocal(doc, tagName);
        if (nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            if (el.hasAttribute(attrName)) {
                return el.getAttribute(attrName).trim();
            }
        }
        return null;
    }

    private NodeList buscarElementosPorNomeLocal(Document doc, String localName) {
        NodeList nl = doc.getElementsByTagNameNS("*", localName);
        if (nl == null || nl.getLength() == 0) {
            nl = doc.getElementsByTagName(localName);
        }
        return nl;
    }

    private NodeList buscarElementosDescendentes(Element parent, String localName) {
        NodeList nl = parent.getElementsByTagNameNS("*", localName);
        if (nl == null || nl.getLength() == 0) {
            nl = parent.getElementsByTagName(localName);
        }
        return nl;
    }

    private String extrairTextoDescendente(Element parent, String localName) {
        NodeList nl = buscarElementosDescendentes(parent, localName);
        if (nl.getLength() > 0) {
            return nl.item(0).getTextContent().trim();
        }
        return null;
    }

    private BigDecimal extrairValorDescendente(Element parent, String localName) {
        String txt = extrairTextoDescendente(parent, localName);
        return parseBigDecimal(txt);
    }
}
