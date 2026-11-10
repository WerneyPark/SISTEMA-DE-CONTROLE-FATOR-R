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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContaReceberService {

    public record ArquivoXmlInput(String nomeArquivo, byte[] conteudo) {}

    public record ItemPreviewXmlLote(
            String nomeArquivo,
            boolean valido,
            boolean periodoEncerrado,
            boolean jaImportada,
            String mensagemErro,
            LeitorXmlNfeService.DadosNotaFiscal dados
    ) {}

    public record ResultadoImportacaoXmlLote(
            int totalArquivos,
            int totalImportados,
            int totalIgnoradosOuErros,
            BigDecimal valorTotalImportado,
            List<ContaReceber> titulosGerados,
            List<String> mensagensErro
    ) {}

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

    public List<ItemPreviewXmlLote> previewXmlLote(
            String empresaId,
            List<ArquivoXmlInput> arquivos,
            LeitorXmlNfeService leitorXmlNfeService) {
        Empresa empresa = tenantResolverService.resolveEmpresa(empresaId);
        List<String> empresaIds = resolverEmpresaIds(empresaId);
        List<ItemPreviewXmlLote> resultado = new ArrayList<>();
        Set<String> chavesVistasNoLote = new HashSet<>();

        if (arquivos == null || arquivos.isEmpty()) {
            return resultado;
        }

        for (ArquivoXmlInput arq : arquivos) {
            String nomeArquivo = arq.nomeArquivo() != null ? arq.nomeArquivo() : "nota.xml";
            if (arq.conteudo() == null || arq.conteudo().length == 0) {
                resultado.add(new ItemPreviewXmlLote(nomeArquivo, false, false, false, "Arquivo XML vazio", null));
                continue;
            }

            try {
                LeitorXmlNfeService.DadosNotaFiscal nota = leitorXmlNfeService.lerXmlNfe(new ByteArrayInputStream(arq.conteudo()));

                String cnpjEmpresa = CnpjUtils.unmask(empresa.getCnpj());
                String cnpjEmitente = CnpjUtils.unmask(nota.cnpjEmitente());
                if (!CnpjUtils.isValid(cnpjEmpresa) || !CnpjUtils.isValid(cnpjEmitente) || !cnpjEmpresa.equals(cnpjEmitente)) {
                    resultado.add(new ItemPreviewXmlLote(nomeArquivo, false, false, false,
                            "O emitente do XML (" + (nota.cnpjEmitente() != null ? nota.cnpjEmitente() : "não inf.") + ") não pertence à empresa ativa (" + empresa.getCnpj() + ")",
                            nota));
                    continue;
                }

                boolean periodoEncerrado = false;
                if (nota.dataEmissao() != null) {
                    YearMonth competencia = YearMonth.from(nota.dataEmissao());
                    periodoEncerrado = encerramentoRepository.findByEmpresaIdAndCompetencia(empresa.getId(), competencia)
                            .map(EncerramentoCompetencia::isEncerrado).orElse(false);
                }

                if (periodoEncerrado) {
                    resultado.add(new ItemPreviewXmlLote(nomeArquivo, false, true, false,
                            "A data de emissão pertence a um período encerrado (" + YearMonth.from(nota.dataEmissao()) + ")",
                            nota));
                    continue;
                }

                boolean jaImportada = false;
                if (nota.chaveAcesso() != null && !nota.chaveAcesso().isBlank()) {
                    if (chavesVistasNoLote.contains(nota.chaveAcesso())) {
                        jaImportada = true;
                    } else if (repository.findByEmpresaIdInAndXmlChaveAcesso(empresaIds, nota.chaveAcesso()).isPresent()) {
                        jaImportada = true;
                    }
                }

                if (jaImportada) {
                    resultado.add(new ItemPreviewXmlLote(nomeArquivo, false, false, true,
                            "A nota fiscal já foi importada ou está duplicada no lote",
                            nota));
                    continue;
                }

                if (nota.chaveAcesso() != null && !nota.chaveAcesso().isBlank()) {
                    chavesVistasNoLote.add(nota.chaveAcesso());
                }

                resultado.add(new ItemPreviewXmlLote(nomeArquivo, true, false, false, null, nota));
            } catch (Exception e) {
                resultado.add(new ItemPreviewXmlLote(nomeArquivo, false, false, false,
                        e.getMessage() != null ? e.getMessage() : "Erro ao processar XML", null));
            }
        }

        return resultado;
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

        // Toda nota fiscal importada alimenta obrigatoriamente o Faturamento Mensal da competência
        if (faturamentoRepository != null && nota.dataEmissao() != null) {
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
        return importarXml(empresaId, arquivoXml, leitorXmlNfeService, true);
    }

    @Transactional
    public ResultadoImportacaoXmlLote importarXmlLote(
            String empresaId,
            List<ArquivoXmlInput> arquivos,
            LeitorXmlNfeService leitorXmlNfeService) {
        int totalArquivos = (arquivos != null) ? arquivos.size() : 0;
        int totalImportados = 0;
        int totalIgnoradosOuErros = 0;
        BigDecimal valorTotalImportado = BigDecimal.ZERO;
        List<ContaReceber> titulosGerados = new ArrayList<>();
        List<String> mensagensErro = new ArrayList<>();

        if (arquivos != null) {
            for (ArquivoXmlInput arq : arquivos) {
                String nome = arq.nomeArquivo() != null ? arq.nomeArquivo() : "nota.xml";
                try {
                    List<ContaReceber> titulos = importarXml(
                            empresaId,
                            new ByteArrayInputStream(arq.conteudo()),
                            leitorXmlNfeService,
                            true
                    );
                    totalImportados++;
                    titulosGerados.addAll(titulos);
                    for (ContaReceber cr : titulos) {
                        if (cr.getValorTotal() != null) {
                            valorTotalImportado = valorTotalImportado.add(cr.getValorTotal());
                        }
                    }
                } catch (Exception e) {
                    totalIgnoradosOuErros++;
                    mensagensErro.add(nome + ": " + (e.getMessage() != null ? e.getMessage() : "Erro ao importar"));
                }
            }
        }

        return new ResultadoImportacaoXmlLote(
                totalArquivos,
                totalImportados,
                totalIgnoradosOuErros,
                valorTotalImportado,
                titulosGerados,
                mensagensErro
        );
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
        if (temCliente && temPeriodo) {
            return repository.findByEmpresaIdInAndClienteIdAndDataVencimentoBetween(
                    empresaIds, clienteId, inicio, fim, pageable);
        }
        if (temStatus) {
            return repository.findByEmpresaIdInAndStatus(empresaIds, status, pageable);
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
        ContaReceber existente = buscarPorId(empresaId, id);

        if ("CANCELADO".equals(existente.getStatus())) {
            throw new IllegalStateException("Não é possível alterar uma conta cancelada");
        }
        if ("PAGO".equals(existente.getStatus())) {
            throw new IllegalStateException("Não é possível alterar uma conta já paga");
        }

        validarConta(dados);

        existente.setClienteId(dados.getClienteId());
        existente.setNumeroDocumento(dados.getNumeroDocumento());
        existente.setValorTotal(dados.getValorTotal());
        existente.setDataEmissao(dados.getDataEmissao());
        existente.setDataVencimento(dados.getDataVencimento());
        existente.setObservacao(dados.getObservacao());
        existente.setUpdatedAt(LocalDateTime.now());

        return repository.save(existente);
    }

    public void cancelar(String empresaId, String id) {
        ContaReceber conta = buscarPorId(empresaId, id);

        if ("PAGO".equals(conta.getStatus())) {
            throw new IllegalStateException("Não é possível cancelar uma conta já recebida");
        }

        conta.setStatus("CANCELADO");
        conta.setUpdatedAt(LocalDateTime.now());
        repository.save(conta);
    }

    @Transactional
    public ContaReceber cancelarNf(String empresaId, String id) {
        ContaReceber conta = buscarPorId(empresaId, id);
        Empresa empresa = tenantResolverService.resolveEmpresa(empresaId);

        if ("CANCELADO".equals(conta.getStatus())) {
            throw new IllegalStateException("Esta nota fiscal já está cancelada");
        }
        if ("PAGO".equals(conta.getStatus())) {
            throw new IllegalStateException("Não é possível cancelar uma nota fiscal que já possui parcelas recebidas");
        }

        // Subtrai o valor do FaturamentoMensal se existir
        if (faturamentoRepository != null && conta.getDataEmissao() != null) {
            YearMonth competencia = YearMonth.from(conta.getDataEmissao());
            List<FaturamentoMensal> existentes = faturamentoRepository.findByEmpresaIdAndCompetencia(empresa.getId(), competencia);
            if (!existentes.isEmpty()) {
                FaturamentoMensal fat = existentes.get(0);
                BigDecimal novoValor = fat.getValor().subtract(conta.getValorTotal());
                fat.setValor(novoValor.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : novoValor);
                faturamentoRepository.save(fat);
            }
        }

        // Cancela todas as parcelas originadas da mesma NF (mesma chave de acesso)
        if (conta.getXmlChaveAcesso() != null && !conta.getXmlChaveAcesso().isBlank()) {
            List<String> empresaIds = resolverEmpresaIds(empresaId);
            List<ContaReceber> todasDaNf = repository.findByEmpresaIdIn(empresaIds, Pageable.unpaged()).getContent()
                    .stream()
                    .filter(c -> conta.getXmlChaveAcesso().equals(c.getXmlChaveAcesso()))
                    .toList();

            for (ContaReceber c : todasDaNf) {
                c.setStatus("CANCELADO");
                c.setUpdatedAt(LocalDateTime.now());
                repository.save(c);
            }
        } else {
            conta.setStatus("CANCELADO");
            conta.setUpdatedAt(LocalDateTime.now());
            repository.save(conta);
        }

        return conta;
    }

    public ContaReceber baixar(String empresaId, String id) {
        ContaReceber conta = buscarPorId(empresaId, id);

        if ("PAGO".equals(conta.getStatus())) {
            throw new IllegalStateException("Conta já está paga");
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
