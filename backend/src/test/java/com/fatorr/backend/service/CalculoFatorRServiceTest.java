package com.fatorr.backend.service;

import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.domain.FaturamentoMensal;
import com.fatorr.backend.domain.FolhaPagamentoMensal;
import com.fatorr.backend.dto.FatorRResultadoDTO;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.repository.FaturamentoMensalRepository;
import com.fatorr.backend.repository.FolhaPagamentoMensalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculoFatorRServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private FaturamentoMensalRepository faturamentoMensalRepository;

    @Mock
    private FolhaPagamentoMensalRepository folhaPagamentoMensalRepository;

    @InjectMocks
    private CalculoFatorRService calculoFatorRService;

    private Empresa empresaMock;
    private YearMonth competenciaReferencia;

    @BeforeEach
    void setUp() {
        empresaMock = new Empresa();
        empresaMock.setId(1L);
        empresaMock.setRazaoSocial("Empresa Teste");
        empresaMock.setCnpj("12345678000199");
        competenciaReferencia = YearMonth.of(2026, 5); // Maio de 2026
    }

    @Test
    void deveRetornarAnexoIIISendoFatorR28PorCentoOuMais() {
        // Arrange
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaMock));

        List<FaturamentoMensal> faturamentos = new ArrayList<>();
        List<FolhaPagamentoMensal> folhas = new ArrayList<>();
        
        for (int i = 1; i <= 12; i++) {
            FaturamentoMensal f = new FaturamentoMensal();
            f.setValor(new BigDecimal("8333.33"));
            faturamentos.add(f);
            
            FolhaPagamentoMensal folha = new FolhaPagamentoMensal();
            folha.setValor(new BigDecimal("2333.33"));
            folhas.add(folha);
        }
        
        faturamentos.get(11).setValor(new BigDecimal("8333.37"));
        folhas.get(11).setValor(new BigDecimal("2333.37"));

        when(faturamentoMensalRepository.findUltimos12Meses(eq(1L), any(YearMonth.class), any(YearMonth.class)))
                .thenReturn(faturamentos);
        when(folhaPagamentoMensalRepository.findUltimos12Meses(eq(1L), any(YearMonth.class), any(YearMonth.class)))
                .thenReturn(folhas);

        // Act
        FatorRResultadoDTO resultado = calculoFatorRService.calcularFatorR(1L, competenciaReferencia);

        // Assert
        assertEquals("Anexo III", resultado.getAnexoAplicavel());
        assertEquals(0, new BigDecimal("28000").compareTo(resultado.getTotalFolha12Meses()));
        assertEquals(0, new BigDecimal("100000").compareTo(resultado.getTotalFaturamento12Meses()));
        assertEquals(0, new BigDecimal("28").compareTo(resultado.getPercentualFatorR()));
    }

    @Test
    void deveRetornarAnexoVSendoFatorRMenorQue28PorCento() {
        // Arrange
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaMock));

        List<FaturamentoMensal> faturamentos = new ArrayList<>();
        List<FolhaPagamentoMensal> folhas = new ArrayList<>();
        
        for (int i = 1; i <= 12; i++) {
            FaturamentoMensal f = new FaturamentoMensal();
            f.setValor(new BigDecimal("8333.33"));
            faturamentos.add(f);
            
            FolhaPagamentoMensal folha = new FolhaPagamentoMensal();
            folha.setValor(new BigDecimal("1666.66"));
            folhas.add(folha);
        }
        faturamentos.get(11).setValor(new BigDecimal("8333.37"));
        folhas.get(11).setValor(new BigDecimal("1666.74"));

        when(faturamentoMensalRepository.findUltimos12Meses(eq(1L), any(YearMonth.class), any(YearMonth.class)))
                .thenReturn(faturamentos);
        when(folhaPagamentoMensalRepository.findUltimos12Meses(eq(1L), any(YearMonth.class), any(YearMonth.class)))
                .thenReturn(folhas);

        // Act
        FatorRResultadoDTO resultado = calculoFatorRService.calcularFatorR(1L, competenciaReferencia);

        // Assert
        assertEquals("Anexo V", resultado.getAnexoAplicavel());
        assertEquals(0, new BigDecimal("20").compareTo(resultado.getPercentualFatorR()));
    }
}
