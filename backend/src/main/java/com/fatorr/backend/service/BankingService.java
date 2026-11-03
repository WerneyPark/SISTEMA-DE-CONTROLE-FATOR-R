package com.fatorr.backend.service;

import com.fatorr.backend.domain.ConciliacaoBancaria;
import com.fatorr.backend.domain.ContaReceber;
import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.domain.ExtratoLancamento;
import com.fatorr.backend.dto.ImportExtratoResponseDTO;
import com.fatorr.backend.repository.ConciliacaoBancariaRepository;
import com.fatorr.backend.repository.ContaReceberRepository;
import com.fatorr.backend.repository.ExtratoLancamentoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BankingService {

    private final ExtratoLancamentoRepository extratoLancamentoRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final ConciliacaoBancariaRepository conciliacaoBancariaRepository;
    private final TenantResolverService tenantResolverService;

    public record ConciliacaoDetalhadaDTO(
            String id,
            String extratoLancamentoId,
            String contaReceberId,
            String usuarioId,
            LocalDateTime dataConciliacao,
            String observacao,
            ExtratoLancamento extratoLancamento,
            ContaReceber contaReceber
    ) {}

    // ===================== MÉTODOS AUXILIARES DE TENANT =====================

    public List<String> resolverIdentificadoresEmpresa(String empresaId) {
        Empresa empresa = tenantResolverService.resolveEmpresa(empresaId);
        List<String> ids = new ArrayList<>();
        if (empresa.getUuidId() != null && !empresa.getUuidId().isBlank()) {
            ids.add(empresa.getUuidId());
        }
        if (empresa.getId() != null) {
            String numId = String.valueOf(empresa.getId());
            if (!ids.contains(numId)) {
                ids.add(numId);
            }
        }
        return ids;
    }

    private boolean existeLancamentoPorFitid(List<String> idsEmpresa, String fitid) {
        for (String id : idsEmpresa) {
            if (extratoLancamentoRepository.findByEmpresaIdAndFitid(id, fitid).isPresent()) {
                return true;
            }
        }
        return false;
    }

    // ===================== IMPORTAÇÃO OFX =====================

    private static final Pattern STMTTRN_PATTERN =
            Pattern.compile("<STMTTRN>(.*?)</STMTTRN>", Pattern.DOTALL);
    private static final Pattern BANKACCTFROM_PATTERN =
            Pattern.compile("<BANKACCTFROM>(.*?)</BANKACCTFROM>", Pattern.DOTALL);
    private static final Pattern TAG_PATTERN =
            Pattern.compile("<(\\w+)>([^<\\r\\n]*)");

    @Transactional
    public ImportExtratoResponseDTO importarOfx(MultipartFile file, String empresaId) throws IOException {
        String canonicalEmpresaId = tenantResolverService.resolveCanonicalUuid(empresaId);
        List<String> idsEmpresa = resolverIdentificadoresEmpresa(empresaId);

        String conteudo = new String(file.getBytes(), StandardCharsets.UTF_8);

        String banco = null;
        String conta = null;
        Matcher acctMatcher = BANKACCTFROM_PATTERN.matcher(conteudo);
        if (acctMatcher.find()) {
            Map<String, String> acctCampos = extrairCampos(acctMatcher.group(1));
            banco = acctCampos.get("BANKID");
            conta = acctCampos.get("ACCTID");
        }

        List<ExtratoLancamento> importados = new ArrayList<>();
        int duplicados = 0;
        int total = 0;

        Matcher trnMatcher = STMTTRN_PATTERN.matcher(conteudo);
        while (trnMatcher.find()) {
            total++;
            Map<String, String> campos = extrairCampos(trnMatcher.group(1));

            String fitid = campos.get("FITID");
            if (fitid == null || fitid.isBlank()) {
                continue;
            }

            if (existeLancamentoPorFitid(idsEmpresa, fitid)) {
                duplicados++;
                continue;
            }

            BigDecimal valor = new BigDecimal(campos.getOrDefault("TRNAMT", "0"));
            String tipo = valor.compareTo(BigDecimal.ZERO) < 0 ? "DEBITO" : "CREDITO";
            LocalDate data = parseOfxDate(campos.get("DTPOSTED"));
            String descricao = campos.getOrDefault("MEMO", campos.getOrDefault("NAME", "Lançamento sem descrição"));

            ExtratoLancamento lancamento = new ExtratoLancamento();
            lancamento.setId(UUID.randomUUID().toString());
            lancamento.setEmpresaId(canonicalEmpresaId);
            lancamento.setBanco(banco);
            lancamento.setConta(conta);
            lancamento.setDataLancamento(data);
            lancamento.setDescricao(descricao);
            lancamento.setValor(valor.abs());
            lancamento.setTipo(tipo);
            lancamento.setFitid(fitid);
            lancamento.setStatusConciliacao("PENDENTE");

            importados.add(extratoLancamentoRepository.save(lancamento));
        }

        return ImportExtratoResponseDTO.builder()
                .sucesso(true)
                .banco(banco)
                .conta(conta)
                .totalLancamentos(total)
                .novosLancamentos(importados.size())
                .duplicadosIgnorados(duplicados)
                .lancamentos(importados)
                .build();
    }

    private Map<String, String> extrairCampos(String bloco) {
        Map<String, String> campos = new HashMap<>();
        Matcher tagMatcher = TAG_PATTERN.matcher(bloco);
        while (tagMatcher.find()) {
            campos.put(tagMatcher.group(1).toUpperCase(), tagMatcher.group(2).trim());
        }
        return campos;
    }

    private LocalDate parseOfxDate(String raw) {
        if (raw == null) return LocalDate.now();
        String soDigitos = raw.length() >= 8 ? raw.substring(0, 8) : raw;
        return LocalDate.parse(soDigitos, DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    // ===================== IMPORTAÇÃO CSV =====================
    // Espera colunas: data,descricao,valor,tipo,fitid (fitid opcional)

    @Transactional
    public ImportExtratoResponseDTO importarCsv(MultipartFile file, String empresaId) throws IOException {
        String canonicalEmpresaId = tenantResolverService.resolveCanonicalUuid(empresaId);
        List<String> idsEmpresa = resolverIdentificadoresEmpresa(empresaId);

        List<ExtratoLancamento> importados = new ArrayList<>();
        int duplicados = 0;
        int total = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                total++;
                String fitid = record.isSet("fitid") ? record.get("fitid") : null;
                if (fitid == null || fitid.isBlank()) {
                    fitid = gerarFitidSintetico(canonicalEmpresaId, record);
                }

                if (existeLancamentoPorFitid(idsEmpresa, fitid)) {
                    duplicados++;
                    continue;
                }

                ExtratoLancamento lancamento = new ExtratoLancamento();
                lancamento.setId(UUID.randomUUID().toString());
                lancamento.setEmpresaId(canonicalEmpresaId);
                lancamento.setDataLancamento(LocalDate.parse(record.get("data")));
                lancamento.setDescricao(record.get("descricao"));
                lancamento.setValor(new BigDecimal(record.get("valor")).abs());
                lancamento.setTipo(record.get("tipo").toUpperCase());
                lancamento.setFitid(fitid);
                lancamento.setStatusConciliacao("PENDENTE");

                importados.add(extratoLancamentoRepository.save(lancamento));
            }
        }

        return ImportExtratoResponseDTO.builder()
                .sucesso(true)
                .banco(null)
                .conta(null)
                .totalLancamentos(total)
                .novosLancamentos(importados.size())
                .duplicadosIgnorados(duplicados)
                .lancamentos(importados)
                .build();
    }

    private String gerarFitidSintetico(String empresaId, CSVRecord record) {
        String base = empresaId + "|" + record.get("data") + "|" + record.get("valor") + "|" + record.get("descricao");
        return "SYN-" + Integer.toHexString(base.hashCode());
    }

    // ===================== CONCILIAÇÃO MANUAL =====================

    @Transactional
    public ConciliacaoBancaria conciliar(String extratoLancamentoId, String contaReceberId,
                                          String usuarioId, String observacao) {

        ExtratoLancamento extrato = extratoLancamentoRepository.findById(extratoLancamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento de extrato não encontrado"));

        ContaReceber conta = contaReceberRepository.findById(contaReceberId)
                .orElseThrow(() -> new IllegalArgumentException("Conta a receber não encontrada"));

        Empresa empExtrato = tenantResolverService.resolveEmpresa(extrato.getEmpresaId());
        Empresa empConta = tenantResolverService.resolveEmpresa(conta.getEmpresaId());

        if (!empExtrato.getId().equals(empConta.getId())) {
            throw new IllegalArgumentException("O extrato e a conta a receber pertencem a empresas distintas");
        }

        if ("CONCILIADO".equalsIgnoreCase(extrato.getStatusConciliacao())) {
            throw new IllegalStateException("Este lançamento já está conciliado");
        }
        if ("PAGO".equalsIgnoreCase(conta.getStatus())) {
            throw new IllegalStateException("Este título já está pago");
        }

        extrato.setStatusConciliacao("CONCILIADO");
        conta.setStatus("PAGO");
        conta.setDataPagamento(LocalDate.now());

        extratoLancamentoRepository.save(extrato);
        contaReceberRepository.save(conta);

        ConciliacaoBancaria conciliacao = new ConciliacaoBancaria();
        conciliacao.setId(UUID.randomUUID().toString());
        conciliacao.setExtratoLancamentoId(extratoLancamentoId);
        conciliacao.setContaReceberId(contaReceberId);
        conciliacao.setUsuarioId(usuarioId != null ? usuarioId : "SISTEMA");
        conciliacao.setDataConciliacao(LocalDateTime.now());
        conciliacao.setObservacao(observacao);

        return conciliacaoBancariaRepository.save(conciliacao);
    }

    // ===================== DESCONCILIAR =====================

    @Transactional
    public void desconciliar(String conciliacaoId) {
        ConciliacaoBancaria conciliacao = conciliacaoBancariaRepository.findById(conciliacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Conciliação não encontrada: " + conciliacaoId));

        if (conciliacao.getExtratoLancamentoId() != null) {
            extratoLancamentoRepository.findById(conciliacao.getExtratoLancamentoId()).ifPresent(extrato -> {
                extrato.setStatusConciliacao("PENDENTE");
                extratoLancamentoRepository.save(extrato);
            });
        }

        if (conciliacao.getContaReceberId() != null) {
            contaReceberRepository.findById(conciliacao.getContaReceberId()).ifPresent(conta -> {
                conta.setStatus("PENDENTE");
                conta.setDataPagamento(null);
                contaReceberRepository.save(conta);
            });
        }

        conciliacaoBancariaRepository.delete(conciliacao);
    }

    // ===================== CONSULTAS =====================

    public List<ExtratoLancamento> listarLancamentosPendentes(String empresaId) {
        List<String> ids = resolverIdentificadoresEmpresa(empresaId);
        Map<String, ExtratoLancamento> mapa = new LinkedHashMap<>();
        for (String id : ids) {
            List<ExtratoLancamento> lista = extratoLancamentoRepository.findByEmpresaIdAndStatusConciliacao(id, "PENDENTE");
            for (ExtratoLancamento l : lista) {
                mapa.putIfAbsent(l.getId(), l);
            }
        }
        return new ArrayList<>(mapa.values());
    }

    public List<ContaReceber> listarContasPendentes(String empresaId) {
        List<String> ids = resolverIdentificadoresEmpresa(empresaId);
        Map<String, ContaReceber> mapa = new LinkedHashMap<>();
        for (String id : ids) {
            List<ContaReceber> lista = contaReceberRepository.findByEmpresaIdAndStatus(id, "PENDENTE");
            for (ContaReceber c : lista) {
                mapa.putIfAbsent(c.getId(), c);
            }
        }
        return new ArrayList<>(mapa.values());
    }

    public List<ConciliacaoDetalhadaDTO> listarConciliacoes(String empresaId) {
        List<String> ids = resolverIdentificadoresEmpresa(empresaId);
        Map<String, ConciliacaoDetalhadaDTO> mapa = new LinkedHashMap<>();

        for (String id : ids) {
            List<ExtratoLancamento> extratos = extratoLancamentoRepository.findByEmpresaIdAndStatusConciliacao(id, "CONCILIADO");
            for (ExtratoLancamento ext : extratos) {
                Optional<ConciliacaoBancaria> opt = conciliacaoBancariaRepository.findByExtratoLancamentoId(ext.getId());
                if (opt.isPresent()) {
                    ConciliacaoBancaria conc = opt.get();
                    ContaReceber conta = null;
                    if (conc.getContaReceberId() != null) {
                        conta = contaReceberRepository.findById(conc.getContaReceberId()).orElse(null);
                    }
                    mapa.put(conc.getId(), new ConciliacaoDetalhadaDTO(
                            conc.getId(),
                            conc.getExtratoLancamentoId(),
                            conc.getContaReceberId(),
                            conc.getUsuarioId(),
                            conc.getDataConciliacao(),
                            conc.getObservacao(),
                            ext,
                            conta
                    ));
                }
            }
        }

        List<ConciliacaoDetalhadaDTO> resultado = new ArrayList<>(mapa.values());
        resultado.sort((a, b) -> {
            if (a.dataConciliacao() == null && b.dataConciliacao() == null) return 0;
            if (a.dataConciliacao() == null) return 1;
            if (b.dataConciliacao() == null) return -1;
            return b.dataConciliacao().compareTo(a.dataConciliacao());
        });
        return resultado;
    }
}
