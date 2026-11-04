package com.fatorr.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaturamentoHistoricoDTO {
    private YearMonth competencia;
    private BigDecimal faturamento;
    private BigDecimal folhaPagamento;
    private boolean encerrado;

    public FaturamentoHistoricoDTO(YearMonth competencia, BigDecimal faturamento, BigDecimal folhaPagamento) {
        this.competencia = competencia;
        this.faturamento = faturamento;
        this.folhaPagamento = folhaPagamento;
        this.encerrado = false;
    }
}
