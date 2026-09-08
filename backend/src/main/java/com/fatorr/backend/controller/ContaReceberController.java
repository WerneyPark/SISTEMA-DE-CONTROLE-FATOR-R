package com.fatorr.backend.controller;

import com.fatorr.backend.domain.ContaReceber;
import com.fatorr.backend.service.ContaReceberService;
import com.fatorr.backend.service.LeitorXmlNfeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

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

    @PostMapping("/importar-xml")
    public ResponseEntity<ContaReceber> importarXml(
            @RequestParam String empresaId,
            @RequestParam("arquivo") MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new IllegalArgumentException("O arquivo XML é obrigatório");
        }
        try {
            return ResponseEntity.ok(service.importarXml(
                    empresaId, arquivo.getInputStream(), leitorXmlNfeService));
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o arquivo XML", e);
        }
    }

    @GetMapping
    public Page<ContaReceber> listar(
            @RequestParam String empresaId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String clienteId,
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fim,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new IllegalArgumentException("Paginação inválida");
        }
        Pageable pageable = PageRequest.of(
                pagina, tamanho, Sort.by(Sort.Direction.ASC, "dataVencimento"));
        return service.listar(empresaId, status, clienteId, inicio, fim, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaReceber> buscar(
            @PathVariable String id, @RequestParam String empresaId) {
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
            @PathVariable String id, @RequestParam String empresaId) {
        return ResponseEntity.ok(service.baixar(empresaId, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(
            @PathVariable String id, @RequestParam String empresaId) {
        service.cancelar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}