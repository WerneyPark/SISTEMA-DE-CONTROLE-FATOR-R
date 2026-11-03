package com.fatorr.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametrosEmissaoLaudoDTO {
    private String empresaId;
    private String competencia; // Ex: "2026-08"
    private String tipoLaudo; // LAUDO_ENQUADRAMENTO_FATOR_R, MEMORIA_CALCULO_12M, ESTUDO_COMPARATIVO_REGIMES, DOSSIE_COMPLETO
    private String formato; // PDF, EXCEL
    private boolean incluirProjecaoCenarios;
    private boolean incluirAssinaturaContador;
    private String observacaoPersonalizada;
}
