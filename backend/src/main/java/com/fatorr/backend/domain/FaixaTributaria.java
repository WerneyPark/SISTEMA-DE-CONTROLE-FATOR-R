package com.fatorr.backend.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "faixas_tributarias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaixaTributaria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String anexo; // "III" ou "V"

    @Column(nullable = false)
    private Integer numeroFaixa; // 1 a 6

    @Column(precision = 15, scale = 2)
    private BigDecimal limiteSuperior; // null para a última faixa

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal aliquotaNominal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorDeduzir;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parametro_id")
    @JsonBackReference
    private ParametroTributario parametro;
}
