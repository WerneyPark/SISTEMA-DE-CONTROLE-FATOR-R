package com.fatorr.backend.controller;

import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.domain.TipoUsuario;
import com.fatorr.backend.domain.Usuario;
import com.fatorr.backend.dto.NovoUsuarioEmpresaDTO;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    @PostMapping("/vincular-empresa")
    @Transactional
    public ResponseEntity<?> criarEVincularUsuario(@RequestBody NovoUsuarioEmpresaDTO dto) {
        Optional<Empresa> empresaOpt = empresaRepository.findById(dto.getEmpresaId());
        
        if (empresaOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Empresa não encontrada.");
        }

        Empresa empresa = empresaOpt.get();
        Usuario usuario;

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCpf(dto.getCpf());
        if (usuarioOpt.isPresent()) {
            usuario = usuarioOpt.get();
            // Se o usuário já existe, atualizamos a senha (opcional, dependendo da regra de negócio)
            // e vinculamos à nova empresa
            if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
                usuario.setSenha(dto.getSenha());
            }
        } else {
            usuario = new Usuario();
            usuario.setCpf(dto.getCpf());
            usuario.setSenha(dto.getSenha());
            usuario.setTipo(TipoUsuario.USUARIO_EMPRESA);
        }

        usuario.getEmpresas().add(empresa);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok("Usuário vinculado com sucesso.");
    }

    @GetMapping
    public ResponseEntity<List<com.fatorr.backend.dto.UsuarioDTO>> listarTodosUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAllWithEmpresas();
        List<com.fatorr.backend.dto.UsuarioDTO> dtos = usuarios.stream().map(u -> {
            List<com.fatorr.backend.dto.EmpresaResumoDTO> empresasDto = u.getEmpresas().stream()
                .map(e -> new com.fatorr.backend.dto.EmpresaResumoDTO(e.getId(), e.getRazaoSocial(), e.getCnpj()))
                .collect(java.util.stream.Collectors.toList());
            return new com.fatorr.backend.dto.UsuarioDTO(u.getId(), u.getCpf(), u.getTipo(), empresasDto);
        }).collect(java.util.stream.Collectors.toList());
        
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
