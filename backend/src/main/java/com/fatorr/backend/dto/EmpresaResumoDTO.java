package com.fatorr.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaResumoDTO {
    private Long id;
    private String razaoSocial;
    private String cnpj;
}
