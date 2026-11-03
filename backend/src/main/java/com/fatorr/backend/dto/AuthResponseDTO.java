package com.fatorr.backend.dto;

import com.fatorr.backend.domain.StatusUsuario;
import com.fatorr.backend.domain.StatusVinculo;
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
public class AuthResponseDTO {
    private Long id;
    private String uuidId;
    private String nome;
    private String email;
    private String cpf;
    private TipoPerfil tipoPerfil;
    private TipoUsuario tipo;
    private StatusUsuario status;
    private String organizacaoContabilId;
    private String token;

    @Builder.Default
    private List<EmpresaVinculadaDTO> empresas = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmpresaVinculadaDTO {
        private Long id;
        private String uuidId;
        private String razaoSocial;
        private String nomeFantasia;
        private String cnpj;
        private StatusVinculo statusVinculo;
        private String funcao;
    }
}
