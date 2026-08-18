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
public class FaturamentoMensalDTO {
    private Long id;
    private Long empresaId;
    private YearMonth competencia;
    private BigDecimal valor;
}
