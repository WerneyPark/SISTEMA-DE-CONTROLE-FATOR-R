package com.fatorr.backend.service;

import com.fatorr.backend.domain.ClienteFornecedor;
import com.fatorr.backend.domain.ContaReceber;
import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.domain.EncerramentoCompetencia;
import com.fatorr.backend.domain.FaturamentoMensal;
import com.fatorr.backend.repository.ClienteFornecedorRepository;
import com.fatorr.backend.repository.ContaReceberRepository;
import com.fatorr.backend.repository.EncerramentoCompetenciaRepository;
import com.fatorr.backend.repository.FaturamentoMensalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContaReceberServiceTest {

    @Mock
    private ContaReceberRepository repository;

    @Mock
    private ClienteFornecedorRepository clienteRepository;

    @Mock
    private TenantResolverService tenantResolverService;

    @Mock
    private FaturamentoMensalRepository faturamentoRepository;

    @Mock
    private EncerramentoCompetenciaRepository encerramentoRepository;

    private final LeitorXmlNfeService leitorXmlNfeService = new LeitorXmlNfeService();

    @InjectMocks
    private ContaReceberService service;

    private Empresa empresa;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1L);
        empresa.setUuidId("uuid-empresa-1");
        empresa.setCnpj("12.345.678/0001-99");
        empresa.setRazaoSocial("Empresa Teste");

        lenient().when(tenantResolverService.resolveEmpresa(anyString())).thenReturn(empresa);
        lenient().when(tenantResolverService.resolveCanonicalUuid(anyString())).thenReturn("uuid-empresa-1");
    }

    private String gerarXmlNfe(String chave, String cnpjEmit, String numero, String valor, String dataEmissao) {
        return """
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
                  <NFe><infNFe Id="NFe%s">
                    <ide><nNF>%s</nNF><dhEmi>%sT10:00:00-03:00</dhEmi></ide>
                    <emit><CNPJ>%s</CNPJ><xNome>Empresa Emitente</xNome></emit>
                    <dest><CNPJ>98765432000110</CNPJ><xNome>Cliente Dest</xNome></dest>
                    <total><ICMSTot><vNF>%s</vNF></ICMSTot></total>
                  </infNFe></NFe>
                </nfeProc>
                """.formatted(chave, numero, dataEmissao, cnpjEmit, valor);
    }

    @Test
    void deveAlimentarFaturamentoMensalObrigatoriamenteAoImportarXml() {
        String xml = gerarXmlNfe("35260112345678000199550010000001234567890123", "12345678000199", "101", "1500.00", "2026-05-15");

        when(repository.findByEmpresaIdInAndXmlChaveAcesso(anyList(), anyString())).thenReturn(Optional.empty());
        when(clienteRepository.findByEmpresaIdAndCnpjCpf(anyString(), anyString())).thenReturn(Optional.empty());
        when(clienteRepository.save(any(ClienteFornecedor.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.save(any(ContaReceber.class))).thenAnswer(i -> i.getArgument(0));
        when(faturamentoRepository.findByEmpresaIdAndCompetencia(eq(1L), eq(YearMonth.of(2026, 5)))).thenReturn(new ArrayList<>());

        List<ContaReceber> titulos = service.importarXml(
                "uuid-empresa-1",
                new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
                leitorXmlNfeService
        );

        assertNotNull(titulos);
        assertEquals(1, titulos.size());
        assertEquals(new BigDecimal("1500.00"), titulos.get(0).getValorTotal());

        ArgumentCaptor<FaturamentoMensal> fatCaptor = ArgumentCaptor.forClass(FaturamentoMensal.class);
        verify(faturamentoRepository).save(fatCaptor.capture());
        FaturamentoMensal fatSalvo = fatCaptor.getValue();
        assertEquals(YearMonth.of(2026, 5), fatSalvo.getCompetencia());
        assertEquals(new BigDecimal("1500.00"), fatSalvo.getValor());
    }

    @Test
    void deveSomarAoFaturamentoMensalExistenteAoImportarXml() {
        String xml = gerarXmlNfe("35260112345678000199550010000001234567890124", "12345678000199", "102", "500.00", "2026-05-20");

        FaturamentoMensal existente = new FaturamentoMensal();
        existente.setId(10L);
        existente.setEmpresa(empresa);
        existente.setCompetencia(YearMonth.of(2026, 5));
        existente.setValor(new BigDecimal("1000.00"));

        List<FaturamentoMensal> listaExistente = new ArrayList<>(List.of(existente));
        when(repository.findByEmpresaIdInAndXmlChaveAcesso(anyList(), anyString())).thenReturn(Optional.empty());
        when(clienteRepository.findByEmpresaIdAndCnpjCpf(anyString(), anyString())).thenReturn(Optional.empty());
        when(clienteRepository.save(any(ClienteFornecedor.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.save(any(ContaReceber.class))).thenAnswer(i -> i.getArgument(0));
        when(faturamentoRepository.findByEmpresaIdAndCompetencia(eq(1L), eq(YearMonth.of(2026, 5)))).thenReturn(listaExistente);

        service.importarXml(
                "uuid-empresa-1",
                new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
                leitorXmlNfeService,
                false // mesmo passando false, deve atualizar obrigatoriamente
        );

        verify(faturamentoRepository).save(existente);
        assertEquals(new BigDecimal("1500.00"), existente.getValor());
    }

    @Test
    void deveGerarPreviewXmlLoteComIdentificacaoDeErrosEDuplicidades() {
        String xmlValido = gerarXmlNfe("35260112345678000199550010000001234567890101", "12345678000199", "201", "1000.00", "2026-05-01");
        String xmlCnpjDivergente = gerarXmlNfe("35260112345678000199550010000001234567890102", "99999999000199", "202", "2000.00", "2026-05-02");
        String xmlDuplicadoLote = gerarXmlNfe("35260112345678000199550010000001234567890101", "12345678000199", "201", "1000.00", "2026-05-01");

        List<ContaReceberService.ArquivoXmlInput> lote = List.of(
                new ContaReceberService.ArquivoXmlInput("nfe1.xml", xmlValido.getBytes(StandardCharsets.UTF_8)),
                new ContaReceberService.ArquivoXmlInput("nfe_cnpj_errado.xml", xmlCnpjDivergente.getBytes(StandardCharsets.UTF_8)),
                new ContaReceberService.ArquivoXmlInput("nfe1_copia.xml", xmlDuplicadoLote.getBytes(StandardCharsets.UTF_8))
        );

        when(repository.findByEmpresaIdInAndXmlChaveAcesso(anyList(), eq("35260112345678000199550010000001234567890101"))).thenReturn(Optional.empty());

        List<ContaReceberService.ItemPreviewXmlLote> preview = service.previewXmlLote("uuid-empresa-1", lote, leitorXmlNfeService);

        assertEquals(3, preview.size());

        // Item 1: Válido
        assertTrue(preview.get(0).valido());
        assertFalse(preview.get(0).periodoEncerrado());
        assertFalse(preview.get(0).jaImportada());
        assertNull(preview.get(0).mensagemErro());

        // Item 2: CNPJ emitente inválido
        assertFalse(preview.get(1).valido());
        assertTrue(preview.get(1).mensagemErro().contains("não pertence à empresa ativa"));

        // Item 3: Duplicado no próprio lote
        assertFalse(preview.get(2).valido());
        assertTrue(preview.get(2).jaImportada());
        assertTrue(preview.get(2).mensagemErro().contains("duplicada no lote"));
    }

    @Test
    void deveImportarXmlLoteComSucessoConsolidandoTotais() {
        String xml1 = gerarXmlNfe("35260112345678000199550010000001234567890201", "12345678000199", "301", "1200.00", "2026-06-10");
        String xml2 = gerarXmlNfe("35260112345678000199550010000001234567890202", "12345678000199", "302", "1800.00", "2026-06-15");

        List<ContaReceberService.ArquivoXmlInput> lote = List.of(
                new ContaReceberService.ArquivoXmlInput("nota301.xml", xml1.getBytes(StandardCharsets.UTF_8)),
                new ContaReceberService.ArquivoXmlInput("nota302.xml", xml2.getBytes(StandardCharsets.UTF_8))
        );

        when(repository.findByEmpresaIdInAndXmlChaveAcesso(anyList(), anyString())).thenReturn(Optional.empty());
        when(clienteRepository.findByEmpresaIdAndCnpjCpf(anyString(), anyString())).thenReturn(Optional.empty());
        when(clienteRepository.save(any(ClienteFornecedor.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.save(any(ContaReceber.class))).thenAnswer(i -> i.getArgument(0));
        when(faturamentoRepository.findByEmpresaIdAndCompetencia(eq(1L), eq(YearMonth.of(2026, 6)))).thenReturn(new ArrayList<>());

        ContaReceberService.ResultadoImportacaoXmlLote resultado = service.importarXmlLote("uuid-empresa-1", lote, leitorXmlNfeService);

        assertEquals(2, resultado.totalArquivos());
        assertEquals(2, resultado.totalImportados());
        assertEquals(0, resultado.totalIgnoradosOuErros());
        assertEquals(new BigDecimal("3000.00"), resultado.valorTotalImportado());
        assertEquals(2, resultado.titulosGerados().size());
        assertTrue(resultado.mensagensErro().isEmpty());

        verify(faturamentoRepository, times(2)).save(any(FaturamentoMensal.class));
    }
}
