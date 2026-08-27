package com.fatorr.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "organizacoes_contabeis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizacaoContabil {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 20)
    private String cnpj;

    @Column(nullable = false, length = 50)
    private String crc;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
