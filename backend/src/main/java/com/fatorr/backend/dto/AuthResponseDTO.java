package com.fatorr.backend.dto;

import com.fatorr.backend.domain.TipoUsuario;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponseDTO {
    private Long id;
    private String uuidId;
    private String cpf;
    private TipoUsuario tipo;
    private String token; // Futuro, para quando colocarmos JWT. Por enquanto, a interface usa o tipo de usuário.
}
