import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subscription } from 'rxjs';
import { ReportsService } from '../../services/reports.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import { Empresa } from '../../models/models';
import {
  TipoLaudo,
  ParametrosEmissaoLaudoDTO,
  LaudoTributarioDTO,
  ValidacaoLaudoResponseDTO
} from '../../shared/types/reports.types';

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

  // Controle de Abas
  abaAtiva: 'emissao' | 'historico' | 'validacao' = 'emissao';

  // Parâmetros de Emissão
  anoCompetencia: number = new Date().getFullYear();
  mesCompetencia: number = new Date().getMonth() + 1;
  tipoLaudo: TipoLaudo = 'DOSSIE_COMPLETO';
  incluirProjecaoCenarios: boolean = true;
  incluirAssinaturaContador: boolean = true;
  observacaoPersonalizada: string = '';

  // Estados de Loading e Mensagens
  gerandoPdf = false;
  baixandoExcel = false;
  carregandoHistorico = false;
  validandoHash = false;
  mensagemSucesso: string | null = null;
  mensagemErro: string | null = null;

  // Pré-visualização Inline em PDF
  pdfPreviewUrl: SafeResourceUrl | null = null;
  private rawPdfUrl: string | null = null;

  // Histórico de Laudos
  historicoLaudos: LaudoTributarioDTO[] = [];

  // Auditoria e Validação de Autenticidade
  hashConsulta: string = '';
  resultadoValidacao: ValidacaoLaudoResponseDTO | null = null;
  consultaRealizada = false;

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

  tiposLaudo: { valor: TipoLaudo; label: string; descricao: string }[] = [
    {
      valor: 'DOSSIE_COMPLETO',
      label: 'Dossiê Tributário Completo',
      descricao: 'Laudo executivo, memória 12M, comparativo de regimes, parecer técnico e projeções'
    },
    {
      valor: 'LAUDO_ENQUADRAMENTO_FATOR_R',
      label: 'Laudo de Enquadramento do Fator R',
      descricao: 'Enquadramento específico Anexo III vs Anexo V com parecer do contador'
    },
    {
      valor: 'MEMORIA_CALCULO_12M',
      label: 'Memória Analítica de Cálculo (12 Meses)',
      descricao: 'Detalhamento mês a mês da janela deslizante com razão folha/faturamento'
    },
    {
      valor: 'ESTUDO_COMPARATIVO_REGIMES',
      label: 'Estudo Comparativo de Regimes',
      descricao: 'Simples Nacional vs Lucro Presumido e cálculo de economia tributária'
    }
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
      if (empresa) {
        this.carregarHistorico();
      } else {
        this.historicoLaudos = [];
      }
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

  setAba(aba: 'emissao' | 'historico' | 'validacao'): void {
    this.abaAtiva = aba;
    this.limparMensagens();
    if (aba === 'historico' && this.empresaSelecionada) {
      this.carregarHistorico();
    }
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
      tipoLaudo: this.tipoLaudo,
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
        this.carregarHistorico();
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
      tipoLaudo: this.tipoLaudo,
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
        this.carregarHistorico();
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
      tipoLaudo: this.tipoLaudo,
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
        this.carregarHistorico();
      },
      error: (err) => {
        this.baixandoExcel = false;
        this.mensagemErro = 'Erro ao exportar planilha Excel.';
        console.error('Falha ao exportar Excel:', err);
      }
    });
  }

  carregarHistorico(): void {
    const empId = this.empresaIdAtual;
    if (!empId) return;

    this.carregandoHistorico = true;
    this.reportsService.listarHistorico(empId).subscribe({
      next: (dados) => {
        this.carregandoHistorico = false;
        this.historicoLaudos = dados || [];
      },
      error: (err) => {
        this.carregandoHistorico = false;
        console.error('Erro ao listar histórico de laudos:', err);
      }
    });
  }

  baixarDoHistorico(laudo: LaudoTributarioDTO): void {
    this.reportsService.downloadLaudo(laudo.id).subscribe({
      next: (blob) => {
        this.dispararDownload(blob, `laudo-${laudo.competencia}-${laudo.tipoLaudo}.pdf`);
      },
      error: (err) => {
        this.mensagemErro = 'Erro ao baixar arquivo do histórico.';
        console.error(err);
      }
    });
  }

  validarHashAutenticidade(): void {
    if (!this.hashConsulta || !this.hashConsulta.trim()) {
      this.mensagemErro = 'Informe o código Hash SHA-256 para consultar.';
      return;
    }

    this.validandoHash = true;
    this.consultaRealizada = false;
    this.resultadoValidacao = null;
    this.limparMensagens();

    this.reportsService.validarAutenticidade(this.hashConsulta.trim()).subscribe({
      next: (res) => {
        this.validandoHash = false;
        this.consultaRealizada = true;
        this.resultadoValidacao = res;
      },
      error: (err) => {
        this.validandoHash = false;
        this.consultaRealizada = true;
        this.resultadoValidacao = {
          valido: false,
          hashAutenticidade: this.hashConsulta,
          dataEmissao: '',
          empresaNome: '',
          empresaCnpj: '',
          contadorResponsavel: '',
          crcContador: '',
          competencia: '',
          anexoEnquadrado: '',
          percentualFatorR: 0
        };
        console.error('Erro ao validar hash:', err);
      }
    });
  }

  copiarHashParaValidacao(hash: string): void {
    this.hashConsulta = hash;
    this.setAba('validacao');
    this.validarHashAutenticidade();
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
