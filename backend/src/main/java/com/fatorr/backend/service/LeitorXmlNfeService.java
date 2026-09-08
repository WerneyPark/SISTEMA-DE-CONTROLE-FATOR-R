package com.fatorr.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service
public class LeitorXmlNfeService {

    public record DadosNotaFiscal(
            String chaveAcesso,
            String cnpjEmitente,
            String nomeEmitente,
            String cnpjTomador,
            String nomeTomador,
            BigDecimal valorTotal,
            LocalDate dataEmissao,
            LocalDate dataVencimento,
            String numeroDocumento) {
    }

    /**
     * Parseia um arquivo XML de NF-e para extrair informações relevantes (ex: Valor Total, CNPJ).
     * @param arquivoXml InputStream do arquivo XML
     */
    public void processarXmlNfe(InputStream arquivoXml) {
        lerXmlNfe(arquivoXml);
    }
    
    public void processarLote(MultipartFile[] arquivos) {
        if (arquivos == null) {
            return;
        }
        for (MultipartFile arquivo : arquivos) {
            try {
                lerXmlNfe(arquivo.getInputStream());
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Não foi possível processar o arquivo XML", e);
            }
        }
    }

    public DadosNotaFiscal lerXmlNfe(InputStream arquivoXml) {
        try {
            if (arquivoXml == null) {
            throw new IllegalArgumentException("O arquivo XML é obrigatório");
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(arquivoXml);

                    String cnpjEmitente = firstTextInContexts(document, new String[] {"CNPJ", "Cnpj"},
                "emit", "prestador", "prestadorServico");
                    String cnpjTomador = firstTextInContexts(document, new String[] {"CNPJ", "Cnpj"},
                "dest", "toma", "tomador", "tomadorServico");
                    String nomeEmitente = firstTextInContexts(document,
                        new String[] {"xNome", "RazaoSocial", "Nome"},
                "emit", "prestador", "prestadorServico");
                    String nomeTomador = firstTextInContexts(document,
                        new String[] {"xNome", "RazaoSocial", "Nome"},
                "dest", "toma", "tomador", "tomadorServico");
                String valor = firstTextInContexts(document,
                        new String[] {"vNF", "vServ", "vLiq", "valorServico", "ValorServicos",
                            "ValorLiquidoNfse", "ValorNfse"});
                String data = firstTextInContexts(document,
                        new String[] {"dhEmi", "dEmi", "dataEmissao", "DataEmissao", "competencia"});
                String vencimento = firstTextInContexts(document,
                    new String[] {"dVenc", "vencimento", "dataVencimento"});
            String chave = extractKey(document);
                String numero = firstTextInContexts(document,
                    new String[] {"nNF", "nNFS", "numero", "numeroNota"});

            if (cnpjEmitente == null || cnpjTomador == null || valor == null || data == null) {
                throw new IllegalArgumentException(
                        "XML não contém emitente, tomador, valor ou data da nota");
            }

            return new DadosNotaFiscal(
                    chave,
                    cnpjEmitente,
                    nomeEmitente,
                    cnpjTomador,
                    nomeTomador,
                    parseDecimal(valor),
                    parseDate(data),
                    vencimento == null ? parseDate(data) : parseDate(vencimento),
                    numero);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("XML fiscal inválido", e);
        }
    }

    private String extractKey(Document document) {
        NodeList nodes = document.getElementsByTagNameNS("*", "infNFe");
        if (nodes.getLength() > 0) {
            String id = ((Element) nodes.item(0)).getAttribute("Id");
            if (id != null && !id.isBlank()) {
                return id.startsWith("NFe") ? id.substring(3) : id;
            }
        }
        return firstText(document, "chNFe", "chave", "chaveAcesso");
    }

    private String firstTextInContexts(Document document, String[] valueNames,
            String... contextNames) {
        for (String valueName : valueNames) {
            NodeList values = document.getElementsByTagNameNS("*", valueName);
            for (int i = 0; i < values.getLength(); i++) {
                Node value = values.item(i);
                if (contextNames.length == 0 || hasAncestorName(value, contextNames)) {
                    String text = value.getTextContent().trim();
                    if (!text.isEmpty()) {
                        return text;
                    }
                }
            }
        }
        return null;
    }

    private String firstText(Document document, String valueName, String... parentNames) {
        NodeList values = document.getElementsByTagNameNS("*", valueName);
        for (int i = 0; i < values.getLength(); i++) {
            Node value = values.item(i);
            if (parentNames.length == 0 || hasAncestorName(value, parentNames)) {
                return value.getTextContent().trim();
            }
        }
        NodeList fallback = document.getElementsByTagName(valueName);
        return fallback.getLength() == 0 ? null : fallback.item(0).getTextContent().trim();
    }

    private boolean hasAncestorName(Node node, String... names) {
        Node current = node.getParentNode();
        while (current != null) {
            if (hasName(current, names)) {
                return true;
            }
            current = current.getParentNode();
        }
        return false;
    }

    private boolean hasName(Node node, String... names) {
        if (node == null) {
            return false;
        }
        String name = node.getLocalName() == null ? node.getNodeName() : node.getLocalName();
        for (String expected : names) {
            if (expected.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal parseDecimal(String value) {
        String normalized = value.trim().replace(".", "").replace(',', '.');
        if (value.indexOf('.') >= 0 && value.indexOf(',') < 0) {
            normalized = value.trim();
        }
        return new BigDecimal(normalized);
    }

    private LocalDate parseDate(String value) {
        String normalized = value.trim();
        try {
            return OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate();
            } catch (Exception ignoredAgain) {
                return LocalDate.parse(normalized.substring(0, 10));
            }
        }
    }
}
