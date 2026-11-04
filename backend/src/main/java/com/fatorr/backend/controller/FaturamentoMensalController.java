package com.fatorr.backend.controller;

import com.fatorr.backend.domain.FaturamentoMensal;
import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.dto.FaturamentoMensalDTO;
import com.fatorr.backend.repository.FaturamentoMensalRepository;
import com.fatorr.backend.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/faturamentos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FaturamentoMensalController {

    private final FaturamentoMensalRepository faturamentoRepository;
    private final EmpresaRepository empresaRepository;
    private final com.fatorr.backend.service.CalculoFatorRService calculoFatorRService;

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<FaturamentoMensal> criar(@RequestBody FaturamentoMensalDTO dto) {
        if (calculoFatorRService.isCompetenciaEncerrada(dto.getEmpresaId(), dto.getCompetencia())) {
            throw new IllegalStateException("A competência " + dto.getCompetencia() + " já está encerrada. Reabra o período no Simulador do Fator R.");
        }
        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));

        List<FaturamentoMensal> existentes = faturamentoRepository.findByEmpresaIdAndCompetencia(empresa.getId(), dto.getCompetencia());
        if (!existentes.isEmpty()) {
            faturamentoRepository.deleteAll(existentes);
        }

        FaturamentoMensal faturamento = new FaturamentoMensal();
        faturamento.setEmpresa(empresa);
        faturamento.setCompetencia(dto.getCompetencia());
        faturamento.setValor(dto.getValor());

        return ResponseEntity.ok(faturamentoRepository.save(faturamento));
    }

    @GetMapping("/empresa/{empresaId}")
    public List<FaturamentoMensal> listarPorEmpresa(@PathVariable Long empresaId) {
        YearMonth dataFim = YearMonth.now();
        YearMonth dataInicio = dataFim.minusMonths(12);
        return faturamentoRepository.findUltimos12Meses(empresaId, dataInicio, dataFim);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        FaturamentoMensal faturamento = faturamentoRepository.findById(id).orElse(null);
        if (faturamento != null) {
            if (calculoFatorRService.isCompetenciaEncerrada(faturamento.getEmpresa().getId(), faturamento.getCompetencia())) {
                throw new IllegalStateException("A competência " + faturamento.getCompetencia() + " já está encerrada. Reabra o período no Simulador do Fator R.");
            }
            faturamentoRepository.delete(faturamento);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
