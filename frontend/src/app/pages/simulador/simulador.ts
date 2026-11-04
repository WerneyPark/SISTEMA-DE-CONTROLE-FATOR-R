import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FatorRService } from '../../services/fator-r.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import { ContaReceberService, DadosNotaFiscalDTO } from '../../services/conta-receber.service';
import { Empresa, SimulacaoTributaria, ProjecaoCenario, FaturamentoHistorico } from '../../models/models';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-simulador',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './simulador.html',
  styleUrls: ['./simulador.css']
})
export class SimuladorComponent implements OnInit, OnDestroy {
  empresaSelecionada: Empresa | null = null;
  simulacao: SimulacaoTributaria | null = null;
  projecoes: ProjecaoCenario[] = [];
  carregando = false;
  erro: string | null = null;
  sucesso: string | null = null;
  subscription?: Subscription;

  // Formulário de competência geral (Simulador)
  anoReferencia: number = new Date().getFullYear();
  mesReferencia: number = new Date().getMonth() + 1;

  // Modal Inserir Dados para Simulação
  mostrarModalInserirDados = false;
  modalAno: number = new Date().getFullYear();
  modalMes: number = new Date().getMonth() + 1;
  modalFaturamento: number = 0;
  modalFolha: number = 0;
  modalErro: string | null = null;
  salvandoDados = false;

  // Modal Importação de XML NF-e direto no Simulador
  mostrarModalXml = false;
  arquivoXml: File | null = null;
  carregandoPreview = false;
  importandoXml = false;
  previewDados: DadosNotaFiscalDTO | null = null;
  atualizarFaturamento = true;
  erroPreview = '';

  meses = [
    { valor: 1, nome: 'Janeiro' }, { valor: 2, nome: 'Fevereiro' },
    { valor: 3, nome: 'Março' }, { valor: 4, nome: 'Abril' },
    { valor: 5, nome: 'Maio' }, { valor: 6, nome: 'Junho' },
    { valor: 7, nome: 'Julho' }, { valor: 8, nome: 'Agosto' },
    { valor: 9, nome: 'Setembro' }, { valor: 10, nome: 'Outubro' },
    { valor: 11, nome: 'Novembro' }, { valor: 12, nome: 'Dezembro' }
  ];

  constructor(
    private fatorRService: FatorRService,
    private empresaContext: EmpresaContextService,
    private contaReceberService: ContaReceberService
  ) {}

  ngOnInit(): void {
    this.subscription = this.empresaContext.empresaSelecionada$.subscribe(empresa => {
      this.empresaSelecionada = empresa;
      this.simulacao = null;
      if (empresa) {
        this.simular();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  simular(): void {
    if (!this.empresaSelecionada?.id) return;
    this.carregando = true;
    this.erro = null;

    this.fatorRService.simularTributacao(
      this.empresaSelecionada.id,
      this.anoReferencia,
      this.mesReferencia
    ).subscribe({
      next: (data) => {
        this.simulacao = data;
        
        if (this.empresaSelecionada?.id) {
          this.fatorRService.simularFuturo(
            this.empresaSelecionada.id,
            this.anoReferencia,
            this.mesReferencia
          ).subscribe({
            next: (projData) => {
              this.projecoes = projData;
              this.carregando = false;
            },
            error: () => {
              this.carregando = false;
            }
          });
        } else {
          this.carregando = false;
        }
      },
      error: () => {
        this.erro = 'Erro ao calcular simulação tributária.';
        this.carregando = false;
      }
    });
  }

  usarCompetenciaAtual(): void {
    const agora = new Date();
    this.anoReferencia = agora.getFullYear();
    this.mesReferencia = agora.getMonth() + 1;
    if (this.empresaSelecionada) this.simular();
  }

  // ===================== SISTEMA DE ENCERRAMENTO =====================

  isCompetenciaEncerrada(comp: string): boolean {
    if (!this.simulacao?.competenciasEncerradas) return false;
    return this.simulacao.competenciasEncerradas.includes(comp);
  }

  encerrarCompetencia(comp: string): void {
    if (!this.empresaSelecionada?.id) return;
    if (!confirm(`Deseja realmente encerrar a competência ${comp}? Os valores para este mês serão consolidados com base nos arquivos de importação (XML e eSocial) e novas alterações manuais serão bloqueadas.`)) {
      return;
    }

    this.carregando = true;
    this.erro = null;
    this.sucesso = null;

    this.fatorRService.encerrarCompetencia(this.empresaSelecionada.id, comp).subscribe({
      next: (res) => {
        this.sucesso = res.mensagem || `Competência ${comp} encerrada com sucesso!`;
        this.simular();
      },
      error: (err) => {
        this.carregando = false;
        this.erro = err.error?.erro || 'Erro ao encerrar competência.';
      }
    });
  }

  reabrirCompetencia(comp: string): void {
    if (!this.empresaSelecionada?.id) return;
    if (!confirm(`Deseja reabrir a competência ${comp}? O mês voltará a permitir novos lançamentos e ajustes.`)) {
      return;
    }

    this.carregando = true;
    this.erro = null;
    this.sucesso = null;

    this.fatorRService.reabrirCompetencia(this.empresaSelecionada.id, comp).subscribe({
      next: (res) => {
        this.sucesso = res.mensagem || `Competência ${comp} reaberta com sucesso!`;
        this.simular();
      },
      error: (err) => {
        this.carregando = false;
        this.erro = err.error?.erro || 'Erro ao reabrir competência.';
      }
    });
  }

  // ===================== MODAL INSERIR DADOS =====================

  abrirModalInserirDados(): void {
    this.modalAno = this.anoReferencia;
    this.modalMes = this.mesReferencia;
    this.modalFaturamento = 0;
    this.modalFolha = 0;
    this.modalErro = null;

    // Se houver valor no histórico para essa competência, pré-carrega
    const comp = `${this.modalAno}-${this.modalMes.toString().padStart(2, '0')}`;
    const hist = this.simulacao?.historicoFaturamentos?.find(h => h.competencia === comp);
    if (hist) {
      this.modalFaturamento = hist.faturamento;
      this.modalFolha = hist.folhaPagamento;
    }

    this.mostrarModalInserirDados = true;
  }

  fecharModalInserirDados(): void {
    this.mostrarModalInserirDados = false;
    this.modalErro = null;
  }

  get modalCompetenciaFormatada(): string {
    return `${this.modalAno}-${this.modalMes.toString().padStart(2, '0')}`;
  }

  get modalMesEncerrado(): boolean {
    return this.isCompetenciaEncerrada(this.modalCompetenciaFormatada);
  }

  onModalCompetenciaChange(): void {
    const comp = this.modalCompetenciaFormatada;
    const hist = this.simulacao?.historicoFaturamentos?.find(h => h.competencia === comp);
    if (hist) {
      this.modalFaturamento = hist.faturamento;
      this.modalFolha = hist.folhaPagamento;
    } else {
      this.modalFaturamento = 0;
      this.modalFolha = 0;
    }
    this.modalErro = null;
  }

  salvarDadosSimulacao(): void {
    if (!this.empresaSelecionada?.id) return;
    if (this.modalMesEncerrado) {
      this.modalErro = 'Este mês já está encerrado. Reabra a competência antes de alterar os valores.';
      return;
    }

    this.salvandoDados = true;
    this.modalErro = null;

    const payload = {
      competencia: this.modalCompetenciaFormatada,
      valorFaturamento: this.modalFaturamento,
      valorFolha: this.modalFolha
    };

    this.fatorRService.salvarDadosSimulacao(this.empresaSelecionada.id, payload).subscribe({
      next: (res) => {
        this.salvandoDados = false;
        this.sucesso = res.mensagem || 'Dados de simulação salvos com sucesso!';
        this.fecharModalInserirDados();
        this.simular();
      },
      error: (err) => {
        this.salvandoDados = false;
        this.modalErro = err.error?.erro || 'Erro ao salvar dados de simulação.';
      }
    });
  }

  // ===================== MODAL IMPORTAR XML NO SIMULADOR =====================

  abrirModalXml(): void {
    this.mostrarModalXml = true;
    this.arquivoXml = null;
    this.previewDados = null;
    this.erroPreview = '';
    this.atualizarFaturamento = true;
  }

  fecharModalXml(): void {
    this.mostrarModalXml = false;
    this.arquivoXml = null;
    this.previewDados = null;
    this.erroPreview = '';
    this.carregandoPreview = false;
    this.importandoXml = false;
  }

  selecionarArquivoXml(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.arquivoXml = input.files[0];
      this.obterPreview();
    }
  }

  obterPreview(): void {
    if (!this.arquivoXml || !this.empresaSelecionada) return;
    const empresaIdStr = this.empresaSelecionada.uuidId || String(this.empresaSelecionada.id);

    this.carregandoPreview = true;
    this.erroPreview = '';
    this.previewDados = null;

    this.contaReceberService.previewXml(empresaIdStr, this.arquivoXml).subscribe({
      next: dados => {
        this.previewDados = dados;
        this.carregandoPreview = false;
        if (dados.periodoEncerrado) {
          this.erroPreview = dados.mensagemAlertaPeriodo || 'Atenção: A data de emissão desta nota pertence a um período já encerrado. Abra o período para prosseguir.';
        }
      },
      error: err => {
        this.carregandoPreview = false;
        this.erroPreview = err.error?.message || 'Erro ao processar pré-visualização do XML.';
      }
    });
  }

  confirmarImportacaoXml(): void {
    if (!this.arquivoXml || !this.empresaSelecionada) return;
    if (this.previewDados?.periodoEncerrado) {
      this.erroPreview = 'Não é possível importar a nota em um período já encerrado. Abra a competência no Simulador antes de importar.';
      return;
    }

    const empresaIdStr = this.empresaSelecionada.uuidId || String(this.empresaSelecionada.id);
    this.importandoXml = true;
    this.erroPreview = '';

    this.contaReceberService.importarXml(empresaIdStr, this.arquivoXml, this.atualizarFaturamento).subscribe({
      next: titulos => {
        this.importandoXml = false;
        this.sucesso = `Nota fiscal importada com sucesso: ${titulos.length} título(s) gerado(s) e Faturamento Mensal atualizado!`;
        this.fecharModalXml();
        this.simular();
      },
      error: err => {
        this.importandoXml = false;
        this.erroPreview = err.error?.message || err.error?.erro || 'Erro ao importar XML da nota fiscal.';
      }
    });
  }

  // ===================== FORMATADORES =====================

  getNomeMes(mes: number): string {
    return this.meses[mes - 1]?.nome || '';
  }

  getGaugeRotation(): number {
    if (!this.simulacao) return -90;
    const pct = Math.min(this.simulacao.percentualFatorR, 56);
    return -90 + (pct / 56) * 180;
  }

  getGaugeColor(): string {
    if (!this.simulacao) return '#64748b';
    const pct = this.simulacao.percentualFatorR;
    if (pct >= 28) return '#10b981';
    if (pct >= 25) return '#f59e0b';
    return '#ef4444';
  }

  formatCurrency(value?: number | null): string {
    if (value === undefined || value === null) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  }
}
