package com.fatorr.backend.controller;

import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.domain.Usuario;
import com.fatorr.backend.dto.SimulacaoTributariaDTO;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.repository.UsuarioRepository;
import com.fatorr.backend.service.AuthService;
import com.fatorr.backend.service.CalculoFatorRService;
import com.fatorr.backend.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmpresaController {

    private final EmpresaRepository empresaRepository;
    private final CalculoFatorRService calculoFatorRService;
    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;
    private final JwtUtils jwtUtils;

    @GetMapping
    public List<Empresa> listarTodas(
            @RequestParam(required = false) Long usuarioId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Usuario usuario = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (jwtUtils.validateToken(token)) {
                String uuidId = jwtUtils.extractUuidId(token);
                if (uuidId != null) {
                    usuario = authService.buscarUsuarioPorUuid(uuidId).orElse(null);
                }
                if (usuario == null) {
                    String cpf = jwtUtils.extractCpf(token);
                    if (cpf != null) {
                        usuario = usuarioRepository.findByCpf(cpf).orElse(null);
                    }
                }
            }
        }

        if (usuario == null && usuarioId != null) {
            usuario = usuarioRepository.findById(usuarioId).orElse(null);
        }

        if (usuario != null) {
            return authService.listarEmpresasPermitidasEntidade(usuario);
        }

        // Sem credenciais autenticadas, não retorna empresas
        return Collections.emptyList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Empresa> buscarPorId(@PathVariable Long id) {
        return empresaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> criar(
            @RequestBody AuthService.CriarEmpresaRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Usuario usuario = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                if (jwtUtils.validateToken(token)) {
                    String uuidId = jwtUtils.extractUuidId(token);
                    if (uuidId != null) {
                        usuario = authService.buscarUsuarioPorUuid(uuidId).orElse(null);
                    }
                    if (usuario == null) {
                        String cpf = jwtUtils.extractCpf(token);
                        if (cpf != null) {
                            usuario = usuarioRepository.findByCpf(cpf).orElse(null);
                        }
                    }
                }
            }

            Empresa criada = authService.criarEmpresaComVinculo(request, usuario);
            return ResponseEntity.ok(criada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erro ao criar empresa: " + e.getMessage()));
        }
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
