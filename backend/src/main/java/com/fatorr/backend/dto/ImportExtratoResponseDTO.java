package com.fatorr.backend.dto;

import com.fatorr.backend.domain.ExtratoLancamento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ImportExtratoResponseDTO {
    private boolean sucesso;
    private String banco;
    private String conta;
    private int totalLancamentos;
    private int novosLancamentos;
    private int duplicadosIgnorados;
    private List<ExtratoLancamento> lancamentos;
}