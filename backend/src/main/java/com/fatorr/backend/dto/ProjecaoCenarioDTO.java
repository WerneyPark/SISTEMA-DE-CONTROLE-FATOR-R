package com.fatorr.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjecaoCenarioDTO {
    private String nomeCenario;
    private String descricao;
    private List<MesProjecaoDTO> meses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MesProjecaoDTO {
        private YearMonth competencia;
        private BigDecimal faturamentoEstimado;
        private BigDecimal folhaEstimada;
        private BigDecimal rbt12Estimado;
        private BigDecimal percentualFatorR;
        private String anexoAplicavel;
        private BigDecimal valorEstimadoSimplesNacional;
    }
}
