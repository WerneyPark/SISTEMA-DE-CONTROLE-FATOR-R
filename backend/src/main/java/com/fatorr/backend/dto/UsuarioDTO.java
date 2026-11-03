package com.fatorr.backend.dto;

import com.fatorr.backend.domain.StatusUsuario;
import com.fatorr.backend.domain.TipoPerfil;
import com.fatorr.backend.domain.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
    private Long id;
    private String uuidId;
    private String nome;
    private String email;
    private String cpf;
    private TipoPerfil tipoPerfil;
    private TipoUsuario tipo;
    private StatusUsuario status;
    private String organizacaoContabilId;

    @Builder.Default
    private List<EmpresaResumoDTO> empresas = new ArrayList<>();

    public UsuarioDTO(Long id, String cpf, TipoUsuario tipo, List<EmpresaResumoDTO> empresas) {
        this.id = id;
        this.cpf = cpf;
        this.tipo = tipo;
        this.empresas = empresas != null ? empresas : new ArrayList<>();
    }
}
