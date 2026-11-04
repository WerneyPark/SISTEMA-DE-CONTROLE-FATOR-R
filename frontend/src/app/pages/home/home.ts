import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import { FatorRService } from '../../services/fator-r.service';
import { ContaReceberService } from '../../services/conta-receber.service';
import { BankingService } from '../../services/banking.service';
import { ReportsService } from '../../services/reports.service';
import { Empresa, SimulacaoTributaria } from '../../models/models';
import { ContaReceberDTO } from '../../shared/types/receivables.types';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent implements OnInit, OnDestroy {
  isAdmin = false;
  usuarioNome = '';
  empresaSelecionada: Empresa | null = null;
  carregando = false;

  // KPIs e Dados da Simulação Tributária
  simulacao: SimulacaoTributaria | null = null;
  
  // Contas a Receber
  totalContasPendentes = 0;
  valorTotalContasPendentes = 0;
  contasRecentes: ContaReceberDTO[] = [];

  // Banking
  totalLancamentosPendentes = 0;
  
  // Reports
  totalLaudosEmitidos = 0;

  competenciaAtualTexto = '';

  private sub?: Subscription;

  constructor(
    private authService: AuthService,
    private empresaContext: EmpresaContextService,
    private fatorRService: FatorRService,
    private contaReceberService: ContaReceberService,
    private bankingService: BankingService,
    private reportsService: ReportsService
  ) {}

  ngOnInit() {
    this.isAdmin = this.authService.isAdmin();
    const user = this.authService.getCurrentUser();
    this.usuarioNome = user?.nome || 'Usuário';

    const hoje = new Date();
    const meses = [
      'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
      'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
    ];
    this.competenciaAtualTexto = `${meses[hoje.getMonth()]} de ${hoje.getFullYear()}`;

    this.sub = this.empresaContext.empresaSelecionada$.subscribe(emp => {
      this.empresaSelecionada = emp;
      if (emp && emp.id) {
        this.carregarDadosDashboard(emp);
      } else {
        this.limparDados();
      }
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  carregarDadosDashboard(empresa: Empresa) {
    this.carregando = true;
    const empresaIdStr = empresa.uuidId || String(empresa.id);
    const empresaIdNum = empresa.id!;
    const agora = new Date();
    const ano = agora.getFullYear();
    const mes = agora.getMonth() + 1;

    // 1. Simulação Tributária do Mês Atual
    this.fatorRService.simularTributacao(empresaIdNum, ano, mes).subscribe({
      next: (dados) => {
        this.simulacao = dados;
        this.carregando = false;
      },
      error: () => {
        this.simulacao = null;
        this.carregando = false;
      }
    });

    // 2. Contas a Receber Pendentes
    this.contaReceberService.listar(empresaIdStr, { status: 'PENDENTE', pagina: 0, tamanho: 5 }).subscribe({
      next: (res) => {
        this.totalContasPendentes = res.totalElements || 0;
        this.contasRecentes = res.content || [];
        this.valorTotalContasPendentes = (res.content || []).reduce(
          (acc, c) => acc + (Number(c.valorTotal) || 0), 0
        );
      },
      error: () => {
        this.totalContasPendentes = 0;
        this.contasRecentes = [];
      }
    });

    // 3. Extratos Pendentes de Conciliação
    this.bankingService.listarLancamentosPendentes(empresaIdStr).subscribe({
      next: (lancamentos) => {
        this.totalLancamentosPendentes = (lancamentos || []).length;
      },
      error: () => {
        this.totalLancamentosPendentes = 0;
      }
    });

    // 4. Histórico de Laudos
    this.reportsService.listarHistorico(empresaIdStr).subscribe({
      next: (laudos) => {
        this.totalLaudosEmitidos = (laudos || []).length;
      },
      error: () => {
        this.totalLaudosEmitidos = 0;
      }
    });
  }

  limparDados() {
    this.simulacao = null;
    this.totalContasPendentes = 0;
    this.valorTotalContasPendentes = 0;
    this.contasRecentes = [];
    this.totalLancamentosPendentes = 0;
    this.totalLaudosEmitidos = 0;
  }

  formatCurrency(value?: number | null): string {
    if (value === undefined || value === null) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  }

  getProgressoMetaFatorR(): number {
    if (!this.simulacao) return 0;
    const pct = this.simulacao.percentualFatorR || 0;
    return Math.min(Math.round((pct / 28) * 100), 100);
  }
}
