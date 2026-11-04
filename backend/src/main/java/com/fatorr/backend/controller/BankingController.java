package com.fatorr.backend.controller;

import com.fatorr.backend.domain.ConciliacaoBancaria;
import com.fatorr.backend.domain.ContaReceber;
import com.fatorr.backend.domain.ExtratoLancamento;
import com.fatorr.backend.dto.ImportExtratoResponseDTO;
import com.fatorr.backend.service.BankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/banking")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BankingController {

    private final BankingService bankingService;

    @PostMapping(value = "/importar/ofx", consumes = "multipart/form-data")
    public ResponseEntity<?> importarOfx(@RequestParam("file") MultipartFile file,
                                          @RequestParam("empresaId") String empresaId) {
        try {
            ImportExtratoResponseDTO resultado = bankingService.importarOfx(file, empresaId);
            return ResponseEntity.ok(resultado);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Falha ao ler arquivo OFX: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping(value = "/importar/csv", consumes = "multipart/form-data")
    public ResponseEntity<?> importarCsv(@RequestParam("file") MultipartFile file,
                                          @RequestParam("empresaId") String empresaId) {
        try {
            ImportExtratoResponseDTO resultado = bankingService.importarCsv(file, empresaId);
            return ResponseEntity.ok(resultado);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Falha ao ler arquivo CSV: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/extrato/{empresaId}/pendentes")
    public List<ExtratoLancamento> listarLancamentosPendentes(
            @PathVariable String empresaId,
            @RequestParam(required = false) String competencia) {
        return bankingService.listarLancamentosPendentes(empresaId, competencia);
    }

    @GetMapping("/contas-receber/{empresaId}/pendentes")
    public List<ContaReceber> listarContasPendentes(
            @PathVariable String empresaId,
            @RequestParam(required = false) String competencia) {
        return bankingService.listarContasPendentes(empresaId, competencia);
    }

    @DeleteMapping("/extrato/{id}")
    public ResponseEntity<?> excluirLancamentoExtrato(
            @PathVariable String id,
            @RequestParam String empresaId) {
        try {
            bankingService.excluirLancamentoExtrato(empresaId, id);
            return ResponseEntity.ok(Map.of("mensagem", "Lançamento excluído com sucesso", "id", id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/conciliar")
    public ResponseEntity<?> conciliar(
            @RequestParam(value = "usuarioId", required = false) String usuarioIdParam,
            @RequestBody ConciliarRequest request) {
        try {
            String usuarioFinal = (usuarioIdParam != null && !usuarioIdParam.isBlank())
                    ? usuarioIdParam
                    : (request.usuarioId() != null && !request.usuarioId().isBlank() ? request.usuarioId() : "SISTEMA");

            ConciliacaoBancaria resultado = bankingService.conciliar(
                    request.extratoLancamentoId(),
                    request.contaReceberId(),
                    usuarioFinal,
                    request.observacao()
            );
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/conciliacoes/{empresaId}")
    public ResponseEntity<List<BankingService.ConciliacaoDetalhadaDTO>> listarConciliacoes(
            @PathVariable String empresaId,
            @RequestParam(required = false) String competencia) {
        List<BankingService.ConciliacaoDetalhadaDTO> historico = bankingService.listarConciliacoes(empresaId, competencia);
        return ResponseEntity.ok(historico);
    }

    @DeleteMapping("/conciliacoes/{id}")
    public ResponseEntity<?> desconciliar(@PathVariable String id) {
        try {
            bankingService.desconciliar(id);
            return ResponseEntity.ok(Map.of("mensagem", "Desconciliação realizada com sucesso", "id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    public record ConciliarRequest(
            String extratoLancamentoId,
            String contaReceberId,
            String observacao,
            String usuarioId
    ) {
        public ConciliarRequest(String extratoLancamentoId, String contaReceberId, String observacao) {
            this(extratoLancamentoId, contaReceberId, observacao, null);
        }
    }
}
