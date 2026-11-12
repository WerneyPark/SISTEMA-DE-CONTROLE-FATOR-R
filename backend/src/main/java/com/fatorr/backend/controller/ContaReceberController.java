package com.fatorr.backend.controller;

import com.fatorr.backend.domain.ClienteFornecedor;
import com.fatorr.backend.domain.ContaReceber;
import com.fatorr.backend.service.ContaReceberService;
import com.fatorr.backend.service.LeitorXmlNfeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/contas-a-receber")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContaReceberController {

    private final ContaReceberService service;
    private final LeitorXmlNfeService leitorXmlNfeService;

    @PostMapping
    public ResponseEntity<ContaReceber> criar(@RequestBody ContaReceber conta) {
        return ResponseEntity.ok(service.criar(conta));
    }

    @GetMapping
    public Page<ContaReceber> listar(
            @RequestParam String empresaId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String clienteId,
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fim,
            @RequestParam(name = "pagina", required = false) Integer pagina,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "tamanho", required = false) Integer tamanho,
            @RequestParam(name = "size", required = false) Integer size,
            Pageable pageable) {

        int pageNum = pagina != null ? pagina : (page != null ? page : (pageable != null ? pageable.getPageNumber() : 0));
        int pageSize = tamanho != null ? tamanho : (size != null ? size : (pageable != null ? pageable.getPageSize() : 20));

        if (pageNum < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Paginação inválida");
        }

        Pageable resolvedPageable = PageRequest.of(
                pageNum, pageSize, Sort.by(Sort.Direction.ASC, "dataVencimento"));

        return service.listar(empresaId, status, clienteId, inicio, fim, resolvedPageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaReceber> buscar(
            @PathVariable String id,
            @RequestParam String empresaId) {
        return ResponseEntity.ok(service.buscarPorId(empresaId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(
            @PathVariable String id,
            @RequestParam String empresaId,
            @RequestBody ContaReceber conta) {
        try {
            return ResponseEntity.ok(service.atualizar(empresaId, id, conta));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", e.getMessage(), "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/baixar")
    public ResponseEntity<?> baixar(
            @PathVariable String id,
            @RequestParam String empresaId) {
        try {
            return ResponseEntity.ok(service.baixar(empresaId, id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", e.getMessage(), "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelar(
            @PathVariable String id,
            @RequestParam String empresaId) {
        try {
            service.cancelar(empresaId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", e.getMessage(), "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancelar-nf")
    public ResponseEntity<?> cancelarNf(
            @PathVariable String id,
            @RequestParam String empresaId) {
        try {
            return ResponseEntity.ok(service.cancelarNf(empresaId, id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", e.getMessage(), "message", e.getMessage()));
        }
    }

    @PostMapping("/preview-xml")
    public ResponseEntity<LeitorXmlNfeService.DadosNotaFiscal> previewXml(
            @RequestParam String empresaId,
            @RequestParam("arquivo") MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("O arquivo XML é obrigatório");
        }
        try {
            return ResponseEntity.ok(service.previewXml(empresaId, arquivo.getInputStream(), leitorXmlNfeService));
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o arquivo XML", e);
        }
    }

    @PostMapping("/preview-xml-lote")
    public ResponseEntity<List<ContaReceberService.ItemPreviewXmlLote>> previewXmlLote(
            @RequestParam String empresaId,
            @RequestParam("arquivos") MultipartFile[] arquivos) {
        if (arquivos == null || arquivos.length == 0) {
            throw new IllegalArgumentException("Nenhum arquivo XML foi enviado");
        }
        List<ContaReceberService.ArquivoXmlInput> inputs = new ArrayList<>();
        for (MultipartFile arq : arquivos) {
            try {
                inputs.add(new ContaReceberService.ArquivoXmlInput(arq.getOriginalFilename(), arq.getBytes()));
            } catch (IOException e) {
                inputs.add(new ContaReceberService.ArquivoXmlInput(arq.getOriginalFilename(), new byte[0]));
            }
        }
        return ResponseEntity.ok(service.previewXmlLote(empresaId, inputs, leitorXmlNfeService));
    }

    @PostMapping("/importar-xml")
    public ResponseEntity<List<ContaReceber>> importarXml(
            @RequestParam String empresaId,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(name = "atualizarFaturamento", required = false, defaultValue = "true") boolean atualizarFaturamento) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("O arquivo XML é obrigatório");
        }
        try {
            return ResponseEntity.ok(service.importarXml(
                    empresaId, arquivo.getInputStream(), leitorXmlNfeService, true));
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o arquivo XML", e);
        }
    }

    @PostMapping("/importar-xml-lote")
    public ResponseEntity<ContaReceberService.ResultadoImportacaoXmlLote> importarXmlLote(
            @RequestParam String empresaId,
            @RequestParam("arquivos") MultipartFile[] arquivos) {
        if (arquivos == null || arquivos.length == 0) {
            throw new IllegalArgumentException("Nenhum arquivo XML foi enviado");
        }
        List<ContaReceberService.ArquivoXmlInput> inputs = new ArrayList<>();
        for (MultipartFile arq : arquivos) {
            try {
                inputs.add(new ContaReceberService.ArquivoXmlInput(arq.getOriginalFilename(), arq.getBytes()));
            } catch (IOException e) {
                throw new IllegalArgumentException("Falha ao ler arquivo: " + arq.getOriginalFilename(), e);
            }
        }
        return ResponseEntity.ok(service.importarXmlLote(empresaId, inputs, leitorXmlNfeService));
    }

    @GetMapping("/clientes")
    public ResponseEntity<List<ClienteFornecedor>> listarClientes(@RequestParam String empresaId) {
        return ResponseEntity.ok(service.listarClientes(empresaId));
    }
}
