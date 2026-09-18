package com.fatorr.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "folhas_detalhadas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"empresa_id", "competencia"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolhaDetalhada {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 36)
    private String empresaId;

    @Column(nullable = false, length = 7)
    private String competencia; // Ex: "2026-08"

    @Column(name = "valor_salarios", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorSalarios = BigDecimal.ZERO;

    @Column(name = "valor_pro_labore", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorProLabore = BigDecimal.ZERO;

    @Column(name = "valor_decimo_terceiro", precision = 15, scale = 2)
    private BigDecimal valorDecimoTerceiro = BigDecimal.ZERO;

    @Column(name = "valor_inss_patronal", precision = 15, scale = 2)
    private BigDecimal valorInssPatronal = BigDecimal.ZERO;

    @Column(name = "valor_fgts", precision = 15, scale = 2)
    private BigDecimal valorFgts = BigDecimal.ZERO;

    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal;

    @Column(length = 50)
    private String origem = "MANUAL"; // 'MANUAL', 'IMPORTACAO_ESOCIAL', 'IMPORTACAO_CSV'

    @Column(name = "xml_chave_recibo", length = 100)
    private String xmlChaveRecibo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
