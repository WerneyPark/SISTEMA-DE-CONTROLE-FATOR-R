import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  PayrollService,
  ProLaboreIdealFullDTO,
  ImportFolhaFullResponseDTO
} from '../../services/payroll.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import {
  FolhaDetalhadaDTO,
  CreateFolhaDetalhadaDTO
} from '../../shared/types/payroll.types';

@Component({
  selector: 'app-payroll',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payroll.html',
  styleUrls: ['./payroll.css']
})
export class PayrollComponent implements OnInit, OnDestroy {
  folhas: FolhaDetalhadaDTO[] = [];
  loading = false;
  importando = false;
  calculando = false;
  salvando = false;

  empresaId: string | null = null;
  empresaNome = '';

  abaAtiva: 'tabela' | 'importador' | 'simulador' | 'manual' = 'tabela';

  mensagemSucesso = '';
  mensagemErro = '';

  // Último resultado de importação
  ultimoResultadoImportacao: ImportFolhaFullResponseDTO | null = null;

  // Simulador de Pró-Labore Ideal
  competenciaSimulacao: string = this.getMesAtual();
  faturamentoSimulacao = 25000;
  proLaboreIdeal: ProLaboreIdealFullDTO | null = null;

  // Cadastro Manual
  formManual: CreateFolhaDetalhadaDTO = {
    empresaId: '',
    competencia: this.getMesAtual(),
    valorSalarios: 0,
    valorProLabore: 0,
    valorDecimoTerceiro: 0,
    valorInssPatronal: 0,
    valorFgts: 0,
    origem: 'MANUAL'
  };

  private destroy$ = new Subject<void>();

  constructor(
    private payrollService: PayrollService,
    private empresaContextService: EmpresaContextService
  ) {}

  ngOnInit(): void {
    this.empresaContextService.empresaSelecionada$
      .pipe(takeUntil(this.destroy$))
      .subscribe(empresa => {
        this.empresaId = empresa?.uuidId || (empresa?.id ? String(empresa.id) : null);
        this.empresaNome = empresa?.razaoSocial || '';

        if (this.empresaId) {
          this.formManual.empresaId = this.empresaId;
          this.mensagemErro = '';
          this.carregarFolhas();
          this.executarSimulacao();
        } else {
          this.folhas = [];
          this.proLaboreIdeal = null;
          this.mensagemErro = 'Selecione uma empresa no cabeçalho para visualizar ou gerenciar a folha de pagamento.';
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setAba(aba: 'tabela' | 'importador' | 'simulador' | 'manual'): void {
    this.abaAtiva = aba;
    this.mensagemErro = '';
    this.mensagemSucesso = '';
    if (aba === 'simulador' && !this.proLaboreIdeal && this.empresaId) {
      this.executarSimulacao();
    }
  }

  carregarFolhas(): void {
    if (!this.empresaId) return;
    this.loading = true;
    this.payrollService.listarFolhas(this.empresaId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: folhas => {
          this.folhas = folhas || [];
          this.loading = false;
        },
        error: err => {
          this.loading = false;
          this.mensagemErro = 'Não foi possível carregar o histórico de folhas analíticas: ' + (err.error?.erro || err.message);
        }
      });
  }

  // ==========================================
  // UPLOAD E IMPORTAÇÃO DE ARQUIVOS
  // ==========================================

  onArquivoSelecionado(event: Event, tipo: 'esocial' | 'csv'): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    if (!this.empresaId) {
      this.mensagemErro = 'Selecione uma empresa antes de importar o arquivo.';
      return;
    }

    const file = input.files[0];
    this.importando = true;
    this.mensagemErro = '';
    this.mensagemSucesso = '';
    this.ultimoResultadoImportacao = null;

    const requisicao$ = tipo === 'esocial'
      ? this.payrollService.importarEsocial(file, this.empresaId)
      : this.payrollService.importarCsv(file, this.empresaId);

    requisicao$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (resultado: ImportFolhaFullResponseDTO) => {
        this.importando = false;
        this.ultimoResultadoImportacao = resultado;
        this.mensagemSucesso = resultado.mensagem || 'Arquivo de folha importado e sincronizado com sucesso!';
        this.carregarFolhas();
        this.executarSimulacao();
        input.value = '';
      },
      error: err => {
        this.importando = false;
        this.mensagemErro = 'Erro ao processar importação da folha: ' + (err.error?.erro || err.message);
        input.value = '';
      }
    });
  }

  // ==========================================
  // SIMULADOR DE PRÓ-LABORE IDEAL
  // ==========================================

  executarSimulacao(): void {
    if (!this.empresaId) return;

    this.calculando = true;
    this.payrollService.calcularProLaboreIdeal(
      this.empresaId,
      this.competenciaSimulacao,
      this.faturamentoSimulacao
    )
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (dto: ProLaboreIdealFullDTO) => {
          this.proLaboreIdeal = dto;
          this.calculando = false;
        },
        error: err => {
          this.calculando = false;
          this.mensagemErro = 'Falha ao calcular Pró-Labore Ideal: ' + (err.error?.erro || err.message);
        }
      });
  }

  onFaturamentoChange(valor: number): void {
    this.faturamentoSimulacao = valor;
    this.executarSimulacao();
  }

  onCompetenciaSimulacaoChange(comp: string): void {
    this.competenciaSimulacao = comp;
    this.executarSimulacao();
  }

  // ==========================================
  // CADASTRO MANUAL DE FOLHA
  // ==========================================

  get totalManual(): number {
    return (Number(this.formManual.valorSalarios) || 0) +
           (Number(this.formManual.valorProLabore) || 0) +
           (Number(this.formManual.valorDecimoTerceiro) || 0) +
           (Number(this.formManual.valorInssPatronal) || 0) +
           (Number(this.formManual.valorFgts) || 0);
  }

  salvarFolhaManual(): void {
    if (!this.empresaId) {
      this.mensagemErro = 'Selecione uma empresa para salvar a folha.';
      return;
    }
    if (!this.formManual.competencia) {
      this.mensagemErro = 'Informe a competência (AAAA-MM).';
      return;
    }

    this.salvando = true;
    this.mensagemErro = '';
    this.mensagemSucesso = '';

    this.formManual.empresaId = this.empresaId;
    this.payrollService.salvarFolha(this.formManual)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: salva => {
          this.salvando = false;
          this.mensagemSucesso = `Folha da competência ${salva.competencia} salva com sucesso e sincronizada com o cálculo do Fator R!`;
          this.carregarFolhas();
          this.executarSimulacao();
          this.setAba('tabela');
        },
        error: err => {
          this.salvando = false;
          this.mensagemErro = 'Falha ao salvar folha manual: ' + (err.error?.erro || err.message);
        }
      });
  }

  excluirFolha(id: string): void {
    if (!confirm('Deseja realmente excluir este registro detalhado de folha de pagamento?')) return;

    this.payrollService.excluirFolha(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.mensagemSucesso = 'Folha excluída com sucesso!';
          this.carregarFolhas();
          this.executarSimulacao();
        },
        error: err => {
          this.mensagemErro = 'Erro ao excluir folha: ' + (err.error?.erro || err.message);
        }
      });
  }

  // ==========================================
  // FORMATADORES E UTILS
  // ==========================================

  formatarMoeda(valor: number | undefined | null): string {
    if (valor === undefined || valor === null) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor);
  }

  formatarPercentual(valor: number | undefined | null): string {
    if (valor === undefined || valor === null) return '0,00%';
    return valor.toFixed(2).replace('.', ',') + '%';
  }

  formatarCompetencia(comp: string | undefined): string {
    if (!comp) return '';
    const parts = comp.split('-');
    if (parts.length === 2) {
      return `${parts[1]}/${parts[0]}`;
    }
    return comp;
  }

  getMesAtual(): string {
    const d = new Date();
    const ano = d.getFullYear();
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    return `${ano}-${mes}`;
  }

  getTotalFolhasSalarios(): number {
    return this.folhas.reduce((acc, f) => acc + (f.valorSalarios || 0), 0);
  }

  getTotalFolhasProLabore(): number {
    return this.folhas.reduce((acc, f) => acc + (f.valorProLabore || 0), 0);
  }

  getTotalFolhasEncargos(): number {
    return this.folhas.reduce((acc, f) => acc + (f.valorInssPatronal || 0) + (f.valorFgts || 0), 0);
  }

  getTotalFolhasGeral(): number {
    return this.folhas.reduce((acc, f) => acc + (f.valorTotal || 0), 0);
  }
}
