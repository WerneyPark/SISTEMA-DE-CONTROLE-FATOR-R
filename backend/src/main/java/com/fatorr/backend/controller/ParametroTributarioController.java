package com.fatorr.backend.controller;

import com.fatorr.backend.domain.ParametroTributario;
import com.fatorr.backend.repository.ParametroTributarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.domain.Empresa;
import java.util.Optional;

@RestController
@RequestMapping("/api/parametros")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ParametroTributarioController {

    private final ParametroTributarioRepository repository;
    private final EmpresaRepository empresaRepository;

    @GetMapping("/padrao")
    public ResponseEntity<ParametroTributario> getPadrao() {
        return repository.findByAtivoTrueAndEmpresaIsNull()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/padrao")
    public ResponseEntity<ParametroTributario> salvarPadrao(@RequestBody ParametroTributario novoParametro) {
        repository.findByAtivoTrueAndEmpresaIsNull().ifPresent(atual -> {
            atual.setAtivo(false);
            repository.save(atual);
        });
        
        novoParametro.setAtivo(true);
        novoParametro.setEmpresa(null); // Assegura que é o padrão
        if (novoParametro.getFaixas() != null) {
            novoParametro.getFaixas().forEach(f -> f.setParametro(novoParametro));
        }
        return ResponseEntity.ok(repository.save(novoParametro));
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<ParametroTributario> getPorEmpresa(@PathVariable Long empresaId) {
        return repository.findByAtivoTrueAndEmpresaId(empresaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/empresa/{empresaId}")
    public ResponseEntity<?> salvarPorEmpresa(@PathVariable Long empresaId, @RequestBody ParametroTributario novoParametro) {
        Optional<Empresa> empresaOpt = empresaRepository.findById(empresaId);
        if (empresaOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Empresa não encontrada.");
        }

        repository.findByAtivoTrueAndEmpresaId(empresaId).ifPresent(atual -> {
            atual.setAtivo(false);
            repository.save(atual);
        });

        novoParametro.setAtivo(true);
        novoParametro.setEmpresa(empresaOpt.get());
        if (novoParametro.getFaixas() != null) {
            novoParametro.getFaixas().forEach(f -> f.setParametro(novoParametro));
        }
        return ResponseEntity.ok(repository.save(novoParametro));
    }

    // Mantendo compatibilidade caso algo use o /atual ainda sem contexto de empresa
    @GetMapping("/atual")
    public ResponseEntity<ParametroTributario> getAtual() {
        return getPadrao();
    }
}
