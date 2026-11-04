package com.fatorr.backend.service;

import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.domain.FaturamentoMensal;
import com.fatorr.backend.domain.FolhaDetalhada;
import com.fatorr.backend.domain.FolhaPagamentoMensal;
import com.fatorr.backend.domain.ParametroTributario;
import com.fatorr.backend.dto.ImportFolhaResponseDTO;
import com.fatorr.backend.dto.ProLaboreIdealDTO;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.repository.EncerramentoCompetenciaRepository;
import com.fatorr.backend.repository.FaturamentoMensalRepository;
import com.fatorr.backend.repository.FolhaDetalhadaRepository;
import com.fatorr.backend.repository.FolhaPagamentoMensalRepository;
import com.fatorr.backend.repository.ParametroTributarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private FolhaDetalhadaRepository folhaDetalhadaRepository;

    @Mock
    private FolhaPagamentoMensalRepository folhaPagamentoMensalRepository;

    @Mock
    private FaturamentoMensalRepository faturamentoRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private TenantResolverService tenantResolverService;

    @Mock
    private ParametroTributarioRepository parametroRepository;

    @Mock
    private EncerramentoCompetenciaRepository encerramentoRepository;

    @InjectMocks
    private PayrollService payrollService;

    private Empresa empresaMock;
    private final String empresaUuid = "emp-alpha-uuid-0001";
    private final Long empresaIdNum = 1L;

    @BeforeEach
    void setUp() {
        empresaMock = new Empresa();
        empresaMock.setId(empresaIdNum);
        empresaMock.setUuidId(empresaUuid);
        empresaMock.setRazaoSocial("Alpha Tech Ltda");
        empresaMock.setCnpj("12345678000199");
    }

    @Test
    void deveSalvarFolhaDetalhadaESincronizarComFolhaPagamentoMensal() {
        // Arrange
        when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);
        when(folhaDetalhadaRepository.findByEmpresaIdAndCompetencia(empresaUuid, "2026-08"))
                .thenReturn(Optional.empty());

        when(folhaDetalhadaRepository.save(any(FolhaDetalhada.class))).thenAnswer(i -> i.getArgument(0));

        when(folhaPagamentoMensalRepository.findByEmpresaIdAndCompetencia(eq(empresaIdNum), eq(YearMonth.of(2026, 8))))
                .thenReturn(Collections.emptyList());

        FolhaDetalhada novaFolha = new FolhaDetalhada();
        novaFolha.setEmpresaId(empresaUuid);
        novaFolha.setCompetencia("2026-08");
        novaFolha.setValorSalarios(new BigDecimal("10000.00"));
        novaFolha.setValorProLabore(new BigDecimal("5000.00"));
        novaFolha.setValorDecimoTerceiro(new BigDecimal("1000.00"));
        novaFolha.setValorInssPatronal(new BigDecimal("2000.00"));
        novaFolha.setValorFgts(new BigDecimal("800.00"));

        // Act
        FolhaDetalhada salva = payrollService.saveFolha(novaFolha);

        // Assert
        assertNotNull(salva);
        assertEquals(0, new BigDecimal("18800.00").compareTo(salva.getValorTotal()));
        assertEquals("MANUAL", salva.getOrigem());

        // Verifica sincronização atômica na tabela folhas_pagamento
        ArgumentCaptor<FolhaPagamentoMensal> captor = ArgumentCaptor.forClass(FolhaPagamentoMensal.class);
        verify(folhaPagamentoMensalRepository).save(captor.capture());
        FolhaPagamentoMensal sincronizada = captor.getValue();
        assertEquals(empresaMock, sincronizada.getEmpresa());
        assertEquals(YearMonth.of(2026, 8), sincronizada.getCompetencia());
        assertEquals(0, new BigDecimal("18800.00").compareTo(sincronizada.getValor()));
    }

    @Test
    void deveCalcularProLaboreIdealComSucessoParaAtingirAnexoIII() {
        // Arrange
        when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);

        // Simula RBT12 = R$ 100.000,00 (12 meses de R$ 8.333,33)
        List<FaturamentoMensal> faturamentos = new ArrayList<>();
        List<FolhaPagamentoMensal> folhas = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            FaturamentoMensal fat = new FaturamentoMensal();
            fat.setValor(new BigDecimal("8333.33"));
            faturamentos.add(fat);

            FolhaPagamentoMensal folha = new FolhaPagamentoMensal();
            folha.setValor(new BigDecimal("1000.00")); // Total folha = R$ 12.000,00 (12% do RBT12 -> Anexo V)
            folhas.add(folha);
        }
        faturamentos.get(11).setValor(new BigDecimal("8333.37")); // fecha 100.000

        when(faturamentoRepository.findUltimos12Meses(eq(empresaIdNum), any(YearMonth.class), any(YearMonth.class)))
                .thenReturn(faturamentos);
        when(folhaPagamentoMensalRepository.findUltimos12Meses(eq(empresaIdNum), any(YearMonth.class), any(YearMonth.class)))
                .thenReturn(folhas);
        when(parametroRepository.findByAtivoTrueAndEmpresaId(empresaIdNum)).thenReturn(Optional.empty());
        when(parametroRepository.findByAtivoTrueAndEmpresaIsNull()).thenReturn(Optional.empty());

        // Act: Simula competência 2026-08 com faturamento previsto de R$ 10.000,00
        ProLaboreIdealDTO resultado = payrollService.calcularProLaboreIdeal(
                empresaUuid,
                "2026-08",
                new BigDecimal("10000.00")
        );

        // Assert
        assertNotNull(resultado);
        assertEquals(0, new BigDecimal("100000.00").compareTo(resultado.getRbt12()));
        assertEquals(0, new BigDecimal("12000.00").compareTo(resultado.getFolha12Atual()));
        assertEquals(0, new BigDecimal("12.00").compareTo(resultado.getFatorRAtual()));
        assertEquals("Anexo V", resultado.getAnexoAtual());

        // Folha alvo: 28% de 100.000 = 28.000. Falta: 28.000 - 12.000 = 16.000
        assertEquals(0, new BigDecimal("16000.00").compareTo(resultado.getProLaboreMinimoParaAnexo3()));
        assertEquals(0, new BigDecimal("28.00").compareTo(resultado.getFatorRProjetado()));
        assertEquals("Anexo III", resultado.getAnexoProjetado());

        // INSS sócio: 11% de 16.000 limitado ao teto 2026 (R$ 908,86)
        assertEquals(0, new BigDecimal("908.86").compareTo(resultado.getInssSocio()));

        // IRPF sócio: Base = 16.000 - 908.86 = 15.091,14. Tabela RFB > 4.664,68: 27.5% - 896.00 = 3.254,06
        assertEquals(0, new BigDecimal("3254.06").compareTo(resultado.getIrpfSocio()));

        // Economia Simples Nacional: Anexo V (15.5%) vs Anexo III (6%) sobre R$ 10.000 = 9.5% = R$ 950,00
        assertEquals(0, new BigDecimal("950.00").compareTo(resultado.getEconomiaSimplesNacional()));

        assertNotNull(resultado.getMensagemRecomendacao());
    }

    @Test
    void deveIdentificarQueEmpresaJaCumpreFatorR28PorCento() {
        // Arrange
        when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);

        List<FaturamentoMensal> faturamentos = new ArrayList<>();
        List<FolhaPagamentoMensal> folhas = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            FaturamentoMensal fat = new FaturamentoMensal();
            fat.setValor(new BigDecimal("10000.00"));
            faturamentos.add(fat);

            FolhaPagamentoMensal folha = new FolhaPagamentoMensal();
            folha.setValor(new BigDecimal("3000.00")); // 30% da receita -> Já é Anexo III!
            folhas.add(folha);
        }

        when(faturamentoRepository.findUltimos12Meses(eq(empresaIdNum), any(YearMonth.class), any(YearMonth.class)))
                .thenReturn(faturamentos);
        when(folhaPagamentoMensalRepository.findUltimos12Meses(eq(empresaIdNum), any(YearMonth.class), any(YearMonth.class)))
                .thenReturn(folhas);
        when(parametroRepository.findByAtivoTrueAndEmpresaId(empresaIdNum)).thenReturn(Optional.empty());
        when(parametroRepository.findByAtivoTrueAndEmpresaIsNull()).thenReturn(Optional.empty());

        // Act
        ProLaboreIdealDTO resultado = payrollService.calcularProLaboreIdeal(
                empresaUuid,
                "2026-08",
                new BigDecimal("10000.00")
        );

        // Assert
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.getProLaboreMinimoParaAnexo3()));
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.getInssSocio()));
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.getIrpfSocio()));
        assertEquals("Anexo III", resultado.getAnexoAtual());
        assertTrue(resultado.getMensagemRecomendacao().contains("já atinge a margem de 28%"));
    }

    @Test
    void deveImportarXmlEsocialComSucessoEDiscriminarSociosEColaboradores() throws Exception {
        // Arrange
        when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);
        when(folhaDetalhadaRepository.findByEmpresaIdAndCompetencia(empresaUuid, "2026-08"))
                .thenReturn(Optional.empty());
        when(folhaDetalhadaRepository.save(any(FolhaDetalhada.class))).thenAnswer(i -> i.getArgument(0));

        String xmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <eSocial xmlns="http://www.esocial.gov.br/schema/evt/evtRemun/v_S_01_02_00">
                  <evtRemun Id="ID1000000000000002026080112345678000199">
                    <ideEvento>
                      <perApur>2026-08</perApur>
                    </ideEvento>
                    <ideTrabalhador>
                      <cpfTrab>11122233344</cpfTrab>
                      <dmDev>
                        <codCateg>101</codCateg>
                        <infoPerApur>
                          <itensRemun>
                            <codRubr>1000</codRubr>
                            <vrRubr>12000.00</vrRubr>
                          </itensRemun>
                        </infoPerApur>
                      </dmDev>
                    </ideTrabalhador>
                    <ideTrabalhador>
                      <cpfTrab>22233344455</cpfTrab>
                      <dmDev>
                        <codCateg>721</codCateg>
                        <infoPerApur>
                          <itensRemun>
                            <codRubr>3501</codRubr>
                            <natRubr>3501</natRubr>
                            <vrRubr>5000.00</vrRubr>
                          </itensRemun>
                        </infoPerApur>
                      </dmDev>
                    </ideTrabalhador>
                  </evtRemun>
                </eSocial>
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "esocial_s1200.xml",
                "application/xml",
                xmlContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ImportFolhaResponseDTO resultado = payrollService.importarEsocialXml(file, empresaUuid);

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isSucesso());
        assertEquals("2026-08", resultado.getCompetencia());
        assertEquals(1, resultado.getTotalColaboradores());
        assertEquals(1, resultado.getTotalSocios());
        assertEquals(0, new BigDecimal("12000.00").compareTo(resultado.getValorSalarios()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(resultado.getValorProLabore()));
        assertEquals(0, new BigDecimal("17000.00").compareTo(resultado.getValorTotalFolha()));
        assertEquals("IMPORTACAO_ESOCIAL", resultado.getOrigem());
        assertEquals("ID1000000000000002026080112345678000199", resultado.getXmlChaveRecibo());
    }

    @Test
    void deveImportarPlanilhaCsvComSucesso() throws Exception {
        // Arrange
        when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);
        when(folhaDetalhadaRepository.findByEmpresaIdAndCompetencia(empresaUuid, "2026-08"))
                .thenReturn(Optional.empty());
        when(folhaDetalhadaRepository.save(any(FolhaDetalhada.class))).thenAnswer(i -> i.getArgument(0));

        String csvContent = """
                competencia;valorSalarios;valorProLabore;valorDecimoTerceiro;valorInssPatronal;valorFgts
                2026-08;8500,00;4500,00;0,00;1800,00;680,00
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "folha_analitica.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        ImportFolhaResponseDTO resultado = payrollService.importarCsvOuExcel(file, empresaUuid);

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isSucesso());
        assertEquals("2026-08", resultado.getCompetencia());
        assertEquals(0, new BigDecimal("8500.00").compareTo(resultado.getValorSalarios()));
        assertEquals(0, new BigDecimal("4500.00").compareTo(resultado.getValorProLabore()));
        assertEquals(0, new BigDecimal("1800.00").compareTo(resultado.getValorInssPatronal()));
        assertEquals(0, new BigDecimal("680.00").compareTo(resultado.getValorFgts()));
        assertEquals(0, new BigDecimal("15480.00").compareTo(resultado.getValorTotalFolha()));
        assertEquals("IMPORTACAO_CSV", resultado.getOrigem());
    }

    @Test
    void deveImportarArquivosGeradosDeFolhaComSucesso() throws Exception {
        java.io.File esocialFile = new java.io.File("../arquivos para testes/folha de pagamento/01 - Inovar Softwares/2026/05/esocial_s1200_2026_05.xml");
        java.io.File csvFile = new java.io.File("../arquivos para testes/folha de pagamento/01 - Inovar Softwares/2026/05/folha_2026_05.csv");

        if (esocialFile.exists() && csvFile.exists()) {
            when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);
            when(folhaDetalhadaRepository.findByEmpresaIdAndCompetencia(eq(empresaUuid), anyString()))
                    .thenReturn(Optional.empty());
            when(folhaDetalhadaRepository.save(any(FolhaDetalhada.class))).thenAnswer(i -> i.getArgument(0));

            // Testa eSocial
            byte[] esocialBytes = java.nio.file.Files.readAllBytes(esocialFile.toPath());
            MockMultipartFile mfEsocial = new MockMultipartFile("file", "esocial.xml", "application/xml", esocialBytes);
            ImportFolhaResponseDTO respEsocial = payrollService.importarEsocialXml(mfEsocial, empresaUuid);
            assertNotNull(respEsocial);
            assertTrue(respEsocial.isSucesso());
            assertEquals("2026-05", respEsocial.getCompetencia());

            // Testa CSV
            byte[] csvBytes = java.nio.file.Files.readAllBytes(csvFile.toPath());
            MockMultipartFile mfCsv = new MockMultipartFile("file", "folha.csv", "text/csv", csvBytes);
            ImportFolhaResponseDTO respCsv = payrollService.importarCsvOuExcel(mfCsv, empresaUuid);
            assertNotNull(respCsv);
            assertTrue(respCsv.isSucesso());
            assertEquals("2026-05", respCsv.getCompetencia());
        }
    }
}
