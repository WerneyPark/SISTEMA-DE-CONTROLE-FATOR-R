package com.fatorr.backend.service;

import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.domain.FaixaTributaria;
import com.fatorr.backend.domain.FaturamentoMensal;
import com.fatorr.backend.domain.FolhaPagamentoMensal;
import com.fatorr.backend.domain.ParametroTributario;
import com.fatorr.backend.dto.FatorRResultadoDTO;
import com.fatorr.backend.dto.ProjecaoCenarioDTO;
import com.fatorr.backend.repository.ContaReceberRepository;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.repository.EncerramentoCompetenciaRepository;
import com.fatorr.backend.repository.FaturamentoMensalRepository;
import com.fatorr.backend.repository.FolhaDetalhadaRepository;
import com.fatorr.backend.repository.FolhaPagamentoMensalRepository;
import com.fatorr.backend.repository.ParametroTributarioRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Mock
    private ParametroTributarioRepository parametroRepository;

    @Mock
    private EncerramentoCompetenciaRepository encerramentoRepository;

    @Mock
    private ContaReceberRepository contaReceberRepository;

    @Mock
    private FolhaDetalhadaRepository folhaDetalhadaRepository;

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

    @Test
    void deveGarantirFatorRIdenticoNoMes1ParaTodosOsCenariosEVariandoApenasNoMes2() {
        // Arrange
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaMock));
        when(parametroRepository.findByAtivoTrueAndEmpresaId(1L)).thenReturn(Optional.empty());
        when(parametroRepository.findByAtivoTrueAndEmpresaIsNull()).thenReturn(Optional.empty());

        List<FaturamentoMensal> faturamentos12m = new ArrayList<>();
        List<FolhaPagamentoMensal> folhas12m = new ArrayList<>();

        for (int i = 12; i >= 1; i--) {
            YearMonth ym = competenciaReferencia.minusMonths(i);
            FaturamentoMensal f = new FaturamentoMensal();
            f.setCompetencia(ym);
            f.setValor(new BigDecimal("10000.00"));
            faturamentos12m.add(f);

            FolhaPagamentoMensal folha = new FolhaPagamentoMensal();
            folha.setCompetencia(ym);
            folha.setValor(new BigDecimal("3000.00"));
            folhas12m.add(folha);
        }

        when(faturamentoMensalRepository.findUltimos12Meses(eq(1L), any(YearMonth.class), any(YearMonth.class)))
                .thenReturn(faturamentos12m);
        when(folhaPagamentoMensalRepository.findUltimos12Meses(eq(1L), any(YearMonth.class), any(YearMonth.class)))
                .thenReturn(folhas12m);

        // Competência atual (M = 2026-05)
        FaturamentoMensal faturamentoAtual = new FaturamentoMensal();
        faturamentoAtual.setCompetencia(competenciaReferencia);
        faturamentoAtual.setValor(new BigDecimal("10000.00"));

        FolhaPagamentoMensal folhaAtual = new FolhaPagamentoMensal();
        folhaAtual.setCompetencia(competenciaReferencia);
        folhaAtual.setValor(new BigDecimal("3000.00"));

        when(faturamentoMensalRepository.findByEmpresaIdAndCompetencia(1L, competenciaReferencia))
                .thenReturn(List.of(faturamentoAtual));
        when(folhaPagamentoMensalRepository.findByEmpresaIdAndCompetencia(1L, competenciaReferencia))
                .thenReturn(List.of(folhaAtual));

        // Act
        List<ProjecaoCenarioDTO> cenarios = calculoFatorRService.simularProximosMeses(1L, competenciaReferencia);

        // Assert
        assertEquals(3, cenarios.size());

        ProjecaoCenarioDTO otimista = cenarios.stream().filter(c -> c.getNomeCenario().contains("Otimista")).findFirst().orElseThrow();
        ProjecaoCenarioDTO neutro = cenarios.stream().filter(c -> c.getNomeCenario().contains("Neutro")).findFirst().orElseThrow();
        ProjecaoCenarioDTO pessimista = cenarios.stream().filter(c -> c.getNomeCenario().contains("Pessimista")).findFirst().orElseThrow();

        // --- Verificação do Mês 1 (M+1: Junho/2026) ---
        // O Fator R deve ser RIGOROSAMENTE IGUAL nos três cenários para M+1
        ProjecaoCenarioDTO.MesProjecaoDTO mes1Otimista = otimista.getMeses().get(0);
        ProjecaoCenarioDTO.MesProjecaoDTO mes1Neutro = neutro.getMeses().get(0);
        ProjecaoCenarioDTO.MesProjecaoDTO mes1Pessimista = pessimista.getMeses().get(0);

        assertEquals(competenciaReferencia.plusMonths(1), mes1Otimista.getCompetencia());
        assertEquals(0, new BigDecimal("11500.00").compareTo(mes1Otimista.getFaturamentoEstimado()));
        assertEquals(0, new BigDecimal("10000.00").compareTo(mes1Neutro.getFaturamentoEstimado()));
        assertEquals(0, new BigDecimal("8500.00").compareTo(mes1Pessimista.getFaturamentoEstimado()));

        // Fator R deve ser idêntico (30.00%) pois os 12 meses anteriores a M+1 são iguais para todos
        assertEquals(mes1Neutro.getPercentualFatorR(), mes1Otimista.getPercentualFatorR());
        assertEquals(mes1Neutro.getPercentualFatorR(), mes1Pessimista.getPercentualFatorR());
        assertEquals(0, new BigDecimal("30.00").compareTo(mes1Neutro.getPercentualFatorR()));
        assertEquals(mes1Neutro.getAnexoAplicavel(), mes1Otimista.getAnexoAplicavel());
        assertEquals(mes1Neutro.getAnexoAplicavel(), mes1Pessimista.getAnexoAplicavel());
        assertEquals("Anexo III", mes1Neutro.getAnexoAplicavel());

        // RBT12 do Mês 1 também deve ser idêntico nos três cenários
        assertEquals(mes1Neutro.getRbt12Estimado(), mes1Otimista.getRbt12Estimado());
        assertEquals(mes1Neutro.getRbt12Estimado(), mes1Pessimista.getRbt12Estimado());

        // --- Verificação do Mês 2 (M+2: Julho/2026) ---
        // No Mês 2, o faturamento projetado de M+1 passa a integrar a janela de 12 meses anteriores.
        ProjecaoCenarioDTO.MesProjecaoDTO mes2Otimista = otimista.getMeses().get(1);
        ProjecaoCenarioDTO.MesProjecaoDTO mes2Neutro = neutro.getMeses().get(1);
        ProjecaoCenarioDTO.MesProjecaoDTO mes2Pessimista = pessimista.getMeses().get(1);

        assertEquals(competenciaReferencia.plusMonths(2), mes2Otimista.getCompetencia());

        // No cenário Otimista, RBT12 aumentou (+15%), logo o Fator R reduziu
        assertTrue(mes2Otimista.getRbt12Estimado().compareTo(mes2Neutro.getRbt12Estimado()) > 0);
        assertTrue(mes2Otimista.getPercentualFatorR().compareTo(mes2Neutro.getPercentualFatorR()) < 0);

        // No cenário Pessimista, RBT12 diminuiu (-15%), logo o Fator R aumentou
        assertTrue(mes2Pessimista.getRbt12Estimado().compareTo(mes2Neutro.getRbt12Estimado()) < 0);
        assertTrue(mes2Pessimista.getPercentualFatorR().compareTo(mes2Neutro.getPercentualFatorR()) > 0);
    }
}
