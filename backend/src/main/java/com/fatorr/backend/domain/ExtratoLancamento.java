package com.fatorr.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "extrato_lancamentos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtratoLancamento {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 36)
    private String empresaId;

    @Column(length = 100)
    private String banco;

    @Column(length = 50)
    private String conta;

    @Column(name = "data_lancamento", nullable = false)
    private LocalDate dataLancamento;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 10)
    private String tipo; // 'CREDITO', 'DEBITO'

    @Column(length = 100)
    private String fitid;

    @Column(name = "status_conciliacao", length = 30)
    private String statusConciliacao = "PENDENTE"; // 'PENDENTE', 'CONCILIADO'

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
