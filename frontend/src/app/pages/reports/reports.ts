import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subscription } from 'rxjs';
import { ReportsService } from '../../services/reports.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import { Empresa } from '../../models/models';
import { ParametrosEmissaoLaudoDTO } from '../../shared/types/reports.types';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reports.html',
  styleUrls: ['./reports.css']
})
export class ReportsComponent implements OnInit, OnDestroy {
  empresaSelecionada: Empresa | null = null;
  private subEmpresa?: Subscription;

  // Parâmetros de Emissão
  anoCompetencia: number = new Date().getFullYear();
  mesCompetencia: number = new Date().getMonth() + 1;
  incluirProjecaoCenarios: boolean = true;
  incluirAssinaturaContador: boolean = true;
  observacaoPersonalizada: string = '';

  // Estados de Loading e Mensagens
  gerandoPdf = false;
  baixandoExcel = false;
  mensagemSucesso: string | null = null;
  mensagemErro: string | null = null;

  // Pré-visualização Inline em PDF
  pdfPreviewUrl: SafeResourceUrl | null = null;
  private rawPdfUrl: string | null = null;

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

  constructor(
    private reportsService: ReportsService,
    private empresaContext: EmpresaContextService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.subEmpresa = this.empresaContext.empresaSelecionada$.subscribe((empresa) => {
      this.empresaSelecionada = empresa;
      this.limparMensagens();
      this.fecharPreviewPdf();
    });
  }

  ngOnDestroy(): void {
    if (this.subEmpresa) {
      this.subEmpresa.unsubscribe();
    }
    this.fecharPreviewPdf();
  }

  get empresaIdAtual(): string {
    if (!this.empresaSelecionada) return '';
    return this.empresaSelecionada.uuidId || String(this.empresaSelecionada.id ?? '');
  }

  get competenciaFormatada(): string {
    const mesStr = this.mesCompetencia < 10 ? `0${this.mesCompetencia}` : `${this.mesCompetencia}`;
    return `${this.anoCompetencia}-${mesStr}`;
  }

  gerarEVisualizarPdf(): void {
    const empId = this.empresaIdAtual;
    if (!empId) {
      this.mensagemErro = 'Selecione uma empresa antes de gerar o laudo.';
      return;
    }

    this.gerandoPdf = true;
    this.limparMensagens();

    const params: ParametrosEmissaoLaudoDTO = {
      empresaId: empId,
      competencia: this.competenciaFormatada,
      tipoLaudo: 'DOSSIE_COMPLETO',
      formato: 'PDF',
      incluirProjecaoCenarios: this.incluirProjecaoCenarios,
      incluirAssinaturaContador: this.incluirAssinaturaContador,
      observacaoPersonalizada: this.observacaoPersonalizada || undefined
    };

    this.reportsService.emitirLaudo(params).subscribe({
      next: (blob) => {
        this.gerandoPdf = false;
        this.fecharPreviewPdf();
        this.rawPdfUrl = URL.createObjectURL(blob);
        this.pdfPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.rawPdfUrl);
        this.mensagemSucesso = `Laudo PDF gerado com sucesso para a competência ${this.competenciaFormatada}!`;
      },
      error: (err) => {
        this.gerandoPdf = false;
        this.mensagemErro = 'Erro ao processar laudo pericial em PDF. Verifique se há dados de faturamento/folha.';
        console.error('Falha ao gerar PDF:', err);
      }
    });
  }

  baixarPdfDireto(): void {
    const empId = this.empresaIdAtual;
    if (!empId) {
      this.mensagemErro = 'Selecione uma empresa antes de baixar o laudo.';
      return;
    }

    this.gerandoPdf = true;
    this.limparMensagens();

    const params: ParametrosEmissaoLaudoDTO = {
      empresaId: empId,
      competencia: this.competenciaFormatada,
      tipoLaudo: 'DOSSIE_COMPLETO',
      formato: 'PDF',
      incluirProjecaoCenarios: this.incluirProjecaoCenarios,
      incluirAssinaturaContador: this.incluirAssinaturaContador,
      observacaoPersonalizada: this.observacaoPersonalizada || undefined
    };

    this.reportsService.emitirLaudo(params).subscribe({
      next: (blob) => {
        this.gerandoPdf = false;
        this.dispararDownload(blob, `laudo-fator-r-${this.competenciaFormatada}.pdf`);
        this.mensagemSucesso = 'Download do laudo em PDF concluído.';
      },
      error: (err) => {
        this.gerandoPdf = false;
        this.mensagemErro = 'Erro ao processar download do PDF.';
        console.error('Falha no download direto de PDF:', err);
      }
    });
  }

  baixarExcel(): void {
    const empId = this.empresaIdAtual;
    if (!empId) {
      this.mensagemErro = 'Selecione uma empresa para baixar a planilha.';
      return;
    }

    this.baixandoExcel = true;
    this.limparMensagens();

    const params: ParametrosEmissaoLaudoDTO = {
      empresaId: empId,
      competencia: this.competenciaFormatada,
      tipoLaudo: 'DOSSIE_COMPLETO',
      formato: 'EXCEL',
      incluirProjecaoCenarios: this.incluirProjecaoCenarios,
      incluirAssinaturaContador: this.incluirAssinaturaContador,
      observacaoPersonalizada: this.observacaoPersonalizada || undefined
    };

    this.reportsService.emitirLaudo(params).subscribe({
      next: (blob) => {
        this.baixandoExcel = false;
        this.dispararDownload(blob, `fator-r-${this.competenciaFormatada}.xlsx`);
        this.mensagemSucesso = 'Planilha Excel (.xlsx) baixada com sucesso com abas e fórmulas dinâmicas.';
      },
      error: (err) => {
        this.baixandoExcel = false;
        this.mensagemErro = 'Erro ao exportar planilha Excel.';
        console.error('Falha ao exportar Excel:', err);
      }
    });
  }

  fecharPreviewPdf(): void {
    if (this.rawPdfUrl) {
      URL.revokeObjectURL(this.rawPdfUrl);
      this.rawPdfUrl = null;
    }
    this.pdfPreviewUrl = null;
  }

  private dispararDownload(blob: Blob, nomeArquivo: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = nomeArquivo;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    setTimeout(() => URL.revokeObjectURL(url), 2000);
  }

  private limparMensagens(): void {
    this.mensagemSucesso = null;
    this.mensagemErro = null;
  }
}