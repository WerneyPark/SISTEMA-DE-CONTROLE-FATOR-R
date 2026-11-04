package com.fatorr.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "encerramentos_competencia", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"empresa_id", "competencia"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncerramentoCompetencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private YearMonth competencia;

    @Column(nullable = false)
    private boolean encerrado;

    @Column(name = "data_encerramento")
    private LocalDateTime dataEncerramento;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
