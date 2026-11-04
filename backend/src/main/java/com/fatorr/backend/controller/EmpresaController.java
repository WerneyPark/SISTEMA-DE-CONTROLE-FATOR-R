package com.fatorr.backend.controller;

import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.dto.SimulacaoTributariaDTO;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.service.CalculoFatorRService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmpresaController {

    private final EmpresaRepository empresaRepository;
    private final CalculoFatorRService calculoFatorRService;

    @GetMapping
    public List<Empresa> listarTodas(@RequestParam(required = false) Long usuarioId) {
        if (usuarioId != null) {
            // Em um sistema real com Spring Security, pegaríamos o usuário do Contexto (Token JWT).
            // Aqui filtramos na unha usando o repositório.
            return empresaRepository.findByUsuariosId(usuarioId);
        }
        return empresaRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Empresa> buscarPorId(@PathVariable Long id) {
        return empresaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Empresa criar(@RequestBody Empresa empresa) {
        return empresaRepository.save(empresa);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Empresa> atualizar(@PathVariable Long id, @RequestBody Empresa empresaAtualizada) {
        return empresaRepository.findById(id)
                .map(empresa -> {
                    empresa.setRazaoSocial(empresaAtualizada.getRazaoSocial());
                    empresa.setCnpj(empresaAtualizada.getCnpj());
                    return ResponseEntity.ok(empresaRepository.save(empresa));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (empresaRepository.existsById(id)) {
            empresaRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Endpoint principal de simulação tributária.
     * Calcula o Fator R, emite alertas de proximidade e compara regimes.
     * @param id ID da empresa
     * @param ano Ano de referência (ex: 2026)
     * @param mes Mês de referência (ex: 5)
     */
    @GetMapping("/{id}/simulacao-tributaria")
    public ResponseEntity<SimulacaoTributariaDTO> simularTributacao(
            @PathVariable Long id,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes) {

        YearMonth competencia;
        if (ano != null && mes != null) {
            competencia = YearMonth.of(ano, mes);
        } else {
            competencia = YearMonth.now();
        }

        SimulacaoTributariaDTO resultado = calculoFatorRService.simularTributacao(id, competencia);
        return ResponseEntity.ok(resultado);
    }
    @GetMapping("/{id}/simulacao-futura")
    public ResponseEntity<List<com.fatorr.backend.dto.ProjecaoCenarioDTO>> simularProximosMeses(
            @PathVariable Long id,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes) {

        YearMonth competencia;
        if (ano != null && mes != null) {
            competencia = YearMonth.of(ano, mes);
        } else {
            competencia = YearMonth.now();
        }

        List<com.fatorr.backend.dto.ProjecaoCenarioDTO> resultado = calculoFatorRService.simularProximosMeses(id, competencia);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/{id}/competencias/{competencia}/encerrar")
    public ResponseEntity<?> encerrarCompetencia(
            @PathVariable Long id,
            @PathVariable String competencia) {
        try {
            YearMonth ym = YearMonth.parse(competencia);
            calculoFatorRService.encerrarCompetencia(id, ym);
            return ResponseEntity.ok(java.util.Map.of("mensagem", "Competência " + competencia + " encerrada com sucesso."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/{id}/competencias/{competencia}/reabrir")
    public ResponseEntity<?> reabrirCompetencia(
            @PathVariable Long id,
            @PathVariable String competencia) {
        try {
            YearMonth ym = YearMonth.parse(competencia);
            calculoFatorRService.reabrirCompetencia(id, ym);
            return ResponseEntity.ok(java.util.Map.of("mensagem", "Competência " + competencia + " reaberta com sucesso."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/{id}/dados-simulacao")
    public ResponseEntity<?> salvarDadosSimulacao(
            @PathVariable Long id,
            @RequestBody DadosSimulacaoRequest request) {
        try {
            if (request.competencia() == null || request.competencia().isBlank()) {
                throw new IllegalArgumentException("A competência é obrigatória.");
            }
            YearMonth ym = YearMonth.parse(request.competencia());
            calculoFatorRService.salvarDadosSimulacao(id, ym, request.valorFaturamento(), request.valorFolha());
            return ResponseEntity.ok(java.util.Map.of("mensagem", "Dados de simulação salvos com sucesso."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", e.getMessage()));
        }
    }

    public record DadosSimulacaoRequest(
            String competencia,
            java.math.BigDecimal valorFaturamento,
            java.math.BigDecimal valorFolha
    ) {}
}
