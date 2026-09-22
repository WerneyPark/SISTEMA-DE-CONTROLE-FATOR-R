package com.fatorr.backend.service;

import com.fatorr.backend.domain.ConciliacaoBancaria;
import com.fatorr.backend.domain.ContaReceber;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BankingService {

    private final ExtratoLancamentoRepository extratoLancamentoRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final ConciliacaoBancariaRepository conciliacaoBancariaRepository;

    // ===================== IMPORTAÇÃO OFX =====================

    private static final Pattern STMTTRN_PATTERN =
            Pattern.compile("<STMTTRN>(.*?)</STMTTRN>", Pattern.DOTALL);
    private static final Pattern BANKACCTFROM_PATTERN =
            Pattern.compile("<BANKACCTFROM>(.*?)</BANKACCTFROM>", Pattern.DOTALL);
    private static final Pattern TAG_PATTERN =
            Pattern.compile("<(\\w+)>([^<\\r\\n]*)");

    @Transactional
    public ImportExtratoResponseDTO importarOfx(MultipartFile file, String empresaId) throws IOException {
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

            if (extratoLancamentoRepository.findByEmpresaIdAndFitid(empresaId, fitid).isPresent()) {
                duplicados++;
                continue;
            }

            BigDecimal valor = new BigDecimal(campos.getOrDefault("TRNAMT", "0"));
            String tipo = valor.compareTo(BigDecimal.ZERO) < 0 ? "DEBITO" : "CREDITO";
            LocalDate data = parseOfxDate(campos.get("DTPOSTED"));
            String descricao = campos.getOrDefault("MEMO", campos.getOrDefault("NAME", "Lançamento sem descrição"));

            ExtratoLancamento lancamento = new ExtratoLancamento();
            lancamento.setId(UUID.randomUUID().toString());
            lancamento.setEmpresaId(empresaId);
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
                    fitid = gerarFitidSintetico(empresaId, record);
                }

                if (extratoLancamentoRepository.findByEmpresaIdAndFitid(empresaId, fitid).isPresent()) {
                    duplicados++;
                    continue;
                }

                ExtratoLancamento lancamento = new ExtratoLancamento();
                lancamento.setId(UUID.randomUUID().toString());
                lancamento.setEmpresaId(empresaId);
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

        if ("CONCILIADO".equals(extrato.getStatusConciliacao())) {
            throw new IllegalStateException("Este lançamento já está conciliado");
        }
        if ("PAGO".equals(conta.getStatus())) {
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
        conciliacao.setUsuarioId(usuarioId);
        conciliacao.setObservacao(observacao);

        return conciliacaoBancariaRepository.save(conciliacao);
    }

    // ===================== CONSULTAS =====================

    public List<ExtratoLancamento> listarLancamentosPendentes(String empresaId) {
        return extratoLancamentoRepository.findByEmpresaIdAndStatusConciliacao(empresaId, "PENDENTE");
    }

    public List<ContaReceber> listarContasPendentes(String empresaId) {
        return contaReceberRepository.findByEmpresaIdAndStatus(empresaId, "PENDENTE");
    }
}