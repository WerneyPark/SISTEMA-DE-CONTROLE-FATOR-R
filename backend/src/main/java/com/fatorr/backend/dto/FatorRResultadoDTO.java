package com.fatorr.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@Builder
public class FatorRResultadoDTO {
    private Long empresaId;
    private String razaoSocial;
    private YearMonth competenciaReferencia;
    private BigDecimal totalFaturamento12Meses;
    private BigDecimal totalFolha12Meses;
    private BigDecimal percentualFatorR;
    private String anexoAplicavel;
    private String mensagem;
}
