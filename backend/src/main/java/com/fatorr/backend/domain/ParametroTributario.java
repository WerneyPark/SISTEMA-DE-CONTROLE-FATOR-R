package com.fatorr.backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "parametros_tributarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParametroTributario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 5, scale = 4)
    private BigDecimal aliquotaPis = new BigDecimal("0.0065");

    @Column(precision = 5, scale = 4)
    private BigDecimal aliquotaCofins = new BigDecimal("0.0300");

    @Column(precision = 5, scale = 4)
    private BigDecimal aliquotaIss = new BigDecimal("0.0500");

    @Column(precision = 5, scale = 4)
    private BigDecimal aliquotaIrpj = new BigDecimal("0.1500");

    @Column(precision = 5, scale = 4)
    private BigDecimal aliquotaAdicionalIrpj = new BigDecimal("0.1000");

    @Column(precision = 5, scale = 4)
    private BigDecimal aliquotaCsll = new BigDecimal("0.0900");

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Empresa empresa;

    @OneToMany(mappedBy = "parametro", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private java.util.List<FaixaTributaria> faixas = new java.util.ArrayList<>();
}
