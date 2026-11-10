package com.fatorr.backend.controller;

import com.fatorr.backend.domain.FolhaDetalhada;
import com.fatorr.backend.dto.ImportFolhaResponseDTO;
import com.fatorr.backend.dto.ProLaboreIdealDTO;
import com.fatorr.backend.service.PayrollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/{empresaId}")
    public ResponseEntity<List<FolhaDetalhada>> listarFolhas(
            @PathVariable String empresaId,
            @RequestParam(required = false) String competencia) {
        try {
            List<FolhaDetalhada> lista = payrollService.listFolhas(empresaId, competencia);
            return ResponseEntity.ok(lista);
        } catch (IllegalArgumentException e) {
            log.error("Erro ao listar folhas para empresa {}: {}", empresaId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{empresaId}/{competencia}")
    public ResponseEntity<FolhaDetalhada> buscarPorCompetencia(
            @PathVariable String empresaId,
            @PathVariable String competencia) {
        return payrollService.getFolhaByCompetencia(empresaId, competencia)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> salvarFolha(@RequestBody FolhaDetalhada folha) {
        try {
            FolhaDetalhada salva = payrollService.saveFolha(folha);
            return ResponseEntity.ok(salva);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            log.error("Erro inesperado ao salvar folha: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro interno ao processar folha: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/importar/esocial", consumes = "multipart/form-data")
    public ResponseEntity<?> importarEsocial(
            @RequestParam("file") MultipartFile file,
            @RequestParam("empresaId") String empresaId) {
        try {
            ImportFolhaResponseDTO resultado = payrollService.importarEsocialXml(file, empresaId);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            log.error("Falha ao importar eSocial XML: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("erro", "Falha ao processar arquivo eSocial XML: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/importar/csv", consumes = "multipart/form-data")
    public ResponseEntity<?> importarCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("empresaId") String empresaId) {
        try {
            ImportFolhaResponseDTO resultado = payrollService.importarCsvOuExcel(file, empresaId);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            log.error("Falha ao importar planilha de folha: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("erro", "Falha ao processar arquivo de folha: " + e.getMessage()));
        }
    }

    @GetMapping("/{empresaId}/pro-labore-ideal")
    public ResponseEntity<?> calcularProLaboreIdeal(
            @PathVariable String empresaId,
            @RequestParam String competencia,
            @RequestParam(required = false) BigDecimal faturamentoEstimado) {
        try {
            ProLaboreIdealDTO resultado = payrollService.calcularProLaboreIdeal(empresaId, competencia, faturamentoEstimado);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            log.error("Erro ao calcular pró-labore ideal: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("erro", "Erro no cálculo de pró-labore ideal: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirFolha(@PathVariable String id) {
        payrollService.deleteFolha(id);
        return ResponseEntity.noContent().build();
    }
}
