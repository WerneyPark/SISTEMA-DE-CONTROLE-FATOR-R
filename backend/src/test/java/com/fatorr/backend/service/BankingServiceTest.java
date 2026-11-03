package com.fatorr.backend.service;

import com.fatorr.backend.domain.ConciliacaoBancaria;
import com.fatorr.backend.domain.ContaReceber;
import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.domain.ExtratoLancamento;
import com.fatorr.backend.dto.ImportExtratoResponseDTO;
import com.fatorr.backend.repository.ConciliacaoBancariaRepository;
import com.fatorr.backend.repository.ContaReceberRepository;
import com.fatorr.backend.repository.ExtratoLancamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankingServiceTest {

    @Mock
    private ExtratoLancamentoRepository extratoLancamentoRepository;

    @Mock
    private ContaReceberRepository contaReceberRepository;

    @Mock
    private ConciliacaoBancariaRepository conciliacaoBancariaRepository;

    @Mock
    private TenantResolverService tenantResolverService;

    @InjectMocks
    private BankingService bankingService;

    private Empresa empresaMock;
    private final String empresaUuid = "uuid-empresa-123";

    @BeforeEach
    void setUp() {
        empresaMock = new Empresa();
        empresaMock.setId(10L);
        empresaMock.setUuidId(empresaUuid);
        empresaMock.setRazaoSocial("Empresa Teste S/A");
    }

    @Test
    @DisplayName("Deve importar arquivo OFX com sucesso criando lançamentos pendentes")
    void importarOfx_Sucesso() throws IOException {
        when(tenantResolverService.resolveCanonicalUuid(empresaUuid)).thenReturn(empresaUuid);
        when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);
        when(extratoLancamentoRepository.findByEmpresaIdAndFitid(anyString(), eq("FIT-001")))
                .thenReturn(Optional.empty());
        when(extratoLancamentoRepository.save(any(ExtratoLancamento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String ofx = """
                OFXHEADER:100
                DATA:OFXSGML
                <OFX>
                  <BANKMSGSRSV1>
                    <STMTTRNRS>
                      <STMTRS>
                        <BANKACCTFROM>
                          <BANKID>341</BANKID>
                          <ACCTID>9988-7</ACCTID>
                        </BANKACCTFROM>
                        <BANKTRANLIST>
                          <STMTTRN>
                            <TRNTYPE>CREDIT</TRNTYPE>
                            <DTPOSTED>20261101120000</DTPOSTED>
                            <TRNAMT>2500.00</TRNAMT>
                            <FITID>FIT-001</FITID>
                            <MEMO>PIX RECEBIDO CLIENTE ABC</MEMO>
                          </STMTTRN>
                        </BANKTRANLIST>
                      </STMTRS>
                    </STMTTRNRS>
                  </BANKMSGSRSV1>
                </OFX>
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "extrato.ofx", "application/x-ofx", ofx.getBytes(StandardCharsets.UTF_8)
        );

        ImportExtratoResponseDTO response = bankingService.importarOfx(file, empresaUuid);

        assertNotNull(response);
        assertTrue(response.isSucesso());
        assertEquals("341", response.getBanco());
        assertEquals("9988-7", response.getConta());
        assertEquals(1, response.getTotalLancamentos());
        assertEquals(1, response.getNovosLancamentos());
        assertEquals(0, response.getDuplicadosIgnorados());
        verify(extratoLancamentoRepository, times(1)).save(any(ExtratoLancamento.class));
    }

    @Test
    @DisplayName("Deve deduplicar transação OFX quando FITID já existir para a empresa")
    void importarOfx_DeduplicacaoPorFitid() throws IOException {
        when(tenantResolverService.resolveCanonicalUuid(empresaUuid)).thenReturn(empresaUuid);
        when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);

        ExtratoLancamento existente = new ExtratoLancamento();
        existente.setFitid("FIT-001");
        when(extratoLancamentoRepository.findByEmpresaIdAndFitid(empresaUuid, "FIT-001"))
                .thenReturn(Optional.of(existente));

        String ofx = """
                <OFX>
                  <BANKMSGSRSV1>
                    <STMTTRNRS>
                      <STMTRS>
                        <BANKACCTFROM>
                          <BANKID>341</BANKID>
                          <ACCTID>9988-7</ACCTID>
                        </BANKACCTFROM>
                        <BANKTRANLIST>
                          <STMTTRN>
                            <TRNTYPE>CREDIT</TRNTYPE>
                            <DTPOSTED>20261101120000</DTPOSTED>
                            <TRNAMT>2500.00</TRNAMT>
                            <FITID>FIT-001</FITID>
                            <MEMO>PIX RECEBIDO DUPLICADO</MEMO>
                          </STMTTRN>
                        </BANKTRANLIST>
                      </STMTRS>
                    </STMTTRNRS>
                  </BANKMSGSRSV1>
                </OFX>
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "extrato.ofx", "application/x-ofx", ofx.getBytes(StandardCharsets.UTF_8)
        );

        ImportExtratoResponseDTO response = bankingService.importarOfx(file, empresaUuid);

        assertNotNull(response);
        assertEquals(1, response.getTotalLancamentos());
        assertEquals(0, response.getNovosLancamentos());
        assertEquals(1, response.getDuplicadosIgnorados());
        verify(extratoLancamentoRepository, never()).save(any(ExtratoLancamento.class));
    }

    @Test
    @DisplayName("Deve importar arquivo CSV com sucesso")
    void importarCsv_Sucesso() throws IOException {
        when(tenantResolverService.resolveCanonicalUuid(empresaUuid)).thenReturn(empresaUuid);
        when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);
        when(extratoLancamentoRepository.findByEmpresaIdAndFitid(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(extratoLancamentoRepository.save(any(ExtratoLancamento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String csv = "data,descricao,valor,tipo,fitid\n" +
                "2026-11-01,Recebimento DOC,1750.80,CREDITO,FIT-CSV-999\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "extrato.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );

        ImportExtratoResponseDTO response = bankingService.importarCsv(file, empresaUuid);

        assertNotNull(response);
        assertTrue(response.isSucesso());
        assertEquals(1, response.getTotalLancamentos());
        assertEquals(1, response.getNovosLancamentos());
        assertEquals(0, response.getDuplicadosIgnorados());
        verify(extratoLancamentoRepository, times(1)).save(any(ExtratoLancamento.class));
    }

    @Test
    @DisplayName("Deve deduplicar transação CSV quando FITID já existir")
    void importarCsv_DeduplicacaoPorFitid() throws IOException {
        when(tenantResolverService.resolveCanonicalUuid(empresaUuid)).thenReturn(empresaUuid);
        when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);

        ExtratoLancamento existente = new ExtratoLancamento();
        existente.setFitid("FIT-CSV-DUP");
        when(extratoLancamentoRepository.findByEmpresaIdAndFitid(empresaUuid, "FIT-CSV-DUP"))
                .thenReturn(Optional.of(existente));

        String csv = "data,descricao,valor,tipo,fitid\n" +
                "2026-11-01,Recebimento DOC,1750.80,CREDITO,FIT-CSV-DUP\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "extrato.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );

        ImportExtratoResponseDTO response = bankingService.importarCsv(file, empresaUuid);

        assertNotNull(response);
        assertEquals(1, response.getTotalLancamentos());
        assertEquals(0, response.getNovosLancamentos());
        assertEquals(1, response.getDuplicadosIgnorados());
        verify(extratoLancamentoRepository, never()).save(any(ExtratoLancamento.class));
    }

    @Test
    @DisplayName("Deve conciliar com sucesso extrato e conta pendentes pertencentes à mesma empresa")
    void conciliar_Sucesso() {
        String extratoId = "ext-1";
        String contaId = "rec-1";
        String usuarioId = "usr-1";

        ExtratoLancamento extrato = new ExtratoLancamento();
        extrato.setId(extratoId);
        extrato.setEmpresaId(empresaUuid);
        extrato.setStatusConciliacao("PENDENTE");
        extrato.setValor(new BigDecimal("1200.00"));

        ContaReceber conta = new ContaReceber();
        conta.setId(contaId);
        conta.setEmpresaId(empresaUuid);
        conta.setStatus("PENDENTE");
        conta.setValorTotal(new BigDecimal("1200.00"));

        when(extratoLancamentoRepository.findById(extratoId)).thenReturn(Optional.of(extrato));
        when(contaReceberRepository.findById(contaId)).thenReturn(Optional.of(conta));
        when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);
        when(conciliacaoBancariaRepository.save(any(ConciliacaoBancaria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ConciliacaoBancaria result = bankingService.conciliar(extratoId, contaId, usuarioId, "Conciliação OK");

        assertNotNull(result);
        assertEquals(extratoId, result.getExtratoLancamentoId());
        assertEquals(contaId, result.getContaReceberId());
        assertEquals(usuarioId, result.getUsuarioId());
        assertEquals("Conciliação OK", result.getObservacao());

        assertEquals("CONCILIADO", extrato.getStatusConciliacao());
        assertEquals("PAGO", conta.getStatus());
        assertEquals(LocalDate.now(), conta.getDataPagamento());

        verify(extratoLancamentoRepository, times(1)).save(extrato);
        verify(contaReceberRepository, times(1)).save(conta);
        verify(conciliacaoBancariaRepository, times(1)).save(any(ConciliacaoBancaria.class));
    }

    @Test
    @DisplayName("Deve recusar conciliação se extrato e conta pertencerem a empresas distintas")
    void conciliar_EmpresasDiferentes_DeveLancarExcecao() {
        String extratoId = "ext-1";
        String contaId = "rec-1";

        ExtratoLancamento extrato = new ExtratoLancamento();
        extrato.setId(extratoId);
        extrato.setEmpresaId("empresa-A");
        extrato.setStatusConciliacao("PENDENTE");

        ContaReceber conta = new ContaReceber();
        conta.setId(contaId);
        conta.setEmpresaId("empresa-B");
        conta.setStatus("PENDENTE");

        Empresa empA = new Empresa();
        empA.setId(1L);

        Empresa empB = new Empresa();
        empB.setId(2L);

        when(extratoLancamentoRepository.findById(extratoId)).thenReturn(Optional.of(extrato));
        when(contaReceberRepository.findById(contaId)).thenReturn(Optional.of(conta));
        when(tenantResolverService.resolveEmpresa("empresa-A")).thenReturn(empA);
        when(tenantResolverService.resolveEmpresa("empresa-B")).thenReturn(empB);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bankingService.conciliar(extratoId, contaId, "usr", "teste"));

        assertTrue(ex.getMessage().contains("pertencem a empresas distintas"));
        verify(conciliacaoBancariaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve recusar conciliação se o extrato já estiver conciliado")
    void conciliar_ExtratoJaConciliado_DeveLancarExcecao() {
        String extratoId = "ext-1";
        String contaId = "rec-1";

        ExtratoLancamento extrato = new ExtratoLancamento();
        extrato.setId(extratoId);
        extrato.setEmpresaId(empresaUuid);
        extrato.setStatusConciliacao("CONCILIADO");

        ContaReceber conta = new ContaReceber();
        conta.setId(contaId);
        conta.setEmpresaId(empresaUuid);
        conta.setStatus("PENDENTE");

        when(extratoLancamentoRepository.findById(extratoId)).thenReturn(Optional.of(extrato));
        when(contaReceberRepository.findById(contaId)).thenReturn(Optional.of(conta));
        when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bankingService.conciliar(extratoId, contaId, "usr", "teste"));

        assertEquals("Este lançamento já está conciliado", ex.getMessage());
        verify(conciliacaoBancariaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve recusar conciliação se a conta já estiver paga")
    void conciliar_ContaJaPaga_DeveLancarExcecao() {
        String extratoId = "ext-1";
        String contaId = "rec-1";

        ExtratoLancamento extrato = new ExtratoLancamento();
        extrato.setId(extratoId);
        extrato.setEmpresaId(empresaUuid);
        extrato.setStatusConciliacao("PENDENTE");

        ContaReceber conta = new ContaReceber();
        conta.setId(contaId);
        conta.setEmpresaId(empresaUuid);
        conta.setStatus("PAGO");

        when(extratoLancamentoRepository.findById(extratoId)).thenReturn(Optional.of(extrato));
        when(contaReceberRepository.findById(contaId)).thenReturn(Optional.of(conta));
        when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bankingService.conciliar(extratoId, contaId, "usr", "teste"));

        assertEquals("Este título já está pago", ex.getMessage());
        verify(conciliacaoBancariaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve desconciliar restaurando status para PENDENTE e removendo a conciliação")
    void desconciliar_Sucesso() {
        String concId = "conc-1";
        String extratoId = "ext-1";
        String contaId = "rec-1";

        ConciliacaoBancaria conc = new ConciliacaoBancaria();
        conc.setId(concId);
        conc.setExtratoLancamentoId(extratoId);
        conc.setContaReceberId(contaId);

        ExtratoLancamento extrato = new ExtratoLancamento();
        extrato.setId(extratoId);
        extrato.setStatusConciliacao("CONCILIADO");

        ContaReceber conta = new ContaReceber();
        conta.setId(contaId);
        conta.setStatus("PAGO");
        conta.setDataPagamento(LocalDate.now());

        when(conciliacaoBancariaRepository.findById(concId)).thenReturn(Optional.of(conc));
        when(extratoLancamentoRepository.findById(extratoId)).thenReturn(Optional.of(extrato));
        when(contaReceberRepository.findById(contaId)).thenReturn(Optional.of(conta));

        bankingService.desconciliar(concId);

        assertEquals("PENDENTE", extrato.getStatusConciliacao());
        assertEquals("PENDENTE", conta.getStatus());
        assertNull(conta.getDataPagamento());

        verify(extratoLancamentoRepository, times(1)).save(extrato);
        verify(contaReceberRepository, times(1)).save(conta);
        verify(conciliacaoBancariaRepository, times(1)).delete(conc);
    }

    @Test
    @DisplayName("Deve listar lançamentos pendentes normalizando identificadores da empresa")
    void listarLancamentosPendentes_Sucesso() {
        when(tenantResolverService.resolveEmpresa(empresaUuid)).thenReturn(empresaMock);

        ExtratoLancamento l1 = new ExtratoLancamento();
        l1.setId("l-1");
        l1.setEmpresaId(empresaUuid);
        l1.setStatusConciliacao("PENDENTE");

        when(extratoLancamentoRepository.findByEmpresaIdAndStatusConciliacao(empresaUuid, "PENDENTE"))
                .thenReturn(List.of(l1));
        when(extratoLancamentoRepository.findByEmpresaIdAndStatusConciliacao("10", "PENDENTE"))
                .thenReturn(List.of());

        List<ExtratoLancamento> result = bankingService.listarLancamentosPendentes(empresaUuid);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("l-1", result.get(0).getId());
    }
}
