import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BankingService } from '../../services/banking.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import { AuthService } from '../../services/auth.service';
import { ExtratoLancamentoDTO } from '../../shared/types/banking.types';
import { ContaReceberDTO } from '../../shared/types/receivables.types';

@Component({
  selector: 'app-banking',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './banking.html',
  styleUrls: ['./banking.css']
})
export class BankingComponent implements OnInit {
  lancamentos: ExtratoLancamentoDTO[] = [];
  contas: ContaReceberDTO[] = [];
  loading = true;
  importando = false;

  lancamentoSelecionado: ExtratoLancamentoDTO | null = null;
  contaSelecionada: ContaReceberDTO | null = null;
  observacao = '';
  conciliando = false;
  mensagemErro = '';
  mensagemSucesso = '';

  private empresaId: string | null = null;
  private usuarioId: string | null = null;

  constructor(
    private bankingService: BankingService,
    private empresaContextService: EmpresaContextService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    const empresa = this.empresaContextService.getEmpresaSelecionada();
    const usuario = this.authService.getCurrentUser();

    this.empresaId = empresa?.uuidId ?? null;
    this.usuarioId = usuario?.uuidId ?? null;

    if (!this.empresaId) {
      this.mensagemErro = 'Selecione uma empresa para continuar.';
      this.loading = false;
      return;
    }

    this.carregarDados();
  }

  carregarDados() {
    if (!this.empresaId) return;
    this.loading = true;
    this.lancamentoSelecionado = null;
    this.contaSelecionada = null;

    this.bankingService.listarLancamentosPendentes(this.empresaId).subscribe({
      next: (dados) => {
        this.lancamentos = dados;
        this.carregarContas();
      },
      error: (err) => {
        console.error('Erro ao carregar lançamentos', err);
        this.loading = false;
      }
    });
  }

  private carregarContas() {
    if (!this.empresaId) return;
    this.bankingService.listarContasPendentes(this.empresaId).subscribe({
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
    if (!this.lancamentoSelecionado || !this.contaSelecionada || !this.usuarioId) {
      this.mensagemErro = 'Selecione um lançamento e uma conta para conciliar.';
      return;
    }

    this.conciliando = true;
    this.mensagemErro = '';

    this.bankingService
      .conciliar(
        this.lancamentoSelecionado.id,
        this.contaSelecionada.id,
        this.usuarioId,
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

  formatarValor(valor: number): string {
    return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }
}