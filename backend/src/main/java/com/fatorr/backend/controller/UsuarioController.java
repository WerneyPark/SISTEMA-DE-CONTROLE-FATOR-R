package com.fatorr.backend.controller;

import com.fatorr.backend.domain.*;
import com.fatorr.backend.dto.EmpresaResumoDTO;
import com.fatorr.backend.dto.NovoUsuarioEmpresaDTO;
import com.fatorr.backend.dto.UsuarioDTO;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.repository.UsuarioRepository;
import com.fatorr.backend.service.AuthService;
import com.fatorr.backend.util.CpfUtils;
import com.fatorr.backend.util.JwtUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final AuthService authService;
    private final JwtUtils jwtUtils;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConvidarCpfDTO {
        private String cpf;
        private String funcao;
        private String empresaId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlterarStatusVinculoDTO {
        private String status;
        private String aprovadoPor;
    }

    /**
     * Pré-cadastra usuário colaborador por CPF com status PENDENTE_CADASTRO.
     */
    @PostMapping("/convidar-cpf")
    public ResponseEntity<?> convidarPorCpf(@RequestBody ConvidarCpfDTO dto) {
        try {
            EmpresaUsuario vinculo = authService.convidarPorCpf(dto.getCpf(), dto.getFuncao(), dto.getEmpresaId());
            return ResponseEntity.status(HttpStatus.CREATED).body(vinculo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erro ao pré-cadastrar usuário: " + e.getMessage()));
        }
    }

    /**
     * Lista vínculos com solicitação pendente para aprovação.
     */
    @GetMapping("/vinculos-pendentes")
    public ResponseEntity<?> listarVinculosPendentes(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String usuarioUuid = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                if (jwtUtils.validateToken(token)) {
                    usuarioUuid = jwtUtils.extractUuidId(token);
                }
            }
            List<AuthService.VinculoPendenteDTO> pendentes = authService.listarVinculosPendentes(usuarioUuid);
            return ResponseEntity.ok(pendentes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erro ao listar vínculos pendentes"));
        }
    }

    /**
     * Aprova ou rejeita solicitação de vínculo de usuário com empresa.
     */
    @PostMapping("/vinculos/{id}/status")
    public ResponseEntity<?> atualizarStatusVinculo(
            @PathVariable String id,
            @RequestBody(required = false) AlterarStatusVinculoDTO dto,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String statusFinal = (dto != null && dto.getStatus() != null && !dto.getStatus().isBlank())
                    ? dto.getStatus() : status;

            if (statusFinal == null || statusFinal.isBlank()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Status obrigatório (APROVADO ou REJEITADO)"));
            }

            String aprovadorUuid = (dto != null) ? dto.getAprovadoPor() : null;
            if (aprovadorUuid == null && authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                if (jwtUtils.validateToken(token)) {
                    aprovadorUuid = jwtUtils.extractUuidId(token);
                }
            }

            StatusVinculo statusEnum = StatusVinculo.valueOf(statusFinal.trim().toUpperCase());
            EmpresaUsuario vinculoAtualizado = authService.atualizarStatusVinculo(id, statusEnum, aprovadorUuid);
            return ResponseEntity.ok(vinculoAtualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erro ao atualizar status do vínculo: " + e.getMessage()));
        }
    }

    @PostMapping("/vincular-empresa")
    @Transactional
    public ResponseEntity<?> criarEVincularUsuario(@RequestBody NovoUsuarioEmpresaDTO dto) {
        Optional<Empresa> empresaOpt = empresaRepository.findById(dto.getEmpresaId());

        if (empresaOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Empresa não encontrada.");
        }

        Empresa empresa = empresaOpt.get();
        Usuario usuario;

        String cleanCpf = CpfUtils.unmask(dto.getCpf());
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCpf(cleanCpf);
        if (usuarioOpt.isPresent()) {
            usuario = usuarioOpt.get();
            if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
                usuario.setSenha(dto.getSenha());
            }
        } else {
            usuario = new Usuario();
            usuario.setCpf(cleanCpf);
            usuario.setSenha(dto.getSenha());
            usuario.setTipo(TipoUsuario.USUARIO_EMPRESA);
            usuario.setTipoPerfil(TipoPerfil.USUARIO);
            usuario.setStatus(StatusUsuario.ATIVO);
        }

        usuario.getEmpresas().add(empresa);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok("Usuário vinculado com sucesso.");
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarTodosUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAllWithEmpresas();
        List<UsuarioDTO> dtos = usuarios.stream().map(u -> {
            List<EmpresaResumoDTO> empresasDto = u.getEmpresas().stream()
                    .map(e -> new EmpresaResumoDTO(e.getId(), e.getRazaoSocial(), e.getCnpj()))
                    .collect(Collectors.toList());

            return UsuarioDTO.builder()
                    .id(u.getId())
                    .uuidId(u.getUuidId())
                    .nome(u.getNome())
                    .email(u.getEmail())
                    .cpf(u.getCpf())
                    .tipoPerfil(u.getTipoPerfil())
                    .tipo(u.getTipo())
                    .status(u.getStatus())
                    .organizacaoContabilId(u.getOrganizacaoContabilId())
                    .empresas(empresasDto)
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<Usuario>> listarUsuariosDaEmpresa(@PathVariable Long empresaId) {
        List<Usuario> usuarios = usuarioRepository.findByEmpresasId(empresaId);
        return ResponseEntity.ok(usuarios);
    }

    @DeleteMapping("/{usuarioId}/vincular-empresa/{empresaId}")
    @Transactional
    public ResponseEntity<?> desvincularUsuario(@PathVariable Long usuarioId, @PathVariable Long empresaId) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(usuarioId);
        Optional<Empresa> empresaOpt = empresaRepository.findById(empresaId);

        if (usuarioOpt.isPresent() && empresaOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            Empresa empresa = empresaOpt.get();

            usuario.getEmpresas().remove(empresa);
            usuarioRepository.save(usuario);
            return ResponseEntity.ok("Usuário desvinculado com sucesso.");
        }
        return ResponseEntity.notFound().build();
    }
}
