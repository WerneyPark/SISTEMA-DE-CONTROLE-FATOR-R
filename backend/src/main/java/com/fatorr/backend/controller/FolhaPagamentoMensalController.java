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

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<FolhaPagamentoMensal> criar(@RequestBody FolhaPagamentoMensalDTO dto) {
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
        if (folhaRepository.existsById(id)) {
            folhaRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
