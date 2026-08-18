package com.fatorr.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

@Service
public class LeitorXmlNfeService {

    /**
     * Parseia um arquivo XML de NF-e para extrair informações relevantes (ex: Valor Total, CNPJ).
     * @param arquivoXml InputStream do arquivo XML
     */
    public void processarXmlNfe(InputStream arquivoXml) {
        // TODO: Implementar lógica de parseamento (ex: usar JAXB ou Jackson XML)
        // 1. Validar estrutura do XML
        // 2. Extrair CNPJ do emissor
        // 3. Extrair Valor Total da NF-e
        // 4. Salvar / atualizar registro mensal correspondente
        throw new UnsupportedOperationException("Método de parseamento de NF-e ainda não implementado.");
    }
    
    public void processarLote(MultipartFile[] arquivos) {
        // TODO: Iterar sobre arquivos e processar cada um
        throw new UnsupportedOperationException("Processamento em lote ainda não implementado.");
    }
}
