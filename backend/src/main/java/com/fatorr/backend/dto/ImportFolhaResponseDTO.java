package com.fatorr.backend.dto;

import com.fatorr.backend.domain.FolhaDetalhada;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportFolhaResponseDTO {
    private boolean sucesso;
    private String mensagem;
    private String empresaId;
    private String competencia;
    private int totalColaboradores;
    private int totalSocios;
    private BigDecimal valorSalarios;
    private BigDecimal valorProLabore;
    private BigDecimal valorDecimoTerceiro;
    private BigDecimal valorInssPatronal;
    private BigDecimal valorFgts;
    private BigDecimal valorEncargos;
    private BigDecimal valorTotalFolha;
    private FolhaDetalhada folhaGerada;
    private String xmlChaveRecibo;
    private String origem;
}
