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
        }
    }

    @GetMapping("/extrato/{empresaId}/pendentes")
    public List<ExtratoLancamento> listarLancamentosPendentes(@PathVariable String empresaId) {
        return bankingService.listarLancamentosPendentes(empresaId);
    }

    @GetMapping("/contas-receber/{empresaId}/pendentes")
    public List<ContaReceber> listarContasPendentes(@PathVariable String empresaId) {
        return bankingService.listarContasPendentes(empresaId);
    }

    @PostMapping("/conciliar")
    public ResponseEntity<?> conciliar(@RequestParam String usuarioId,
                                        @RequestBody ConciliarRequest request) {
        try {
            ConciliacaoBancaria resultado = bankingService.conciliar(
                    request.extratoLancamentoId(),
                    request.contaReceberId(),
                    usuarioId,
                    request.observacao()
            );
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    public record ConciliarRequest(String extratoLancamentoId, String contaReceberId, String observacao) {}
}