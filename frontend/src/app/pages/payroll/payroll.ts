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
import { FolhaDetalhadaDTO } from '../../shared/types/payroll.types';

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

  empresaId: string | null = null;
  empresaNome = '';

  abaAtiva: 'tabela' | 'importador' | 'simulador' = 'tabela';

  mensagemSucesso = '';
  mensagemErro = '';

  // Competência de Referência (idêntico ao Simulador Fator R)
  anoReferencia: number = new Date().getFullYear();
  mesReferencia: number = new Date().getMonth() + 1;

  meses = [
    { valor: 1, nome: '01 - Janeiro' },
    { valor: 2, nome: '02 - Fevereiro' },
    { valor: 3, nome: '03 - Março' },
    { valor: 4, nome: '04 - Abril' },
    { valor: 5, nome: '05 - Maio' },
    { valor: 6, nome: '06 - Junho' },
    { valor: 7, nome: '07 - Julho' },
    { valor: 8, nome: '08 - Agosto' },
    { valor: 9, nome: '09 - Setembro' },
    { valor: 10, nome: '10 - Outubro' },
    { valor: 11, nome: '11 - Novembro' },
    { valor: 12, nome: '12 - Dezembro' }
  ];

  // Último resultado de importação
  ultimoResultadoImportacao: ImportFolhaFullResponseDTO | null = null;

  // Simulador de Pró-Labore Ideal
  proLaboreIdeal: ProLaboreIdealFullDTO | null = null;

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

  get competenciaFormatada(): string {
    const m = String(this.mesReferencia).padStart(2, '0');
    return `${this.anoReferencia}-${m}`;
  }

  get periodo12MesesTexto(): string {
    // 12 meses anteriores: M-12 até M-1
    let anoInicio = this.anoReferencia - 1;
    let mesInicio = this.mesReferencia;
    let anoFim = this.anoReferencia;
    let mesFim = this.mesReferencia - 1;
    if (mesFim === 0) {
      mesFim = 12;
      anoFim = this.anoReferencia - 1;
    }
    const inicioStr = `${String(mesInicio).padStart(2, '0')}/${anoInicio}`;
    const fimStr = `${String(mesFim).padStart(2, '0')}/${anoFim}`;
    return `${inicioStr} a ${fimStr}`;
  }

  onCompetenciaChange(): void {
    if (this.empresaId) {
      this.carregarFolhas();
      this.executarSimulacao();
    }
  }

  usarCompetenciaAtual(): void {
    const d = new Date();
    this.anoReferencia = d.getFullYear();
    this.mesReferencia = d.getMonth() + 1;
    this.onCompetenciaChange();
  }

  setAba(aba: 'tabela' | 'importador' | 'simulador'): void {
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
    this.payrollService.listarFolhas(this.empresaId, this.competenciaFormatada)
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
      this.competenciaFormatada
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
