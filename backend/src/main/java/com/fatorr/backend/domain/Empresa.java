package com.fatorr.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "empresas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_id", length = 36)
    private String uuidId;

    @Column(name = "razao_social", nullable = false)
    private String razaoSocial;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    @Column(nullable = false, unique = true, length = 20)
    private String cnpj;

    @Column(name = "organizacao_contabil_id", length = 36)
    private String organizacaoContabilId;

    @Column(name = "data_inicio_atividade")
    private java.time.LocalDate dataInicioAtividade;

    @ManyToMany(mappedBy = "empresas", fetch = FetchType.LAZY)
    @JsonIgnore
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    private Set<Usuario> usuarios = new HashSet<>();

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private java.util.List<FaturamentoMensal> faturamentos = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private java.util.List<FolhaPagamentoMensal> folhas = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private java.util.List<ParametroTributario> parametros = new java.util.ArrayList<>();
}
