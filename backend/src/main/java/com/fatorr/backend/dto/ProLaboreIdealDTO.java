package com.fatorr.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProLaboreIdealDTO {
    private String empresaId;
    private String razaoSocial;
    private String competencia;
    private BigDecimal rbt12;
    private BigDecimal folha12Atual;
    private BigDecimal fatorRAtual;
    private String anexoAtual;
    private BigDecimal faturamentoPrevisto;
    private BigDecimal folhaAtualSemProLabore;
    private BigDecimal folhaSemProLabore12;
    private BigDecimal proLaboreAtual12;
    private BigDecimal folhaSemProLaboreMes;
    private BigDecimal proLaboreAtualMes;
    private BigDecimal diferencaParaAnexo3;
    private BigDecimal proLaboreMinimoParaAnexo3;
    private BigDecimal fatorRProjetado;
    private String anexoProjetado; // "Anexo III" ou "Anexo V"
    private CustoTributarioAdicionalDTO custoTributarioAdicional;
    private BigDecimal inssSocio;
    private BigDecimal irpfSocio;
    private BigDecimal custoTributarioTotal;
    private BigDecimal custoTributarioAnual;
    private BigDecimal aliquotaEfetivaAnexoV;
    private BigDecimal aliquotaEfetivaAnexoIII;
    private BigDecimal impostoAnexoVMensal;
    private BigDecimal impostoAnexoIIIMensal;
    private BigDecimal impostoAnexoVAnual;
    private BigDecimal impostoAnexoIIIAnual;
    private BigDecimal economiaSimplesNacional;
    private BigDecimal economiaSimplesAnual;
    private BigDecimal saldoLiquidoVantagem;
    private BigDecimal saldoLiquidoVantagemAnual;
    private boolean vantajoso;
    private String mensagemRecomendacao;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustoTributarioAdicionalDTO {
        private BigDecimal inssSocio;
        private BigDecimal irpfSocio;
        private BigDecimal custoTotal;
    }
}
