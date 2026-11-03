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
    public ResponseEntity<ContaReceber> atualizar(
            @PathVariable String id,
            @RequestParam String empresaId,
            @RequestBody ContaReceber conta) {
        return ResponseEntity.ok(service.atualizar(empresaId, id, conta));
    }

    @PostMapping("/{id}/baixar")
    public ResponseEntity<ContaReceber> baixar(
            @PathVariable String id,
            @RequestParam String empresaId) {
        return ResponseEntity.ok(service.baixar(empresaId, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(
            @PathVariable String id,
            @RequestParam String empresaId) {
        service.cancelar(empresaId, id);
        return ResponseEntity.noContent().build();
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

    @PostMapping("/importar-xml")
    public ResponseEntity<List<ContaReceber>> importarXml(
            @RequestParam String empresaId,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(name = "atualizarFaturamento", required = false, defaultValue = "false") boolean atualizarFaturamento) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("O arquivo XML é obrigatório");
        }
        try {
            return ResponseEntity.ok(service.importarXml(
                    empresaId, arquivo.getInputStream(), leitorXmlNfeService, atualizarFaturamento));
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o arquivo XML", e);
        }
    }

    @GetMapping("/clientes")
    public ResponseEntity<List<ClienteFornecedor>> listarClientes(@RequestParam String empresaId) {
        return ResponseEntity.ok(service.listarClientes(empresaId));
    }
}
