import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BankingService } from '../../services/banking.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import { AuthService } from '../../services/auth.service';
import { ExtratoLancamentoDTO, ConciliacaoBancariaDTO } from '../../shared/types/banking.types';
import { ContaReceberDTO } from '../../shared/types/receivables.types';

@Component({
  selector: 'app-banking',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './banking.html',
  styleUrls: ['./banking.css']
})
export class BankingComponent implements OnInit, OnDestroy {
  lancamentos: ExtratoLancamentoDTO[] = [];
  contas: ContaReceberDTO[] = [];
  historico: ConciliacaoBancariaDTO[] = [];

  abaAtiva: 'pendentes' | 'historico' = 'pendentes';

  // Filtro de período mensal
  competenciaFiltro = '';

  loading = true;
  loadingHistorico = false;
  importando = false;

  lancamentoSelecionado: ExtratoLancamentoDTO | null = null;
  contaSelecionada: ContaReceberDTO | null = null;
  observacao = '';
  conciliando = false;
  desconciliandoId: string | null = null;
  mensagemErro = '';
  mensagemSucesso = '';

  private empresaId: string | null = null;
  private usuarioId: string | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private bankingService: BankingService,
    private empresaContextService: EmpresaContextService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    const usuario = this.authService.getCurrentUser();
    this.usuarioId = usuario?.uuidId || (usuario?.id ? String(usuario.id) : null);

    this.empresaContextService.empresaSelecionada$
      .pipe(takeUntil(this.destroy$))
      .subscribe(empresa => {
        this.empresaId = empresa?.uuidId || (empresa?.id ? String(empresa.id) : null);

        if (!this.empresaId) {
          this.mensagemErro = 'Selecione uma empresa para continuar.';
          this.loading = false;
          this.lancamentos = [];
          this.contas = [];
          this.historico = [];
        } else {
          this.mensagemErro = '';
          this.carregarDados();
        }
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setAba(aba: 'pendentes' | 'historico') {
    this.abaAtiva = aba;
    this.mensagemErro = '';
    this.mensagemSucesso = '';
    if (aba === 'historico') {
      this.carregarHistorico();
    }
  }

  carregarDados() {
    if (!this.empresaId) return;
    this.loading = true;
    this.lancamentoSelecionado = null;
    this.contaSelecionada = null;

    const comp = this.competenciaFiltro || undefined;

    this.bankingService.listarLancamentosPendentes(this.empresaId, comp).subscribe({
      next: (dados) => {
        this.lancamentos = dados;
        this.carregarContas();
      },
      error: (err) => {
        console.error('Erro ao carregar lançamentos', err);
        this.loading = false;
      }
    });

    this.carregarHistorico();
  }

  private carregarContas() {
    if (!this.empresaId) return;
    const comp = this.competenciaFiltro || undefined;
    this.bankingService.listarContasPendentes(this.empresaId, comp).subscribe({
      next: (dados) => {
        this.contas = dados;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erro ao carregar contas a receber', err);
        this.loading = false;
      }
    });
  }

  carregarHistorico() {
    if (!this.empresaId) return;
    this.loadingHistorico = true;
    const comp = this.competenciaFiltro || undefined;
    this.bankingService.listarConciliacoes(this.empresaId, comp).subscribe({
      next: (dados) => {
        this.historico = dados;
        this.loadingHistorico = false;
      },
      error: (err) => {
        console.error('Erro ao carregar histórico de conciliações', err);
        this.loadingHistorico = false;
      }
    });
  }

  onCompetenciaChange() {
    this.carregarDados();
  }

  limparFiltroCompetencia() {
    this.competenciaFiltro = '';
    this.carregarDados();
  }

  excluirLancamento(lancamento: ExtratoLancamentoDTO, event: Event) {
    event.stopPropagation();
    if (!this.empresaId) return;
    if (!confirm(`Deseja realmente excluir o lançamento "${lancamento.descricao}" no valor de ${this.formatarValor(lancamento.valor)}?`)) {
      return;
    }

    this.bankingService.excluirLancamentoExtrato(this.empresaId, lancamento.id).subscribe({
      next: () => {
        this.mensagemSucesso = 'Lançamento do extrato excluído com sucesso!';
        this.mensagemErro = '';
        this.carregarDados();
      },
      error: (err) => {
        this.mensagemErro = err?.error?.erro || 'Erro ao excluir lançamento do extrato.';
      }
    });
  }

  // ===================== SUGESTÃO E COMBINAÇÃO VISUAL =====================

  temSugestaoLancamento(lancamento: ExtratoLancamentoDTO): boolean {
    return this.contas.some(conta => Math.abs(Number(conta.valorTotal) - Number(lancamento.valor)) < 0.001);
  }

  temSugestaoConta(conta: ContaReceberDTO): boolean {
    return this.lancamentos.some(lancamento => Math.abs(Number(lancamento.valor) - Number(conta.valorTotal)) < 0.001);
  }

  isMatchComSelecionado(lancamento: ExtratoLancamentoDTO): boolean {
    if (!this.contaSelecionada) return false;
    return Math.abs(Number(lancamento.valor) - Number(this.contaSelecionada.valorTotal)) < 0.001;
  }

  isContaMatchComSelecionado(conta: ContaReceberDTO): boolean {
    if (!this.lancamentoSelecionado) return false;
    return Math.abs(Number(conta.valorTotal) - Number(this.lancamentoSelecionado.valor)) < 0.001;
  }

  selecionarLancamento(lancamento: ExtratoLancamentoDTO) {
    this.lancamentoSelecionado =
      this.lancamentoSelecionado?.id === lancamento.id ? null : lancamento;
    this.mensagemErro = '';
    this.mensagemSucesso = '';
  }

  selecionarConta(conta: ContaReceberDTO) {
    this.contaSelecionada = this.contaSelecionada?.id === conta.id ? null : conta;
    this.mensagemErro = '';
    this.mensagemSucesso = '';
  }

  get podeConciliar(): boolean {
    return !!this.lancamentoSelecionado && !!this.contaSelecionada && !this.conciliando;
  }

  confirmarConciliacao() {
    if (!this.lancamentoSelecionado || !this.contaSelecionada) {
      this.mensagemErro = 'Selecione um lançamento bancário e um título a receber para conciliar.';
      return;
    }

    const usuarioIdFinal = this.usuarioId || 'SISTEMA';

    this.conciliando = true;
    this.mensagemErro = '';

    this.bankingService
      .conciliar(
        this.lancamentoSelecionado.id,
        this.contaSelecionada.id,
        usuarioIdFinal,
        this.observacao || undefined
      )
      .subscribe({
        next: () => {
          this.mensagemSucesso = 'Conciliação realizada com sucesso!';
          this.observacao = '';
          this.conciliando = false;
          this.carregarDados();
        },
        error: (err) => {
          this.mensagemErro = err?.error?.erro || 'Erro ao conciliar. Tente novamente.';
          this.conciliando = false;
        }
      });
  }

  desconciliar(conciliacao: ConciliacaoBancariaDTO) {
    if (!confirm('Deseja realmente desfazer esta conciliação? O extrato e o título voltarão para o status PENDENTE.')) {
      return;
    }

    this.desconciliandoId = conciliacao.id;
    this.mensagemErro = '';
    this.mensagemSucesso = '';

    this.bankingService.desconciliar(conciliacao.id).subscribe({
      next: () => {
        this.mensagemSucesso = 'Conciliação desfeita com sucesso! Os lançamentos retornaram aos pendentes.';
        this.desconciliandoId = null;
        this.carregarDados();
      },
      error: (err) => {
        this.mensagemErro = err?.error?.erro || 'Erro ao desconciliar lançamento.';
        this.desconciliandoId = null;
      }
    });
  }

  onArquivoSelecionado(event: Event, tipo: 'csv' | 'ofx') {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (!arquivo || !this.empresaId) return;

    this.importando = true;
    this.mensagemErro = '';
    this.mensagemSucesso = '';

    const importacao$ =
      tipo === 'csv'
        ? this.bankingService.importarCsv(arquivo, this.empresaId)
        : this.bankingService.importarOfx(arquivo, this.empresaId);

    importacao$.subscribe({
      next: (resultado) => {
        this.mensagemSucesso = `${resultado.novosLancamentos} lançamento(s) importado(s), ${resultado.duplicadosIgnorados} duplicado(s) ignorado(s).`;
        this.importando = false;
        input.value = '';
        this.carregarDados();
      },
      error: (err) => {
        this.mensagemErro = err?.error?.erro || 'Erro ao importar arquivo.';
        this.importando = false;
        input.value = '';
      }
    });
  }

  formatarValor(valor?: number | null): string {
    if (valor == null) return 'R$ 0,00';
    return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  formatarData(dataStr?: string | null): string {
    if (!dataStr) return '-';
    // Suporta yyyy-MM-dd ou yyyy-MM-ddTHH:mm:ss
    const partes = dataStr.split('T');
    const dataPart = partes[0].split('-');
    if (dataPart.length === 3) {
      const dataFormatada = `${dataPart[2]}/${dataPart[1]}/${dataPart[0]}`;
      if (partes[1]) {
        const horaPart = partes[1].substring(0, 5);
        return `${dataFormatada} às ${horaPart}`;
      }
      return dataFormatada;
    }
    return dataStr;
  }
}
