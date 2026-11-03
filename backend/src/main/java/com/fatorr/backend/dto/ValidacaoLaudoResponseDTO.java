package com.fatorr.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidacaoLaudoResponseDTO {
    private boolean valido;
    private String hashAutenticidade;
    private String dataEmissao;
    private String empresaNome;
    private String empresaCnpj;
    private String contadorResponsavel;
    private String crcContador;
    private String competencia;
    private String anexoEnquadrado;
    private BigDecimal percentualFatorR;
}
