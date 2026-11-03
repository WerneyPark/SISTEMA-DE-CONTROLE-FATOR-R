package com.fatorr.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fatorr.backend.util.CpfUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_id", length = 36)
    private String uuidId;

    private String nome;

    @Column(unique = true)
    private String email;

    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @JsonIgnore
    private String senha;

    @Column(name = "senha_hash")
    @JsonIgnore
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_perfil")
    private TipoPerfil tipoPerfil;

    @Enumerated(EnumType.STRING)
    private TipoUsuario tipo; // Compatibilidade legada

    @Column(name = "organizacao_contabil_id", length = 36)
    private String organizacaoContabilId;

    @Enumerated(EnumType.STRING)
    private StatusUsuario status = StatusUsuario.ATIVO;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_expires")
    private java.time.LocalDateTime resetPasswordExpires;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "usuario_empresa",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "empresa_id")
    )
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    @JsonIgnore
    private Set<Empresa> empresas = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (this.uuidId == null || this.uuidId.trim().isEmpty()) {
            this.uuidId = UUID.randomUUID().toString();
        }
        if (this.cpf != null) {
            this.cpf = CpfUtils.unmask(this.cpf);
        }
    }
}
