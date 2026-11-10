package com.fatorr.backend.controller;

import com.fatorr.backend.domain.*;
import com.fatorr.backend.dto.EmpresaResumoDTO;
import com.fatorr.backend.dto.NovoUsuarioEmpresaDTO;
import com.fatorr.backend.dto.UsuarioDTO;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.repository.EmpresaUsuarioRepository;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final EmpresaUsuarioRepository empresaUsuarioRepository;
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

    private Usuario extrairUsuarioDoHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (jwtUtils.validateToken(token)) {
                String uuidId = jwtUtils.extractUuidId(token);
                if (uuidId != null) {
                    Optional<Usuario> opt = authService.buscarUsuarioPorUuid(uuidId);
                    if (opt.isPresent()) return opt.get();
                }
                String cpf = jwtUtils.extractCpf(token);
                if (cpf != null) {
                    return usuarioRepository.findByCpf(cpf).orElse(null);
                }
            }
        }
        return null;
    }

    /**
     * Lista os convites pendentes de empresa criados por contadores para o empresário autenticado.
     */
    @GetMapping("/meus-convites")
    public ResponseEntity<?> listarMeusConvites(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Usuario usuario = extrairUsuarioDoHeader(authHeader);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Não autorizado"));
            }
            List<AuthService.ConviteEmpresaDTO> convites = authService.listarConvitesEmpresaParaEmpresario(usuario);
            return ResponseEntity.ok(convites);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erro ao listar convites: " + e.getMessage()));
        }
    }

    /**
     * Responde ao convite de empresa (aceita ou recusa a empresa e o contador).
     */
    @PostMapping("/meus-convites/{id}/responder")
    public ResponseEntity<?> responderConvite(
            @PathVariable String id,
            @RequestBody AuthService.RespostaConviteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Usuario usuario = extrairUsuarioDoHeader(authHeader);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Não autorizado"));
            }
            authService.responderConviteEmpresa(id, request.isAceitar(), usuario);
            String msg = request.isAceitar() ? "Empresa e contador aceitos com sucesso!" : "Convite recusado com sucesso.";
            return ResponseEntity.ok(Collections.singletonMap("message", msg));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erro ao responder convite: " + e.getMessage()));
        }
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
            usuario.setUuidId(UUID.randomUUID().toString());
            usuario.setNome("Colaborador (" + CpfUtils.mask(cleanCpf) + ")");
            usuario.setCpf(cleanCpf);
            usuario.setSenha(dto.getSenha());
            usuario.setTipo(TipoUsuario.USUARIO_EMPRESA);
            usuario.setTipoPerfil(TipoPerfil.USUARIO);
            usuario.setStatus(StatusUsuario.ATIVO);
        }

        usuario.getEmpresas().add(empresa);
        usuario = usuarioRepository.save(usuario);

        // Atualizar ou criar vínculo APROVADO em empresa_usuarios
        Optional<EmpresaUsuario> vincOpt = empresaUsuarioRepository.findByEmpresaIdAndUsuarioId(empresa.getUuidId(), usuario.getUuidId());
        if (vincOpt.isPresent()) {
            EmpresaUsuario vinc = vincOpt.get();
            vinc.setStatusVinculo(StatusVinculo.APROVADO);
            vinc.setAprovadoEm(LocalDateTime.now());
            empresaUsuarioRepository.save(vinc);
        } else {
            EmpresaUsuario novoVinc = new EmpresaUsuario(
                    UUID.randomUUID().toString(),
                    empresa.getUuidId(),
                    usuario.getUuidId(),
                    "Colaborador",
                    StatusVinculo.APROVADO,
                    usuario.getUuidId(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            empresaUsuarioRepository.save(novoVinc);
        }

        return ResponseEntity.ok("Usuário vinculado com sucesso.");
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarTodosUsuarios(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Usuario usuarioLogado = extrairUsuarioDoHeader(authHeader);
        if (usuarioLogado == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Empresa> minhasEmpresas = authService.listarEmpresasPermitidasEntidade(usuarioLogado);
        Set<Long> empresaIds = minhasEmpresas.stream().map(Empresa::getId).collect(Collectors.toSet());
        Set<String> empresaUuids = minhasEmpresas.stream()
                .map(Empresa::getUuidId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Usuario> todos = usuarioRepository.findAllWithEmpresas();
        List<UsuarioDTO> dtos = todos.stream()
                .filter(u -> {
                    if (u.getId().equals(usuarioLogado.getId())) return true;
                    if (usuarioLogado.getOrganizacaoContabilId() != null &&
                            usuarioLogado.getOrganizacaoContabilId().equals(u.getOrganizacaoContabilId())) {
                        return true;
                    }
                    return u.getEmpresas().stream().anyMatch(e -> empresaIds.contains(e.getId()) ||
                            (e.getUuidId() != null && empresaUuids.contains(e.getUuidId())));
                })
                .map(u -> {
                    List<EmpresaResumoDTO> empresasDto = u.getEmpresas().stream()
                            .filter(e -> empresaIds.contains(e.getId()) || (e.getUuidId() != null && empresaUuids.contains(e.getUuidId())))
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

            if (empresa.getUuidId() != null && usuario.getUuidId() != null) {
                empresaUsuarioRepository.findByEmpresaIdAndUsuarioId(empresa.getUuidId(), usuario.getUuidId())
                        .ifPresent(empresaUsuarioRepository::delete);
            }

            return ResponseEntity.ok("Usuário desvinculado com sucesso.");
        }
        return ResponseEntity.notFound().build();
    }
}
