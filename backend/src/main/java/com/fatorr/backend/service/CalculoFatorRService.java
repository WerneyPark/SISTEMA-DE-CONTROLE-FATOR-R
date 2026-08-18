package com.fatorr.backend.service;

import com.fatorr.backend.domain.Empresa;
import com.fatorr.backend.domain.FaixaTributaria;
import com.fatorr.backend.domain.FaturamentoMensal;
import com.fatorr.backend.domain.FolhaPagamentoMensal;
import com.fatorr.backend.domain.ParametroTributario;
import com.fatorr.backend.dto.FatorRResultadoDTO;
import com.fatorr.backend.dto.SimulacaoTributariaDTO;
import com.fatorr.backend.dto.FaturamentoHistoricoDTO;
import com.fatorr.backend.dto.ProjecaoCenarioDTO;
import com.fatorr.backend.repository.EmpresaRepository;
import com.fatorr.backend.repository.FaturamentoMensalRepository;
import com.fatorr.backend.repository.FolhaPagamentoMensalRepository;
import com.fatorr.backend.repository.ParametroTributarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalculoFatorRService {

    private final EmpresaRepository empresaRepository;
    private final FaturamentoMensalRepository faturamentoRepository;
    private final FolhaPagamentoMensalRepository folhaRepository;
    private final ParametroTributarioRepository parametroRepository;

    // Limites para alerta de proximidade (entre 25% e 31%)
    private static final BigDecimal LIMITE_FATOR_R = new BigDecimal("0.28");
    private static final BigDecimal LIMITE_INFERIOR_ALERTA = new BigDecimal("0.25");
    private static final BigDecimal LIMITE_SUPERIOR_ALERTA = new BigDecimal("0.31");

    private ParametroTributario getParametros(Long empresaId) {
        return parametroRepository.findByAtivoTrueAndEmpresaId(empresaId)
                .orElseGet(() -> parametroRepository.findByAtivoTrueAndEmpresaIsNull()
                .orElseGet(() -> {
                    // Fallback parameters if none exist in DB
                    ParametroTributario p = new ParametroTributario();
                    p.setAliquotaPis(new BigDecimal("0.0065"));
                    p.setAliquotaCofins(new BigDecimal("0.0300"));
                    p.setAliquotaIss(new BigDecimal("0.0500"));
                    p.setAliquotaIrpj(new BigDecimal("0.1500"));
                    p.setAliquotaAdicionalIrpj(new BigDecimal("0.1000"));
                    p.setAliquotaCsll(new BigDecimal("0.0900"));
                    p.setFaixas(new java.util.ArrayList<>());
                    return p;
                }));
    }

    public FatorRResultadoDTO calcularFatorR(Long empresaId, YearMonth competenciaReferencia) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));

        YearMonth dataFim = competenciaReferencia.minusMonths(1);
        YearMonth dataInicio = competenciaReferencia.minusMonths(12);

        List<FaturamentoMensal> faturamentos = faturamentoRepository.findUltimos12Meses(empresaId, dataInicio, dataFim);
        List<FolhaPagamentoMensal> folhas = folhaRepository.findUltimos12Meses(empresaId, dataInicio, dataFim);

        BigDecimal totalFaturamento = faturamentos.stream().map(FaturamentoMensal::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFolha = folhas.stream().map(FolhaPagamentoMensal::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fatorR = BigDecimal.ZERO;
        String anexo;
        String mensagem;

        if (totalFaturamento.compareTo(BigDecimal.ZERO) == 0) {
            anexo = "Anexo V";
            mensagem = "Faturamento total dos últimos 12 meses é zero. Não é possível calcular a proporção, assumindo Anexo V por padrão.";
        } else {
            fatorR = totalFolha.divide(totalFaturamento, 4, RoundingMode.HALF_UP);
            if (fatorR.compareTo(LIMITE_FATOR_R) >= 0) {
                anexo = "Anexo III";
                mensagem = "Fator R igual ou superior a 28%. A empresa será tributada pelo Anexo III.";
            } else {
                anexo = "Anexo V";
                mensagem = "Fator R inferior a 28%. A empresa será tributada pelo Anexo V.";
            }
        }

        return FatorRResultadoDTO.builder()
                .empresaId(empresa.getId())
                .razaoSocial(empresa.getRazaoSocial())
                .competenciaReferencia(competenciaReferencia)
                .totalFaturamento12Meses(totalFaturamento)
                .totalFolha12Meses(totalFolha)
                .percentualFatorR(fatorR.multiply(new BigDecimal("100")))
                .anexoAplicavel(anexo)
                .mensagem(mensagem)
                .build();
    }

    public SimulacaoTributariaDTO simularTributacao(Long empresaId, YearMonth competenciaReferencia) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada com ID: " + empresaId));

        YearMonth dataFim = competenciaReferencia.minusMonths(1);
        YearMonth dataInicio = competenciaReferencia.minusMonths(12);

        List<FaturamentoMensal> faturamentos = faturamentoRepository.findUltimos12Meses(empresaId, dataInicio, dataFim);
        List<FolhaPagamentoMensal> folhas = folhaRepository.findUltimos12Meses(empresaId, dataInicio, dataFim);

        BigDecimal totalFaturamento = faturamentos.stream().map(FaturamentoMensal::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFolha = folhas.stream().map(FolhaPagamentoMensal::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fatorR = BigDecimal.ZERO;
        String anexo;
        String mensagem;

        if (totalFaturamento.compareTo(BigDecimal.ZERO) == 0) {
            anexo = "Anexo V";
            mensagem = "Faturamento total dos últimos 12 meses é zero. Assumindo Anexo V por padrão.";
        } else {
            fatorR = totalFolha.divide(totalFaturamento, 4, RoundingMode.HALF_UP);
            if (fatorR.compareTo(LIMITE_FATOR_R) >= 0) {
                anexo = "Anexo III";
                mensagem = "Fator R igual ou superior a 28%. Tributação pelo Anexo III (mais vantajoso).";
            } else {
                anexo = "Anexo V";
                mensagem = "Fator R inferior a 28%. Tributação pelo Anexo V.";
            }
        }

        boolean alertaProximidade = false;
        String mensagemAlerta = null;

        if (fatorR.compareTo(LIMITE_INFERIOR_ALERTA) >= 0 && fatorR.compareTo(LIMITE_FATOR_R) < 0) {
            alertaProximidade = true;
            mensagemAlerta = String.format(
                    "ATENÇÃO: O Fator R está em %.2f%%, próximo ao limite de 28%%. Um pequeno aumento na folha de pagamento pode fazer a empresa migrar para o Anexo III.",
                    fatorR.multiply(new BigDecimal("100")));
        } else if (fatorR.compareTo(LIMITE_FATOR_R) >= 0 && fatorR.compareTo(LIMITE_SUPERIOR_ALERTA) <= 0) {
            alertaProximidade = true;
            mensagemAlerta = String.format(
                    "ATENÇÃO: O Fator R está em %.2f%%, pouco acima do limite de 28%%. Se a folha de pagamento diminuir, a empresa pode voltar ao Anexo V.",
                    fatorR.multiply(new BigDecimal("100")));
        }

        ParametroTributario params = getParametros(empresaId);

        // 1. Calcula os Meses de Atividade
        long mesesAtividade = 12;
        if (empresa.getDataInicioAtividade() != null) {
            long diff = java.time.temporal.ChronoUnit.MONTHS.between(
                empresa.getDataInicioAtividade().withDayOfMonth(1),
                competenciaReferencia.atDay(1)
            );
            if (diff >= 1 && diff < 12) {
                mesesAtividade = diff;
            }
        }

        // 2. Calcula RBT12 (Proporcional ou Total)
        BigDecimal rbt12 = totalFaturamento;
        if (mesesAtividade < 12) {
            rbt12 = totalFaturamento.divide(new BigDecimal(mesesAtividade), 4, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("12"));
        }

        // 3. Encontra a Faixa correspondente
        final String anexoBusca = "Anexo III".equals(anexo) ? "III" : "V";
        final BigDecimal rbt12Final = rbt12;
        final String anexoBuscaExtenso = "Anexo " + anexoBusca;
        FaixaTributaria faixa = params.getFaixas().stream()
                .filter(f -> f.getAnexo().equals(anexoBusca) || f.getAnexo().equals(anexoBuscaExtenso))
                .filter(f -> f.getLimiteSuperior() == null || rbt12Final.compareTo(f.getLimiteSuperior()) <= 0)
                .min(java.util.Comparator.comparing(FaixaTributaria::getNumeroFaixa))
                .orElse(null);

        BigDecimal aliquotaEfetivaSN = BigDecimal.ZERO;
        String mensagemFaixa6 = null;

        if (faixa != null) {
            if (rbt12.compareTo(BigDecimal.ZERO) == 0) {
                aliquotaEfetivaSN = faixa.getAliquotaNominal();
            } else {
                BigDecimal impostoPrevisto = rbt12.multiply(faixa.getAliquotaNominal()).subtract(faixa.getValorDeduzir());
                if (impostoPrevisto.compareTo(BigDecimal.ZERO) < 0) impostoPrevisto = BigDecimal.ZERO;
                aliquotaEfetivaSN = impostoPrevisto.divide(rbt12, 4, RoundingMode.HALF_UP);
            }
            
            if (faixa.getNumeroFaixa() == 6) {
                mensagemFaixa6 = "ATENÇÃO: Empresa atingiu a 6ª Faixa. O ICMS e ISS devem ser recolhidos separadamente em guias próprias, fora do DAS. O valor calculado aqui refere-se apenas aos tributos federais.";
            }
        }

        List<FaturamentoMensal> faturamentoMesAtualList = faturamentoRepository.findByEmpresaIdAndCompetencia(empresaId, competenciaReferencia);
        BigDecimal faturamentoMesAtual = faturamentoMesAtualList.stream()
                .map(FaturamentoMensal::getValor)
                .reduce(BigDecimal::add)
                .orElse(rbt12.compareTo(BigDecimal.ZERO) > 0 ? rbt12.divide(new BigDecimal("12"), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        List<FolhaPagamentoMensal> folhaMesAtualList = folhaRepository.findByEmpresaIdAndCompetencia(empresaId, competenciaReferencia);
        BigDecimal folhaMesAtual = folhaMesAtualList.stream()
                .map(FolhaPagamentoMensal::getValor)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        List<FaturamentoHistoricoDTO> historico = new java.util.ArrayList<>();
        for (int i = 12; i >= 1; i--) {
            YearMonth mesHist = competenciaReferencia.minusMonths(i);
            BigDecimal fValor = faturamentos.stream().filter(f -> f.getCompetencia().equals(mesHist)).map(FaturamentoMensal::getValor).findFirst().orElse(BigDecimal.ZERO);
            BigDecimal folhaValor = folhas.stream().filter(f -> f.getCompetencia().equals(mesHist)).map(FolhaPagamentoMensal::getValor).findFirst().orElse(BigDecimal.ZERO);
            historico.add(new FaturamentoHistoricoDTO(mesHist, fValor, folhaValor));
        }
        historico.add(new FaturamentoHistoricoDTO(competenciaReferencia, faturamentoMesAtual, folhaMesAtual));

        BigDecimal valorSN = faturamentoMesAtual.multiply(aliquotaEfetivaSN).setScale(2, RoundingMode.HALF_UP);
        
        // --- Cálculo do Lucro Presumido ---
        BigDecimal pis = faturamentoMesAtual.multiply(params.getAliquotaPis());
        BigDecimal cofins = faturamentoMesAtual.multiply(params.getAliquotaCofins());
        BigDecimal iss = faturamentoMesAtual.multiply(params.getAliquotaIss());
        
        BigDecimal basePresumida = faturamentoMesAtual.multiply(new BigDecimal("0.32"));
        BigDecimal irpj = basePresumida.multiply(params.getAliquotaIrpj());
        BigDecimal csll = basePresumida.multiply(params.getAliquotaCsll());
        
        BigDecimal adicionalIrpj = BigDecimal.ZERO;
        if (basePresumida.compareTo(new BigDecimal("20000.00")) > 0) {
            adicionalIrpj = basePresumida.subtract(new BigDecimal("20000.00")).multiply(params.getAliquotaAdicionalIrpj());
        }
        
        BigDecimal valorLP = pis.add(cofins).add(iss).add(irpj).add(csll).add(adicionalIrpj).setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal aliquotaEfetivaLP = faturamentoMesAtual.compareTo(BigDecimal.ZERO) > 0 
                ? valorLP.divide(faturamentoMesAtual, 4, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;
        
        BigDecimal economia = valorLP.subtract(valorSN).abs();
        String regimeMaisVantajoso = valorSN.compareTo(valorLP) <= 0 ? "Simples Nacional" : "Lucro Presumido";

        return SimulacaoTributariaDTO.builder()
                .empresaId(empresa.getId())
                .razaoSocial(empresa.getRazaoSocial())
                .cnpj(empresa.getCnpj())
                .competenciaReferencia(competenciaReferencia)
                .mesesComDados(faturamentos.size())
                .totalFaturamento12Meses(totalFaturamento)
                .totalFolha12Meses(totalFolha)
                .rbt12(rbt12)
                .faixaEncontrada(faixa != null ? faixa.getNumeroFaixa() : null)
                .mensagemFaixa6(mensagemFaixa6)
                .percentualFatorR(fatorR.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP))
                .anexoAplicavel(anexo)
                .alertaProximidadeLimite(alertaProximidade)
                .mensagemAlerta(mensagemAlerta)
                .aliquotaEfetivaSimplesNacional(aliquotaEfetivaSN.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP))
                .valorEstimadoSimplesNacional(valorSN)
                .aliquotaEfetivaLucroPresumido(aliquotaEfetivaLP.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP))
                .valorEstimadoLucroPresumido(valorLP)
                .valorPis(pis.setScale(2, RoundingMode.HALF_UP))
                .valorCofins(cofins.setScale(2, RoundingMode.HALF_UP))
                .valorIss(iss.setScale(2, RoundingMode.HALF_UP))
                .valorIrpj(irpj.setScale(2, RoundingMode.HALF_UP))
                .valorCsll(csll.setScale(2, RoundingMode.HALF_UP))
                .valorAdicionalIrpj(adicionalIrpj.setScale(2, RoundingMode.HALF_UP))
                .economiaEstimada(economia)
                .regimeMaisVantajoso(regimeMaisVantajoso)
                .mensagem(mensagem)
                .historicoFaturamentos(historico)
                .build();
    }

    public List<ProjecaoCenarioDTO> simularProximosMeses(Long empresaId, YearMonth competenciaReferencia) {
        SimulacaoTributariaDTO simulacaoAtual = simularTributacao(empresaId, competenciaReferencia);
        
        List<FaturamentoMensal> faturamentoBaseList = faturamentoRepository.findByEmpresaIdAndCompetencia(empresaId, competenciaReferencia);
        BigDecimal faturamentoBase = faturamentoBaseList.stream()
                .map(FaturamentoMensal::getValor)
                .reduce(BigDecimal::add)
                .orElse(simulacaoAtual.getRbt12().compareTo(BigDecimal.ZERO) > 0 ? simulacaoAtual.getRbt12().divide(new BigDecimal("12"), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                
        List<FolhaPagamentoMensal> folhaBaseList = folhaRepository.findByEmpresaIdAndCompetencia(empresaId, competenciaReferencia);
        BigDecimal folhaBase = folhaBaseList.stream()
                .map(FolhaPagamentoMensal::getValor)
                .reduce(BigDecimal::add)
                .orElse(simulacaoAtual.getTotalFolha12Meses().compareTo(BigDecimal.ZERO) > 0 ? simulacaoAtual.getTotalFolha12Meses().divide(new BigDecimal("12"), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        List<ProjecaoCenarioDTO> cenarios = new java.util.ArrayList<>();

        // Cenario 1: +15%
        cenarios.add(gerarCenario(empresaId, competenciaReferencia, simulacaoAtual, faturamentoBase, folhaBase, "Cenário Otimista", "Aumento de 15% no faturamento", new BigDecimal("1.15")));
        
        // Cenario 2: =0%
        cenarios.add(gerarCenario(empresaId, competenciaReferencia, simulacaoAtual, faturamentoBase, folhaBase, "Cenário Neutro", "Manutenção do faturamento atual", new BigDecimal("1.00")));
        
        // Cenario 3: -15%
        cenarios.add(gerarCenario(empresaId, competenciaReferencia, simulacaoAtual, faturamentoBase, folhaBase, "Cenário Pessimista", "Redução de 15% no faturamento", new BigDecimal("0.85")));

        return cenarios;
    }

    private ProjecaoCenarioDTO gerarCenario(Long empresaId, YearMonth competenciaAtual, SimulacaoTributariaDTO simulacaoAtual, BigDecimal faturamentoBase, BigDecimal folhaBase, String nome, String descricao, BigDecimal multiplicador) {
        ParametroTributario params = getParametros(empresaId);
        List<ProjecaoCenarioDTO.MesProjecaoDTO> meses = new java.util.ArrayList<>();
        
        BigDecimal faturamentoMes1 = faturamentoBase.multiply(multiplicador).setScale(2, RoundingMode.HALF_UP);
        BigDecimal folhaMes1 = folhaBase;
        
        BigDecimal novoRbt12Mes1 = simulacaoAtual.getRbt12()
                .subtract(simulacaoAtual.getHistoricoFaturamentos().get(0).getFaturamento())
                .add(faturamentoMes1);
        
        BigDecimal novoTotalFolhaMes1 = simulacaoAtual.getTotalFolha12Meses()
                .subtract(simulacaoAtual.getHistoricoFaturamentos().get(0).getFolhaPagamento())
                .add(folhaMes1);
        
        meses.add(calcularMesProjecao(competenciaAtual.plusMonths(1), faturamentoMes1, folhaMes1, novoRbt12Mes1, novoTotalFolhaMes1, params));
        
        BigDecimal faturamentoMes2 = faturamentoMes1;
        BigDecimal folhaMes2 = folhaBase;
        
        BigDecimal novoRbt12Mes2 = novoRbt12Mes1
                .subtract(simulacaoAtual.getHistoricoFaturamentos().get(1).getFaturamento())
                .add(faturamentoMes2);
                
        BigDecimal novoTotalFolhaMes2 = novoTotalFolhaMes1
                .subtract(simulacaoAtual.getHistoricoFaturamentos().get(1).getFolhaPagamento())
                .add(folhaMes2);
                
        meses.add(calcularMesProjecao(competenciaAtual.plusMonths(2), faturamentoMes2, folhaMes2, novoRbt12Mes2, novoTotalFolhaMes2, params));

        return ProjecaoCenarioDTO.builder()
                .nomeCenario(nome)
                .descricao(descricao)
                .meses(meses)
                .build();
    }
    
    private ProjecaoCenarioDTO.MesProjecaoDTO calcularMesProjecao(YearMonth competencia, BigDecimal faturamentoEstimado, BigDecimal folhaEstimada, BigDecimal rbt12Estimado, BigDecimal folha12Estimado, ParametroTributario params) {
        BigDecimal fatorR = BigDecimal.ZERO;
        String anexo = "Anexo V";
        if (rbt12Estimado.compareTo(BigDecimal.ZERO) > 0) {
            fatorR = folha12Estimado.divide(rbt12Estimado, 4, RoundingMode.HALF_UP);
            if (fatorR.compareTo(LIMITE_FATOR_R) >= 0) {
                anexo = "Anexo III";
            }
        }
        
        final String anexoBusca = "Anexo III".equals(anexo) ? "III" : "V";
        final BigDecimal rbt12Final = rbt12Estimado;
        final String anexoBuscaExtenso = "Anexo " + anexoBusca;
        FaixaTributaria faixa = params.getFaixas().stream()
                .filter(f -> f.getAnexo().equals(anexoBusca) || f.getAnexo().equals(anexoBuscaExtenso))
                .filter(f -> f.getLimiteSuperior() == null || rbt12Final.compareTo(f.getLimiteSuperior()) <= 0)
                .min(java.util.Comparator.comparing(FaixaTributaria::getNumeroFaixa))
                .orElse(null);

        BigDecimal aliquotaEfetivaSN = BigDecimal.ZERO;
        if (faixa != null) {
            if (rbt12Estimado.compareTo(BigDecimal.ZERO) == 0) {
                aliquotaEfetivaSN = faixa.getAliquotaNominal();
            } else {
                BigDecimal impostoPrevisto = rbt12Estimado.multiply(faixa.getAliquotaNominal()).subtract(faixa.getValorDeduzir());
                if (impostoPrevisto.compareTo(BigDecimal.ZERO) < 0) impostoPrevisto = BigDecimal.ZERO;
                aliquotaEfetivaSN = impostoPrevisto.divide(rbt12Estimado, 4, RoundingMode.HALF_UP);
            }
        }
        
        BigDecimal valorEstimadoSN = faturamentoEstimado.multiply(aliquotaEfetivaSN).setScale(2, RoundingMode.HALF_UP);
        
        return ProjecaoCenarioDTO.MesProjecaoDTO.builder()
                .competencia(competencia)
                .faturamentoEstimado(faturamentoEstimado)
                .folhaEstimada(folhaEstimada)
                .rbt12Estimado(rbt12Estimado)
                .percentualFatorR(fatorR.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP))
                .anexoAplicavel(anexo)
                .valorEstimadoSimplesNacional(valorEstimadoSN)
                .build();
    }
}
