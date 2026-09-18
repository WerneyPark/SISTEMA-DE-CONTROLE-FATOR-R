package com.fatorr.backend.controller;

import com.fatorr.backend.domain.Usuario;
import com.fatorr.backend.dto.AuthResponseDTO;
import com.fatorr.backend.dto.LoginDTO;
import com.fatorr.backend.repository.UsuarioRepository;
import com.fatorr.backend.util.CpfUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginDTO loginDto) {
        String cpfLimpo = CpfUtils.unmask(loginDto.getCpf());
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCpf(cpfLimpo);
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            // Comparação de senha simples (sem encriptação para o MVP, em prod usar BCrypt)
            if (usuario.getSenha().equals(loginDto.getSenha())) {
                AuthResponseDTO response = AuthResponseDTO.builder()
                        .id(usuario.getId())
                        .cpf(usuario.getCpf())
                        .tipo(usuario.getTipo())
                        .token("dummy-token-" + usuario.getId())
                        .build();
                return ResponseEntity.ok(response);
            }
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
