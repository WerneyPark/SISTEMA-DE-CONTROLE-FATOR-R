package com.fatorr.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "empresa_usuarios", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"empresa_id", "usuario_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaUsuario {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 36)
    private String empresaId;

    @Column(name = "usuario_id", nullable = false, length = 36)
    private String usuarioId;

    @Column(nullable = false, length = 100)
    private String funcao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_vinculo", length = 30)
    private StatusVinculo statusVinculo = StatusVinculo.PENDENTE;

    @Column(name = "aprovado_por", length = 36)
    private String aprovadoPor;

    @Column(name = "aprovado_em")
    private LocalDateTime aprovadoEm;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
