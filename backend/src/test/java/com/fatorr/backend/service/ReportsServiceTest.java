package com.fatorr.backend.service;

import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.domain.LaudoTributario;
import com.fatorr.backend.domain.OrganizacaoContabil;
import com.fatorr.backend.domain.Usuario;
import com.fatorr.backend.dto.FaturamentoHistoricoDTO;
import com.fatorr.backend.dto.ParametrosEmissaoLaudoDTO;
import com.fatorr.backend.dto.ProjecaoCenarioDTO;
import com.fatorr.backend.dto.SimulacaoTributariaDTO;
import com.fatorr.backend.dto.ValidacaoLaudoResponseDTO;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.repository.LaudoTributarioRepository;
import com.fatorr.backend.repository.OrganizacaoContabilRepository;
import com.fatorr.backend.repository.UsuarioRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportsServiceTest {

    @Mock
    private CalculoFatorRService calculoFatorRService;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private OrganizacaoContabilRepository organizacaoContabilRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private LaudoTributarioRepository laudoTributarioRepository;

    @InjectMocks
    private ReportsService reportsService;

    private Empresa empresaMock;
    private OrganizacaoContabil orgMock;
    private SimulacaoTributariaDTO simulacaoMock;
    private List<ProjecaoCenarioDTO> projecoesMock;

    @BeforeEach
    void setUp() {
        empresaMock = new Empresa();
        empresaMock.setId(1L);
        empresaMock.setUuidId("emp-uuid-001");
        empresaMock.setRazaoSocial("Clínica Médica Alpha Ltda");
        empresaMock.setNomeFantasia("Clínica Alpha");
        empresaMock.setCnpj("12ABC345000199");
        empresaMock.setOrganizacaoContabilId("org-uuid-001");

        orgMock = new OrganizacaoContabil(
                "org-uuid-001",
                "Contabilidade Nacional Modelo Ltda",
                "98ZYX765000111",
                "CRC-SP 123456/O",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        List<FaturamentoHistoricoDTO> historico = new ArrayList<>();
        YearMonth ymRef = YearMonth.of(2026, 8);
        for (int i = 12; i >= 1; i--) {
            historico.add(new FaturamentoHistoricoDTO(
                    ymRef.minusMonths(i),
                    new BigDecimal("100000.00"),
                    new BigDecimal("30000.00")
            ));
        }

        simulacaoMock = SimulacaoTributariaDTO.builder()
                .empresaId(1L)
                .razaoSocial("Clínica Médica Alpha Ltda")
                .cnpj("12ABC345000199")
                .competenciaReferencia(ymRef)
                .totalFaturamento12Meses(new BigDecimal("1200000.00"))
                .totalFolha12Meses(new BigDecimal("360000.00"))
                .rbt12(new BigDecimal("1200000.00"))
                .percentualFatorR(new BigDecimal("30.00"))
                .anexoAplicavel("Anexo III")
                .alertaProximidadeLimite(false)
                .aliquotaEfetivaSimplesNacional(new BigDecimal("13.50"))
                .valorEstimadoSimplesNacional(new BigDecimal("13500.00"))
                .aliquotaEfetivaLucroPresumido(new BigDecimal("16.33"))
                .valorEstimadoLucroPresumido(new BigDecimal("16330.00"))
                .valorPis(new BigDecimal("650.00"))
                .valorCofins(new BigDecimal("3000.00"))
                .valorIss(new BigDecimal("5000.00"))
                .valorIrpj(new BigDecimal("4800.00"))
                .valorAdicionalIrpj(new BigDecimal("0.00"))
                .valorCsll(new BigDecimal("2880.00"))
                .economiaEstimada(new BigDecimal("2830.00"))
                .regimeMaisVantajoso("Simples Nacional")
                .mensagem("Enquadrado favoravelmente no Anexo III.")
                .historicoFaturamentos(historico)
                .build();

        projecoesMock = new ArrayList<>();
        List<ProjecaoCenarioDTO.MesProjecaoDTO> mesesOtimista = List.of(
                new ProjecaoCenarioDTO.MesProjecaoDTO(YearMonth.of(2026, 9), new BigDecimal("115000.00"), new BigDecimal("34500.00"), new BigDecimal("1250000.00"), new BigDecimal("30.00"), "Anexo III", new BigDecimal("14000.00")),
                new ProjecaoCenarioDTO.MesProjecaoDTO(YearMonth.of(2026, 10), new BigDecimal("115000.00"), new BigDecimal("34500.00"), new BigDecimal("1300000.00"), new BigDecimal("30.00"), "Anexo III", new BigDecimal("14500.00"))
        );
        projecoesMock.add(new ProjecaoCenarioDTO("Cenário Otimista (+15%)", "Crescimento sustentado", mesesOtimista));
    }

    @Test
    @DisplayName("Deve emitir laudo em PDF sem erros e salvar no repositório")
    void testEmitirPdfLaudoSucesso() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaMock));
        when(organizacaoContabilRepository.findById("org-uuid-001")).thenReturn(Optional.of(orgMock));
        when(calculoFatorRService.simularTributacao(eq(1L), any(YearMonth.class))).thenReturn(simulacaoMock);
        when(calculoFatorRService.simularProximosMeses(eq(1L), any(YearMonth.class))).thenReturn(projecoesMock);

        ParametrosEmissaoLaudoDTO params = ParametrosEmissaoLaudoDTO.builder()
                .empresaId("1")
                .competencia("2026-08")
                .tipoLaudo("DOSSIE_COMPLETO")
                .formato("PDF")
                .incluirProjecaoCenarios(true)
                .incluirAssinaturaContador(true)
                .observacaoPersonalizada("Auditoria fiscal preventiva de enquadramento.")
                .build();

        byte[] pdfBytes = reportsService.emitirLaudo(params, "usr-01");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100);

        // Valida cabeçalho mágico de PDF: "%PDF"
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);

        // Verifica se persistiu na base de dados
        verify(laudoTributarioRepository, times(1)).save(any(LaudoTributario.class));
    }

    @Test
    @DisplayName("Deve exportar laudo e memória de cálculo em formato Excel (.xlsx) válido")
    void testEmitirExcelLaudoSucesso() throws Exception {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaMock));
        when(organizacaoContabilRepository.findById("org-uuid-001")).thenReturn(Optional.of(orgMock));
        when(calculoFatorRService.simularTributacao(eq(1L), any(YearMonth.class))).thenReturn(simulacaoMock);

        ParametrosEmissaoLaudoDTO params = ParametrosEmissaoLaudoDTO.builder()
                .empresaId("1")
                .competencia("2026-08")
                .tipoLaudo("MEMORIA_CALCULO_12M")
                .formato("EXCEL")
                .incluirProjecaoCenarios(false)
                .incluirAssinaturaContador(true)
                .build();

        byte[] excelBytes = reportsService.emitirLaudo(params, "usr-01");

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 100);

        // Abre a pasta de trabalho do Excel para checar a integridade e as 3 abas requeridas
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            assertNotNull(wb.getSheet("Resumo e Fator R"));
            assertNotNull(wb.getSheet("Memoria de Calculo 12 Meses"));
            assertNotNull(wb.getSheet("Comparativo Regimes Fiscais"));
            assertEquals(3, wb.getNumberOfSheets());
        }

        verify(laudoTributarioRepository, times(1)).save(any(LaudoTributario.class));
    }

    @Test
    @DisplayName("Deve calcular Hash SHA-256 válido e determinístico")
    void testCalculoHashSha256() {
        byte[] input = "FATOR_R_DADOS_CANONICOS_2026".getBytes(StandardCharsets.UTF_8);
        String hash1 = reportsService.calcularHashSha256(input);
        String hash2 = reportsService.calcularHashSha256(input);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
        assertTrue(hash1.matches("^[0-9A-F]{64}$"));
    }

    @Test
    @DisplayName("Deve validar autenticidade com sucesso quando hash existir no banco")
    void testValidarLaudoExistente() {
        String hashValido = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855";
        LaudoTributario laudoMock = new LaudoTributario();
        laudoMock.setId("laudo-uuid-123");
        laudoMock.setEmpresaId("1");
        laudoMock.setCompetencia("2026-08");
        laudoMock.setTipoLaudo("DOSSIE_COMPLETO");
        laudoMock.setHashAutenticidade(hashValido);
        laudoMock.setCreatedAt(LocalDateTime.of(2026, 8, 15, 10, 30));

        when(laudoTributarioRepository.findByHashAutenticidade(hashValido)).thenReturn(Optional.of(laudoMock));
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaMock));
        when(organizacaoContabilRepository.findById("org-uuid-001")).thenReturn(Optional.of(orgMock));
        when(calculoFatorRService.simularTributacao(eq(1L), any(YearMonth.class))).thenReturn(simulacaoMock);

        ValidacaoLaudoResponseDTO resultado = reportsService.validarLaudo(hashValido);

        assertNotNull(resultado);
        assertTrue(resultado.isValido());
        assertEquals(hashValido, resultado.getHashAutenticidade());
        assertEquals("Clínica Médica Alpha Ltda", resultado.getEmpresaNome());
        assertEquals("12ABC345000199", resultado.getEmpresaCnpj());
        assertEquals("Anexo III", resultado.getAnexoEnquadrado());
        assertEquals(new BigDecimal("30.00"), resultado.getPercentualFatorR());
    }

    @Test
    @DisplayName("Deve retornar invalido quando hash não existir")
    void testValidarLaudoInexistente() {
        String hashInexistente = "0000000000000000000000000000000000000000000000000000000000000000";
        when(laudoTributarioRepository.findByHashAutenticidade(hashInexistente)).thenReturn(Optional.empty());

        ValidacaoLaudoResponseDTO resultado = reportsService.validarLaudo(hashInexistente);

        assertNotNull(resultado);
        assertFalse(resultado.isValido());
        assertEquals(hashInexistente, resultado.getHashAutenticidade());
        assertNull(resultado.getEmpresaNome());
    }
}
