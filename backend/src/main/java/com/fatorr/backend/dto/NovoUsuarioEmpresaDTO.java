package com.fatorr.backend.dto;

import lombok.Data;

@Data
public class NovoUsuarioEmpresaDTO {
    private String cpf;
    private String senha;
    private Long empresaId;
}
