package com.fatorr.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Data
@Builder
public class SimulacaoTributariaDTO {
    // Dados da empresa
    private Long empresaId;
    private String razaoSocial;
    private String cnpj;

    // Competência de referência
    private YearMonth competenciaReferencia;
    private int mesesComDados;
    private boolean competenciaReferenciaEncerrada;
    private List<String> competenciasEncerradas;

    // Totais acumulados (12 meses)
    private BigDecimal totalFaturamento12Meses;
    private BigDecimal totalFolha12Meses;

    // RBT12 e Faixas
    private BigDecimal rbt12;
    private Integer faixaEncontrada;
    private String mensagemFaixa6;

    // Fator R
    private BigDecimal percentualFatorR;
    private String anexoAplicavel;

    // Alerta de proximidade
    private boolean alertaProximidadeLimite;
    private String mensagemAlerta;

    // Estimativas tributárias para o mês de referência
    private BigDecimal aliquotaEfetivaSimplesNacional;
    private BigDecimal valorEstimadoSimplesNacional;
    private BigDecimal aliquotaEfetivaLucroPresumido;
    private BigDecimal valorEstimadoLucroPresumido;
    private BigDecimal valorPis;
    private BigDecimal valorCofins;
    private BigDecimal valorIss;
    private BigDecimal valorIrpj;
    private BigDecimal valorCsll;
    private BigDecimal valorAdicionalIrpj;
    private BigDecimal economiaEstimada;
    private String regimeMaisVantajoso;

    // Mensagem geral
    private String mensagem;

    // Histórico de Faturamento para exibição na UI
    private List<FaturamentoHistoricoDTO> historicoFaturamentos;
}
