package com.fatorr.backend.service;

import com.fatorr.backend.domain.ContaReceber;
import com.fatorr.backend.domain.ClienteFornecedor;
import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.repository.ClienteFornecedorRepository;
import com.fatorr.backend.repository.ContaReceberRepository;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.util.CnpjUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContaReceberService {

    private final ContaReceberRepository repository;
    private final ClienteFornecedorRepository clienteRepository;
    private final EmpresaRepository empresaRepository;

    public ContaReceber criar(ContaReceber conta) {
        validarConta(conta);

        conta.setId(UUID.randomUUID().toString());
        conta.setStatus("PENDENTE");
        conta.setOrigem("MANUAL");
        conta.setCreatedAt(LocalDateTime.now());
        conta.setUpdatedAt(LocalDateTime.now());

        return repository.save(conta);
    }

    @Transactional
    public ContaReceber importarXml(String empresaId, InputStream arquivoXml,
            LeitorXmlNfeService leitorXmlNfeService) {
        Empresa empresa = localizarEmpresa(empresaId);
        LeitorXmlNfeService.DadosNotaFiscal nota = leitorXmlNfeService.lerXmlNfe(arquivoXml);

        String cnpjEmpresa = CnpjUtils.unmask(empresa.getCnpj());
        String cnpjEmitente = CnpjUtils.unmask(nota.cnpjEmitente());
        if (!CnpjUtils.isValid(cnpjEmpresa) || !CnpjUtils.isValid(cnpjEmitente)
            || !cnpjEmpresa.equals(cnpjEmitente)) {
            throw new IllegalArgumentException(
                    "O emitente do XML não pertence à empresa ativa");
        }
        if (nota.chaveAcesso() != null
                && repository.findByEmpresaIdAndXmlChaveAcesso(
                        empresaId, nota.chaveAcesso()).isPresent()) {
            throw new IllegalArgumentException("A nota fiscal já foi importada");
        }

        String cnpjTomador = CnpjUtils.unmask(nota.cnpjTomador());
        ClienteFornecedor cliente = clienteRepository
                .findByEmpresaIdAndCnpjCpf(empresaId, cnpjTomador)
                .orElseGet(() -> clienteRepository.save(new ClienteFornecedor(
                        UUID.randomUUID().toString(), empresaId,
                nota.nomeTomador() == null || nota.nomeTomador().isBlank()
                    ? cnpjTomador : nota.nomeTomador(),
                cnpjTomador, "CLIENTE", LocalDateTime.now())));

        ContaReceber conta = new ContaReceber();
        conta.setEmpresaId(empresaId);
        conta.setClienteId(cliente.getId());
        conta.setNumeroDocumento(nota.numeroDocumento());
        conta.setValorTotal(nota.valorTotal());
        conta.setDataEmissao(nota.dataEmissao());
        conta.setDataVencimento(nota.dataVencimento());
        conta.setStatus("PENDENTE");
        conta.setOrigem("IMPORTACAO_XML");
        conta.setXmlChaveAcesso(nota.chaveAcesso());
        conta.setId(UUID.randomUUID().toString());
        conta.setCreatedAt(LocalDateTime.now());
        conta.setUpdatedAt(LocalDateTime.now());
        return repository.save(conta);
    }

    public ContaReceber buscarPorId(String empresaId, String id) {
        ContaReceber conta = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conta a receber não encontrada"));

        validarEmpresa(conta, empresaId);
        return conta;
    }

    public Page<ContaReceber> listar(String empresaId, String status,
            String clienteId, LocalDate inicio, LocalDate fim, Pageable pageable) {
        validarPeriodo(inicio, fim);

        boolean temStatus = status != null && !status.isBlank();
        boolean temCliente = clienteId != null && !clienteId.isBlank();
        boolean temPeriodo = inicio != null && fim != null;

        if (temStatus && temCliente && temPeriodo) {
            return repository.findByEmpresaIdAndStatusAndClienteIdAndDataVencimentoBetween(
                    empresaId, status, clienteId, inicio, fim, pageable);
        }
        if (temStatus && temPeriodo) {
            return repository.findByEmpresaIdAndStatusAndDataVencimentoBetween(
                    empresaId, status, inicio, fim, pageable);
        }
        if (temStatus) {
            return repository.findByEmpresaIdAndStatus(empresaId, status, pageable);
        }
        if (temCliente && temPeriodo) {
            return repository.findByEmpresaIdAndClienteIdAndDataVencimentoBetween(
                    empresaId, clienteId, inicio, fim, pageable);
        }
        if (temCliente) {
            return repository.findByEmpresaIdAndClienteId(empresaId, clienteId, pageable);
        }
        if (temPeriodo) {
            return repository.findByEmpresaIdAndDataVencimentoBetween(
                    empresaId, inicio, fim, pageable);
        }
        return repository.findByEmpresaId(empresaId, pageable);
    }

    public ContaReceber atualizar(String empresaId, String id, ContaReceber dados) {
        ContaReceber conta = buscarPorId(empresaId, id);
        if (!"PENDENTE".equals(conta.getStatus())) {
            throw new IllegalStateException(
                    "Somente contas pendentes podem ser alteradas");
        }

        dados.setEmpresaId(conta.getEmpresaId());
        dados.setId(conta.getId());
        validarConta(dados);
        conta.setClienteId(dados.getClienteId());
        conta.setNumeroDocumento(dados.getNumeroDocumento());
        conta.setValorTotal(dados.getValorTotal());
        conta.setDataEmissao(dados.getDataEmissao());
        conta.setDataVencimento(dados.getDataVencimento());
        conta.setUpdatedAt(LocalDateTime.now());
        return repository.save(conta);
    }

    public void cancelar(String empresaId, String id) {
        ContaReceber conta = buscarPorId(empresaId, id);
        if ("PAGO".equals(conta.getStatus())) {
            throw new IllegalStateException("Uma conta paga não pode ser cancelada");
        }
        conta.setStatus("CANCELADO");
        conta.setUpdatedAt(LocalDateTime.now());
        repository.save(conta);
    }

    public ContaReceber baixar(String empresaId, String id) {
        ContaReceber conta = buscarPorId(empresaId, id);

        if ("PAGO".equals(conta.getStatus())) {
            throw new IllegalStateException("A conta já está paga");
        }

        if ("CANCELADO".equals(conta.getStatus())) {
            throw new IllegalStateException(
                    "Não é possível baixar uma conta cancelada");
        }

        conta.setStatus("PAGO");
        conta.setDataPagamento(LocalDate.now());
        conta.setUpdatedAt(LocalDateTime.now());

        return repository.save(conta);
    }

    private void validarConta(ContaReceber conta) {
        if (conta.getEmpresaId() == null || conta.getEmpresaId().isBlank()) {
            throw new IllegalArgumentException("A empresa é obrigatória");
        }

        if (conta.getClienteId() == null || conta.getClienteId().isBlank()) {
            throw new IllegalArgumentException("O cliente é obrigatório");
        }

        if (conta.getValorTotal() == null
                || conta.getValorTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "O valor deve ser maior que zero");
        }

        if (conta.getDataEmissao() == null
                || conta.getDataVencimento() == null) {
            throw new IllegalArgumentException(
                    "As datas de emissão e vencimento são obrigatórias");
        }

        if (conta.getDataVencimento()
                .isBefore(conta.getDataEmissao())) {
            throw new IllegalArgumentException(
                    "O vencimento não pode ser anterior à emissão");
        }
    }

    private void validarEmpresa(ContaReceber conta, String empresaId) {
        if (!conta.getEmpresaId().equals(empresaId)) {
            throw new IllegalArgumentException(
                    "A conta não pertence à empresa ativa");
        }
    }

    private Empresa localizarEmpresa(String empresaId) {
        return empresaRepository.findByUuidId(empresaId)
                .orElseGet(() -> {
                    try {
                        return empresaRepository.findById(Long.valueOf(empresaId))
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Empresa ativa não encontrada"));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Empresa ativa não encontrada", e);
                    }
                });
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if ((inicio == null) != (fim == null)) {
            throw new IllegalArgumentException(
                    "Informe as datas inicial e final do período");
        }
        if (inicio != null && fim.isBefore(inicio)) {
            throw new IllegalArgumentException(
                    "A data final não pode ser anterior à data inicial");
        }
    }
}