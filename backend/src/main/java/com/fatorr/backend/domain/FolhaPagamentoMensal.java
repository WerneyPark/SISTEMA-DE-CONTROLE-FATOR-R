package com.fatorr.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Table(name = "folhas_pagamento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolhaPagamentoMensal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false)
    private YearMonth competencia;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;
}
