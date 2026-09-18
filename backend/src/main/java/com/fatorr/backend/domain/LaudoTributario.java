package com.fatorr.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "laudos_tributarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LaudoTributario {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 36)
    private String empresaId;

    @Column(name = "usuario_id", nullable = false, length = 36)
    private String usuarioId;

    @Column(nullable = false, length = 7)
    private String competencia; // Ex: "2026-08"

    @Column(name = "tipo_laudo", nullable = false, length = 50)
    private String tipoLaudo;

    @Column(name = "hash_autenticidade", nullable = false, unique = true, length = 100)
    private String hashAutenticidade;

    @Column(name = "caminho_arquivo")
    private String caminhoArquivo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
