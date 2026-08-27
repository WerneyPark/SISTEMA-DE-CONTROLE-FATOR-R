package com.fatorr.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contas_a_receber")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContaReceber {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 36)
    private String empresaId;

    @Column(name = "cliente_id", nullable = false, length = 36)
    private String clienteId;

    @Column(name = "numero_documento", length = 100)
    private String numeroDocumento;

    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(length = 30)
    private String status = "PENDENTE"; // 'PENDENTE', 'PAGO', 'CANCELADO', 'VENCIDO'

    @Column(length = 50)
    private String origem = "MANUAL"; // 'MANUAL', 'IMPORTACAO_XML'

    @Column(name = "xml_chave_acesso", length = 100)
    private String xmlChaveAcesso;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
