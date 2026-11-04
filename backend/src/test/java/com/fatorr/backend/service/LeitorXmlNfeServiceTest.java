package com.fatorr.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class LeitorXmlNfeServiceTest {

    private final LeitorXmlNfeService service = new LeitorXmlNfeService();

    @Test
    void deveLerNfeComNamespace() {
        String xml = """
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
                  <NFe><infNFe Id="NFe35260112345678000199550010000001234567890123">
                    <ide><nNF>123</nNF><dhEmi>2026-01-15T10:30:00-03:00</dhEmi></ide>
                    <emit><CNPJ>12.345.678/0001-99</CNPJ><xNome>Prestador NF-e</xNome></emit>
                    <dest><CNPJ>98.765.432/0001-10</CNPJ><xNome>Cliente NF-e</xNome></dest>
                    <total><ICMSTot><vNF>1234.56</vNF></ICMSTot></total>
                  </infNFe></NFe>
                </nfeProc>
                """;

        LeitorXmlNfeService.DadosNotaFiscal nota = service.lerXmlNfe(stream(xml));

        assertEquals("35260112345678000199550010000001234567890123", nota.chaveAcesso());
        assertEquals("12.345.678/0001-99", nota.cnpjEmitente());
        assertEquals("Prestador NF-e", nota.nomeEmitente());
        assertEquals("98.765.432/0001-10", nota.cnpjTomador());
        assertEquals("Cliente NF-e", nota.nomeTomador());
        assertEquals("1234.56", nota.valorTotal().toPlainString());
        assertEquals("2026-01-15", nota.dataEmissao().toString());
        assertEquals("123", nota.numeroDocumento());
        assertEquals(1, nota.parcelas().size());
        assertEquals("1234.56", nota.parcelas().get(0).valor().toPlainString());
    }

    @Test
    void deveLerNfseComEstruturaAninhada() {
        String xml = """
                <CompNfse xmlns="urn:nfse">
                  <Nfse><InfNfse>
                    <Numero>45</Numero><DataEmissao>2026-02-10</DataEmissao>
                    <PrestadorServico><IdentificacaoPrestador><Cnpj>ABC12345678901</Cnpj></IdentificacaoPrestador>
                      <RazaoSocial>Prestador NFS-e</RazaoSocial></PrestadorServico>
                    <TomadorServico><IdentificacaoTomador><CpfCnpj><Cnpj>12345678000199</Cnpj></CpfCnpj></IdentificacaoTomador>
                      <RazaoSocial>Cliente NFS-e</RazaoSocial></TomadorServico>
                    <Servico><Valores><ValorServicos>987,65</ValorServicos></Valores></Servico>
                  </InfNfse></Nfse>
                </CompNfse>
                """;

        LeitorXmlNfeService.DadosNotaFiscal nota = service.lerXmlNfe(stream(xml));

        assertEquals("ABC12345678901", nota.cnpjEmitente());
        assertEquals("12345678000199", nota.cnpjTomador());
        assertEquals("Prestador NFS-e", nota.nomeEmitente());
        assertEquals("Cliente NFS-e", nota.nomeTomador());
        assertEquals("987.65", nota.valorTotal().toPlainString());
        assertEquals("2026-02-10", nota.dataVencimento().toString());
        assertEquals(1, nota.parcelas().size());
    }

    @Test
    void deveRejeitarXmlInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.lerXmlNfe(stream("<nota>invalida sem fechamento")));
    }

    @Test
    void deveRejeitarXmlSemEmitente() {
        String xml = """
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
                  <NFe><infNFe>
                    <dest><CNPJ>98765432000110</CNPJ></dest>
                    <total><ICMSTot><vNF>123.45</vNF></ICMSTot></total>
                    <ide><dhEmi>2026-01-15T10:30:00-03:00</dhEmi></ide>
                  </infNFe></NFe>
                </nfeProc>
                """;

        assertThrows(IllegalArgumentException.class,
                () -> service.lerXmlNfe(stream(xml)));
    }

    @Test
    void deveExtrairEmitenteComOuSemMascara() {
        String xml = """
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
                  <NFe><infNFe>
                    <ide><dhEmi>2026-01-15T10:30:00-03:00</dhEmi></ide>
                    <emit><CNPJ>12.345.678/0001-99</CNPJ></emit>
                    <dest><CNPJ>98765432000110</CNPJ></dest>
                    <total><ICMSTot><vNF>123.45</vNF></ICMSTot></total>
                  </infNFe></NFe>
                </nfeProc>
                """;

        assertEquals("12.345.678/0001-99",
                service.lerXmlNfe(stream(xml)).cnpjEmitente());
    }

    @Test
    void deveBloquearEntidadesExternas() {
        String xml = """
                <!DOCTYPE nota [<!ENTITY arquivo SYSTEM "file:///nao-deve-ser-lido">]>
                <nota><emit><CNPJ>&arquivo;</CNPJ></emit></nota>
                """;

        assertThrows(IllegalArgumentException.class,
                () -> service.lerXmlNfe(stream(xml)));
    }

    @Test
    void deveExtrairMultiplasParcelas() {
        String xml = """
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
                  <NFe><infNFe Id="NFe35260112345678000199550010000001234567890123">
                    <ide><nNF>500</nNF><dhEmi>2026-03-01T08:00:00-03:00</dhEmi></ide>
                    <emit><CNPJ>12345678000199</CNPJ><xNome>Empresa Prestadora</xNome></emit>
                    <dest><CNPJ>98765432000110</CNPJ><xNome>Cliente Tomador</xNome></dest>
                    <total><ICMSTot><vNF>3000.00</vNF></ICMSTot></total>
                    <cobr>
                      <dup><nDup>001</nDup><dVenc>2026-04-01</dVenc><vDup>1000.00</vDup></dup>
                      <dup><nDup>002</nDup><dVenc>2026-05-01</dVenc><vDup>1000.00</vDup></dup>
                      <dup><nDup>003</nDup><dVenc>2026-06-01</dVenc><vDup>1000.00</vDup></dup>
                    </cobr>
                  </infNFe></NFe>
                </nfeProc>
                """;

        LeitorXmlNfeService.DadosNotaFiscal nota = service.lerXmlNfe(stream(xml));

        assertNotNull(nota.parcelas());
        assertEquals(3, nota.parcelas().size());

        assertEquals("001", nota.parcelas().get(0).numero());
        assertEquals("2026-04-01", nota.parcelas().get(0).dataVencimento().toString());
        assertEquals("1000.00", nota.parcelas().get(0).valor().toPlainString());

        assertEquals("002", nota.parcelas().get(1).numero());
        assertEquals("2026-05-01", nota.parcelas().get(1).dataVencimento().toString());
        assertEquals("1000.00", nota.parcelas().get(1).valor().toPlainString());

        assertEquals("003", nota.parcelas().get(2).numero());
        assertEquals("2026-06-01", nota.parcelas().get(2).dataVencimento().toString());
        assertEquals("1000.00", nota.parcelas().get(2).valor().toPlainString());
    }

    @Test
    void deveExecutarPreviewSemPersistir() {
        String xml = """
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
                  <NFe><infNFe Id="NFe35260112345678000199550010000001234567890123">
                    <ide><nNF>789</nNF><dhEmi>2026-04-10T14:00:00-03:00</dhEmi></ide>
                    <emit><CNPJ>12345678000199</CNPJ><xNome>Empresa Preview</xNome></emit>
                    <dest><CNPJ>ABC98765432101</CNPJ><xNome>Tomador Preview</xNome></dest>
                    <total><ICMSTot><vNF>5432.10</vNF></ICMSTot></total>
                  </infNFe></NFe>
                </nfeProc>
                """;

        LeitorXmlNfeService.DadosNotaFiscal preview = service.previewXml(stream(xml));

        assertNotNull(preview);
        assertEquals("789", preview.numeroDocumento());
        assertEquals("ABC98765432101", preview.cnpjTomador());
        assertEquals("5432.10", preview.valorTotal().toPlainString());
        assertFalse(preview.parcelas().isEmpty());
    }

    @Test
    void deveLerNfsePadraoNacionalCompleta() {
        String xml = """
                <NFSe xmlns="http://www.sped.fazenda.gov.br/nfse">
                  <infNFSe Id="NFSe31062001100005392612628000000000000019010153978386">
                    <nNFSe>456</nNFSe>
                    <cVerif>AB12-CD34</cVerif>
                    <emit>
                      <CNPJ>12345678000199</CNPJ>
                      <xNome>Prestador Nacional SA</xNome>
                    </emit>
                    <DPS>
                      <infDPS>
                        <ide>
                          <dhEmi>2026-05-20T10:00:00-03:00</dhEmi>
                        </ide>
                        <toma>
                          <CNPJ>98765432000110</CNPJ>
                          <xNome>Tomador Nacional Ltda</xNome>
                        </toma>
                        <valores>
                          <vServPrest>
                            <vServ>2500.00</vServ>
                          </vServPrest>
                        </valores>
                      </infDPS>
                    </DPS>
                  </infNFSe>
                </NFSe>
                """;

        LeitorXmlNfeService.DadosNotaFiscal nota = service.lerXmlNfe(stream(xml));

        assertEquals("31062001100005392612628000000000000019010153978386", nota.chaveAcesso());
        assertEquals("12345678000199", nota.cnpjEmitente());
        assertEquals("Prestador Nacional SA", nota.nomeEmitente());
        assertEquals("98765432000110", nota.cnpjTomador());
        assertEquals("Tomador Nacional Ltda", nota.nomeTomador());
        assertEquals("2500.00", nota.valorTotal().toPlainString());
        assertEquals("2026-05-20", nota.dataEmissao().toString());
        assertEquals("456", nota.numeroDocumento());
        assertEquals(1, nota.parcelas().size());
        assertEquals("2500.00", nota.parcelas().get(0).valor().toPlainString());
    }

    @Test
    void deveLerNfsePadraoNacionalApenasComBlocoPrest() {
        String xml = """
                <NFSe xmlns="http://www.sped.fazenda.gov.br/nfse">
                  <infNFSe Id="ID31062000000000000000000000000000000000000000000001">
                    <nNFSe>999</nNFSe>
                    <DPS>
                      <infDPS>
                        <ide>
                          <dhEmi>2026-06-15T09:00:00-03:00</dhEmi>
                        </ide>
                        <prest>
                          <CNPJ>12345678000199</CNPJ>
                          <xNome>Prestador Apenas Prest</xNome>
                        </prest>
                        <toma>
                          <CNPJ>98765432000110</CNPJ>
                          <razaoSocial>Tomador Razao Social</razaoSocial>
                        </toma>
                        <valores>
                          <vServPrest>
                            <vServ>1800.50</vServ>
                          </vServPrest>
                        </valores>
                      </infDPS>
                    </DPS>
                  </infNFSe>
                </NFSe>
                """;

        LeitorXmlNfeService.DadosNotaFiscal nota = service.lerXmlNfe(stream(xml));

        assertEquals("31062000000000000000000000000000000000000000000001", nota.chaveAcesso());
        assertEquals("12345678000199", nota.cnpjEmitente());
        assertEquals("Prestador Apenas Prest", nota.nomeEmitente());
        assertEquals("98765432000110", nota.cnpjTomador());
        assertEquals("Tomador Razao Social", nota.nomeTomador());
        assertEquals("1800.50", nota.valorTotal().toPlainString());
        assertEquals("2026-06-15", nota.dataEmissao().toString());
        assertEquals("999", nota.numeroDocumento());
        assertEquals(1, nota.parcelas().size());
        assertEquals("1800.50", nota.parcelas().get(0).valor().toPlainString());
    }

    @Test
    void deveLerArquivosGeradosParaTestes() throws Exception {
        java.io.File pastaFat = new java.io.File("../arquivos para testes/faturamento/01 - Inovar Softwares/2026/05");
        if (pastaFat.exists()) {
            java.io.File[] files = pastaFat.listFiles((dir, name) -> name.endsWith(".xml"));
            assertNotNull(files);
            for (java.io.File f : files) {
                try (java.io.InputStream is = new java.io.FileInputStream(f)) {
                    LeitorXmlNfeService.DadosNotaFiscal nota = service.lerXmlNfe(is);
                    assertNotNull(nota.cnpjEmitente());
                    assertNotNull(nota.cnpjTomador());
                    assertNotNull(nota.valorTotal());
                    assertNotNull(nota.dataEmissao());
                    assertFalse(nota.parcelas().isEmpty());
                }
            }
        }
    }

    private ByteArrayInputStream stream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}
