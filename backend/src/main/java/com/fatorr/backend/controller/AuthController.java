package com.fatorr.backend.controller;

import com.fatorr.backend.dto.AuthResponseDTO;
import com.fatorr.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthService.LoginRequest loginRequest) {
        try {
            AuthResponseDTO response = authService.login(loginRequest.getIdentifier(), loginRequest.getSenha());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erro ao processar login"));
        }
    }

    @PostMapping("/register/contador")
    public ResponseEntity<?> registerContador(@RequestBody AuthService.RegisterContadorRequest request) {
        try {
            AuthResponseDTO response = authService.registerContador(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erro ao cadastrar contador"));
        }
    }

    @PostMapping("/register/empresario")
    public ResponseEntity<?> registerEmpresario(@RequestBody AuthService.RegisterEmpresarioRequest request) {
        try {
            AuthResponseDTO response = authService.registerEmpresario(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erro ao cadastrar empresário"));
        }
    }

    @PostMapping("/register/usuario")
    public ResponseEntity<?> registerUsuario(@RequestBody AuthService.RegisterUsuarioRequest request) {
        try {
            AuthResponseDTO response = authService.registerUsuario(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erro ao cadastrar colaborador"));
        }
    }

    @GetMapping("/contadores")
    public ResponseEntity<List<AuthService.ContadorResumoDTO>> listarContadores() {
        return ResponseEntity.ok(authService.listarContadoresDisponiveis());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody AuthService.ForgotPasswordRequest request) {
        try {
            Map<String, String> response = authService.forgotPassword(request.getIdentifier());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erro ao processar solicitação"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody AuthService.ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getToken(), request.getNovaSenha());
            return ResponseEntity.ok(Collections.singletonMap("message", "Senha redefinida com sucesso."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erro ao redefinir senha"));
        }
    }
}
