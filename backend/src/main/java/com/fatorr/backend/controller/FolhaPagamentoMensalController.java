package com.fatorr.backend.controller;

import com.fatorr.backend.domain.FolhaPagamentoMensal;
import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.dto.FolhaPagamentoMensalDTO;
import com.fatorr.backend.repository.FolhaPagamentoMensalRepository;
import com.fatorr.backend.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/folhas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FolhaPagamentoMensalController {

    private final FolhaPagamentoMensalRepository folhaRepository;
    private final EmpresaRepository empresaRepository;
    private final com.fatorr.backend.service.CalculoFatorRService calculoFatorRService;

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<FolhaPagamentoMensal> criar(@RequestBody FolhaPagamentoMensalDTO dto) {
        if (calculoFatorRService.isCompetenciaEncerrada(dto.getEmpresaId(), dto.getCompetencia())) {
            throw new IllegalStateException("A competência " + dto.getCompetencia() + " já está encerrada. Reabra o período no Simulador do Fator R.");
        }
        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));

        List<FolhaPagamentoMensal> existentes = folhaRepository.findByEmpresaIdAndCompetencia(empresa.getId(), dto.getCompetencia());
        if (!existentes.isEmpty()) {
            folhaRepository.deleteAll(existentes);
        }

        FolhaPagamentoMensal folha = new FolhaPagamentoMensal();
        folha.setEmpresa(empresa);
        folha.setCompetencia(dto.getCompetencia());
        folha.setValor(dto.getValor());

        return ResponseEntity.ok(folhaRepository.save(folha));
    }

    @GetMapping("/empresa/{empresaId}")
    public List<FolhaPagamentoMensal> listarPorEmpresa(@PathVariable Long empresaId) {
        YearMonth dataFim = YearMonth.now();
        YearMonth dataInicio = dataFim.minusMonths(12);
        return folhaRepository.findUltimos12Meses(empresaId, dataInicio, dataFim);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        FolhaPagamentoMensal folha = folhaRepository.findById(id).orElse(null);
        if (folha != null) {
            if (calculoFatorRService.isCompetenciaEncerrada(folha.getEmpresa().getId(), folha.getCompetencia())) {
                throw new IllegalStateException("A competência " + folha.getCompetencia() + " já está encerrada. Reabra o período no Simulador do Fator R.");
            }
            folhaRepository.delete(folha);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
