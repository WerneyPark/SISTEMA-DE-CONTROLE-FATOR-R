package com.fatorr.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class LeitorXmlNfeServiceTest {

    private final LeitorXmlNfeService service = new LeitorXmlNfeService();

    @Test
    void deveLerNfeComNamespace() {
        String xml = """
                <nfeProc xmlns=\"http://www.portalfiscal.inf.br/nfe\">
                  <NFe><infNFe Id=\"NFe35260112345678000199550010000001234567890123\">
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
        assertEquals("98.765.432/0001-10", nota.cnpjTomador());
        assertEquals("1234.56", nota.valorTotal().toPlainString());
        assertEquals("2026-01-15", nota.dataEmissao().toString());
        assertEquals("123", nota.numeroDocumento());
    }

    @Test
    void deveLerNfseComEstruturaAninhada() {
        String xml = """
                <CompNfse xmlns=\"urn:nfse\">
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
    }

    @Test
    void deveRejeitarXmlInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.lerXmlNfe(stream("<nota>")));
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
                <!DOCTYPE nota [<!ENTITY arquivo SYSTEM \"file:///nao-deve-ser-lido\">]>
                <nota><emit><CNPJ>&arquivo;</CNPJ></emit></nota>
                """;

        assertThrows(IllegalArgumentException.class,
                () -> service.lerXmlNfe(stream(xml)));
    }

    private ByteArrayInputStream stream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}