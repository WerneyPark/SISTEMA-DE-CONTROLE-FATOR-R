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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContaReceberService {

    private final ContaReceberRepository repository;
    private final ClienteFornecedorRepository clienteRepository;
    private final TenantResolverService tenantResolverService;
    private final FaturamentoMensalRepository faturamentoRepository;
    private final EncerramentoCompetenciaRepository encerramentoRepository;

    public ContaReceber criar(ContaReceber conta) {
        throw new IllegalStateException("Não é permitido criar contas a receber manualmente. Todo o contas a receber é gerado exclusivamente por importação de XML.");
    }

    public LeitorXmlNfeService.DadosNotaFiscal previewXml(
            String empresaId,
            InputStream arquivoXml,
            LeitorXmlNfeService leitorXmlNfeService) {
        Empresa empresa = tenantResolverService.resolveEmpresa(empresaId);
        LeitorXmlNfeService.DadosNotaFiscal nota = leitorXmlNfeService.lerXmlNfe(arquivoXml);

        validarEmitente(empresa, nota);

        boolean encerrado = false;
        String aviso = null;
        if (nota.dataEmissao() != null) {
            YearMonth competencia = YearMonth.from(nota.dataEmissao());
            encerrado = encerramentoRepository.findByEmpresaIdAndCompetencia(empresa.getId(), competencia)
                    .map(EncerramentoCompetencia::isEncerrado).orElse(false);
            if (encerrado) {
                aviso = "Atenção: A data de emissão desta nota fiscal (" + competencia + ") pertence a um período já encerrado. Abra o período no Simulador do Fator R para que o valor seja inserido corretamente.";
            }
        }

        return new LeitorXmlNfeService.DadosNotaFiscal(
                nota.chaveAcesso(),
                nota.cnpjEmitente(),
                nota.nomeEmitente(),
                nota.cnpjTomador(),
                nota.nomeTomador(),
                nota.valorTotal(),
                nota.dataEmissao(),
                nota.dataVencimento(),
                nota.numeroDocumento(),
                nota.parcelas(),
                encerrado,
                aviso
        );
    }

    @Transactional
    public List<ContaReceber> importarXml(
            String empresaId,
            InputStream arquivoXml,
            LeitorXmlNfeService leitorXmlNfeService,
            boolean atualizarFaturamento) {
        Empresa empresa = tenantResolverService.resolveEmpresa(empresaId);
        String canonicalEmpresaId = tenantResolverService.resolveCanonicalUuid(empresaId);
        List<String> empresaIds = resolverEmpresaIds(empresaId);

        LeitorXmlNfeService.DadosNotaFiscal nota = leitorXmlNfeService.lerXmlNfe(arquivoXml);
        validarEmitente(empresa, nota);

        if (nota.dataEmissao() != null) {
            YearMonth competencia = YearMonth.from(nota.dataEmissao());
            boolean encerrado = encerramentoRepository.findByEmpresaIdAndCompetencia(empresa.getId(), competencia)
                    .map(EncerramentoCompetencia::isEncerrado).orElse(false);
            if (encerrado) {
                throw new IllegalStateException("A nota fiscal pertence a um período já encerrado (" + competencia + "). Abra o período no Simulador do Fator R para que o valor seja inserido corretamente.");
            }
        }

        if (nota.chaveAcesso() != null && !nota.chaveAcesso().isBlank()
                && repository.findByEmpresaIdInAndXmlChaveAcesso(empresaIds, nota.chaveAcesso()).isPresent()) {
            throw new IllegalArgumentException("A nota fiscal já foi importada");
        }

        String cnpjTomador = CnpjUtils.unmask(nota.cnpjTomador());
        ClienteFornecedor cliente = clienteRepository
                .findByEmpresaIdAndCnpjCpf(canonicalEmpresaId, cnpjTomador)
                .or(() -> {
                    if (empresa.getUuidId() != null) {
                        return clienteRepository.findByEmpresaIdAndCnpjCpf(String.valueOf(empresa.getId()), cnpjTomador);
                    }
                    return java.util.Optional.empty();
                })
                .orElseGet(() -> {
                    String nome = (nota.nomeTomador() != null && !nota.nomeTomador().isBlank())
                            ? nota.nomeTomador().trim()
                            : cnpjTomador;
                    ClienteFornecedor novo = new ClienteFornecedor();
                    novo.setId(UUID.randomUUID().toString());
                    novo.setEmpresaId(canonicalEmpresaId);
                    novo.setNome(nome);
                    novo.setCnpjCpf(cnpjTomador);
                    novo.setTipo("CLIENTE");
                    novo.setCreatedAt(LocalDateTime.now());
                    return clienteRepository.save(novo);
                });

        List<LeitorXmlNfeService.ParcelaNotaFiscal> parcelas = nota.parcelas();
        if (parcelas == null || parcelas.isEmpty()) {
            LocalDate vencPadrao = nota.dataVencimento() != null ? nota.dataVencimento() : nota.dataEmissao();
            parcelas = List.of(new LeitorXmlNfeService.ParcelaNotaFiscal(
                    nota.numeroDocumento() != null ? nota.numeroDocumento() : "1",
                    vencPadrao,
                    nota.valorTotal()));
        }

        int totalParcelas = parcelas.size();
        List<ContaReceber> titulos = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();

        for (LeitorXmlNfeService.ParcelaNotaFiscal parcela : parcelas) {
            ContaReceber conta = new ContaReceber();
            conta.setId(UUID.randomUUID().toString());
            conta.setEmpresaId(canonicalEmpresaId);
            conta.setClienteId(cliente.getId());

            String numDoc = nota.numeroDocumento();
            if (totalParcelas > 1) {
                numDoc = (numDoc != null && !numDoc.isBlank())
                        ? numDoc + "/" + parcela.numero()
                        : parcela.numero();
            }
            conta.setNumeroDocumento(numDoc);
            conta.setValorTotal(parcela.valor());
            conta.setDataEmissao(nota.dataEmissao());
            conta.setDataVencimento(parcela.dataVencimento() != null ? parcela.dataVencimento() : nota.dataEmissao());
            conta.setStatus("PENDENTE");
            conta.setOrigem("IMPORTACAO_XML");
            conta.setXmlChaveAcesso(nota.chaveAcesso());
            conta.setCreatedAt(agora);
            conta.setUpdatedAt(agora);

            titulos.add(repository.save(conta));
        }

        if (atualizarFaturamento && faturamentoRepository != null && nota.dataEmissao() != null) {
            YearMonth competencia = YearMonth.from(nota.dataEmissao());
            List<FaturamentoMensal> existentes = faturamentoRepository.findByEmpresaIdAndCompetencia(empresa.getId(), competencia);
            if (!existentes.isEmpty()) {
                FaturamentoMensal faturamento = existentes.get(0);
                faturamento.setValor(faturamento.getValor().add(nota.valorTotal()));
                faturamentoRepository.save(faturamento);
            } else {
                FaturamentoMensal faturamento = new FaturamentoMensal();
                faturamento.setEmpresa(empresa);
                faturamento.setCompetencia(competencia);
                faturamento.setValor(nota.valorTotal());
                faturamentoRepository.save(faturamento);
            }
        }

        return titulos;
    }

    @Transactional
    public List<ContaReceber> importarXml(
            String empresaId,
            InputStream arquivoXml,
            LeitorXmlNfeService leitorXmlNfeService) {
        return importarXml(empresaId, arquivoXml, leitorXmlNfeService, false);
    }

    public ContaReceber buscarPorId(String empresaId, String id) {
        ContaReceber conta = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta a receber não encontrada"));

        validarEmpresa(conta, empresaId);
        return conta;
    }

    public Page<ContaReceber> listar(String empresaId, String status,
            String clienteId, LocalDate inicio, LocalDate fim, Pageable pageable) {
        validarPeriodo(inicio, fim);
        List<String> empresaIds = resolverEmpresaIds(empresaId);

        boolean temStatus = status != null && !status.isBlank();
        boolean temCliente = clienteId != null && !clienteId.isBlank();
        boolean temPeriodo = inicio != null && fim != null;

        if (temStatus && temCliente && temPeriodo) {
            return repository.findByEmpresaIdInAndStatusAndClienteIdAndDataVencimentoBetween(
                    empresaIds, status, clienteId, inicio, fim, pageable);
        }
        if (temStatus && temCliente) {
            return repository.findByEmpresaIdInAndStatusAndClienteId(
                    empresaIds, status, clienteId, pageable);
        }
        if (temStatus && temPeriodo) {
            return repository.findByEmpresaIdInAndStatusAndDataVencimentoBetween(
                    empresaIds, status, inicio, fim, pageable);
        }
        if (temStatus) {
            return repository.findByEmpresaIdInAndStatus(empresaIds, status, pageable);
        }
        if (temCliente && temPeriodo) {
            return repository.findByEmpresaIdInAndClienteIdAndDataVencimentoBetween(
                    empresaIds, clienteId, inicio, fim, pageable);
        }
        if (temCliente) {
            return repository.findByEmpresaIdInAndClienteId(empresaIds, clienteId, pageable);
        }
        if (temPeriodo) {
            return repository.findByEmpresaIdInAndDataVencimentoBetween(
                    empresaIds, inicio, fim, pageable);
        }
        return repository.findByEmpresaIdIn(empresaIds, pageable);
    }

    public ContaReceber atualizar(String empresaId, String id, ContaReceber dados) {
        ContaReceber conta = buscarPorId(empresaId, id);
        if (!"PENDENTE".equals(conta.getStatus())) {
            throw new IllegalStateException("Somente contas pendentes podem ser alteradas");
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

    @Transactional
    public ContaReceber cancelarNf(String empresaId, String id) {
        ContaReceber conta = buscarPorId(empresaId, id);
        if ("PAGO".equals(conta.getStatus())) {
            throw new IllegalStateException("Não é possível cancelar uma nota fiscal que já possui pagamento registrado.");
        }
        if ("NF_CANCELADA".equals(conta.getStatus())) {
            throw new IllegalStateException("Esta nota fiscal já está cancelada.");
        }

        Empresa empresa = tenantResolverService.resolveEmpresa(empresaId);
        if (conta.getDataEmissao() != null) {
            YearMonth competencia = YearMonth.from(conta.getDataEmissao());
            boolean encerrado = encerramentoRepository.findByEmpresaIdAndCompetencia(empresa.getId(), competencia)
                    .map(EncerramentoCompetencia::isEncerrado).orElse(false);
            if (encerrado) {
                throw new IllegalStateException("Não é possível cancelar uma nota fiscal de um período já encerrado (" + competencia + "). Reabra o período no Simulador do Fator R primeiro.");
            }

            // Abate do faturamento mensal da competência de emissão
            List<FaturamentoMensal> existentes = faturamentoRepository.findByEmpresaIdAndCompetencia(empresa.getId(), competencia);
            if (!existentes.isEmpty()) {
                FaturamentoMensal fat = existentes.get(0);
                BigDecimal novoValor = fat.getValor().subtract(conta.getValorTotal());
                if (novoValor.compareTo(BigDecimal.ZERO) < 0) {
                    novoValor = BigDecimal.ZERO;
                }
                fat.setValor(novoValor);
                faturamentoRepository.save(fat);
            }
        }

        conta.setStatus("NF_CANCELADA");
        conta.setUpdatedAt(LocalDateTime.now());
        return repository.save(conta);
    }

    public ContaReceber baixar(String empresaId, String id) {
        ContaReceber conta = buscarPorId(empresaId, id);

        if ("PAGO".equals(conta.getStatus())) {
            throw new IllegalStateException("A conta já está paga");
        }

        if ("CANCELADO".equals(conta.getStatus())) {
            throw new IllegalStateException("Não é possível baixar uma conta cancelada");
        }

        conta.setStatus("PAGO");
        conta.setDataPagamento(LocalDate.now());
        conta.setUpdatedAt(LocalDateTime.now());

        return repository.save(conta);
    }

    public List<ClienteFornecedor> listarClientes(String empresaId) {
        Empresa empresa = tenantResolverService.resolveEmpresa(empresaId);
        String canonicalEmpresaId = tenantResolverService.resolveCanonicalUuid(empresaId);
        List<ClienteFornecedor> clientes = clienteRepository.findByEmpresaId(canonicalEmpresaId);
        if (clientes.isEmpty() && empresa.getUuidId() != null) {
            clientes = clienteRepository.findByEmpresaId(String.valueOf(empresa.getId()));
        }
        return clientes;
    }

    private void validarEmitente(Empresa empresa, LeitorXmlNfeService.DadosNotaFiscal nota) {
        String cnpjEmpresa = CnpjUtils.unmask(empresa.getCnpj());
        String cnpjEmitente = CnpjUtils.unmask(nota.cnpjEmitente());
        if (!CnpjUtils.isValid(cnpjEmpresa) || !CnpjUtils.isValid(cnpjEmitente)
                || !cnpjEmpresa.equals(cnpjEmitente)) {
            throw new IllegalArgumentException("O emitente do XML não pertence à empresa ativa");
        }
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
            throw new IllegalArgumentException("O valor deve ser maior que zero");
        }

        if (conta.getDataEmissao() == null || conta.getDataVencimento() == null) {
            throw new IllegalArgumentException("As datas de emissão e vencimento são obrigatórias");
        }

        if (conta.getDataVencimento().isBefore(conta.getDataEmissao())) {
            throw new IllegalArgumentException("O vencimento não pode ser anterior à emissão");
        }
    }

    private void validarEmpresa(ContaReceber conta, String empresaId) {
        List<String> empresaIds = resolverEmpresaIds(empresaId);
        if (!empresaIds.contains(conta.getEmpresaId())) {
            throw new IllegalArgumentException("A conta não pertence à empresa ativa");
        }
    }

    private List<String> resolverEmpresaIds(String empresaId) {
        Empresa empresa = tenantResolverService.resolveEmpresa(empresaId);
        List<String> ids = new ArrayList<>();
        ids.add(empresaId);
        if (empresa.getUuidId() != null && !ids.contains(empresa.getUuidId())) {
            ids.add(empresa.getUuidId());
        }
        String numId = String.valueOf(empresa.getId());
        if (!ids.contains(numId)) {
            ids.add(numId);
        }
        return ids;
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if ((inicio == null) != (fim == null)) {
            throw new IllegalArgumentException("Informe as datas inicial e final do período");
        }
        if (inicio != null && fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial");
        }
    }
}
