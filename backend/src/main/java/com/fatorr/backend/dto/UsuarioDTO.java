package com.fatorr.backend.dto;

import com.fatorr.backend.domain.TipoUsuario;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
    private Long id;
    private String cpf;
    private TipoUsuario tipo;
    private List<EmpresaResumoDTO> empresas;
}
