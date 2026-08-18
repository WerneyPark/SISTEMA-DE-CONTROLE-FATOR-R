import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FatorRService } from '../../services/fator-r.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import { Empresa, SimulacaoTributaria, ProjecaoCenario } from '../../models/models';
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
  subscription?: Subscription;

  // Formulário de competência geral (Simulador)
  anoReferencia: number = new Date().getFullYear();
  mesReferencia: number = new Date().getMonth() + 1;

  // Formulários de Lançamento
  mostrarFormLancamento = false;
  lancamentoFaturamento = { ano: new Date().getFullYear(), mes: new Date().getMonth() + 1, valor: 0 };
  lancamentoFolha = { ano: new Date().getFullYear(), mes: new Date().getMonth() + 1, valor: 0 };
  lancamentoSucesso: string | null = null;

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
    private empresaContext: EmpresaContextService
  ) {}

  ngOnInit(): void {
    // Escuta a empresa selecionada globalmente
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
        
        // Fetch future projections
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
      error: (err) => {
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

  toggleFormLancamento(): void {
    this.mostrarFormLancamento = !this.mostrarFormLancamento;
    this.lancamentoSucesso = null;
  }

  salvarFaturamento(): void {
    if (!this.empresaSelecionada?.id) return;
    
    const competencia = `${this.lancamentoFaturamento.ano}-${this.lancamentoFaturamento.mes.toString().padStart(2, '0')}`;
    
    this.fatorRService.criarFaturamento({
      empresaId: this.empresaSelecionada.id,
      competencia: competencia,
      valor: this.lancamentoFaturamento.valor
    }).subscribe({
      next: () => {
        this.lancamentoSucesso = `Faturamento salvo com sucesso!`;
        this.lancamentoFaturamento.valor = 0;
        this.simular();
      },
      error: () => this.erro = 'Erro ao salvar faturamento.'
    });
  }

  salvarFolha(): void {
    if (!this.empresaSelecionada?.id) return;
    
    const competencia = `${this.lancamentoFolha.ano}-${this.lancamentoFolha.mes.toString().padStart(2, '0')}`;
    
    this.fatorRService.criarFolha({
      empresaId: this.empresaSelecionada.id,
      competencia: competencia,
      valor: this.lancamentoFolha.valor
    }).subscribe({
      next: () => {
        this.lancamentoSucesso = `Folha de Pagamento salva com sucesso!`;
        this.lancamentoFolha.valor = 0;
        this.simular();
      },
      error: () => this.erro = 'Erro ao salvar folha de pagamento.'
    });
  }

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

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  }
}
