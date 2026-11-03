import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  ContaReceberService,
  DadosNotaFiscalDTO
} from '../../services/conta-receber.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import {
  ContaReceberDTO,
  CreateContaReceberDTO,
  ClienteFornecedorDTO
} from '../../shared/types/receivables.types';

@Component({
  selector: 'app-contas-a-receber',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './contas-a-receber.html',
  styleUrls: ['./contas-a-receber.css']
})
export class ContasAReceberComponent implements OnInit, OnDestroy {
  private readonly service = inject(ContaReceberService);
  private readonly empresaContext = inject(EmpresaContextService);
  private readonly destroy$ = new Subject<void>();

  empresaId = '';
  empresaRazaoSocial = '';

  contas: ContaReceberDTO[] = [];
  clientes: ClienteFornecedorDTO[] = [];
  clientesMap = new Map<string, string>();

  loading = false;
  erro = '';
  sucesso = '';

  // Filtros
  status = '';
  clienteId = '';
  inicio = '';
  fim = '';
  pagina = 0;
  tamanho = 10;
  totalElementos = 0;
  totalPaginas = 0;

  // Modal Criação Manual
  mostrarModalCriacao = false;
  novaConta: CreateContaReceberDTO = this.formularioVazio();

  // Modal Importação XML com Preview
  mostrarModalXml = false;
  arquivoXml: File | null = null;
  carregandoPreview = false;
  importandoXml = false;
  previewDados: DadosNotaFiscalDTO | null = null;
  atualizarFaturamento = true;
  erroPreview = '';

  ngOnInit(): void {
    this.empresaContext.empresaSelecionada$
      .pipe(takeUntil(this.destroy$))
      .subscribe(empresa => {
        this.empresaId = empresa?.uuidId || (empresa?.id ? String(empresa.id) : '');
        this.empresaRazaoSocial = empresa?.razaoSocial || '';

        if (this.empresaId) {
          this.erro = '';
          this.pagina = 0;
          this.carregarClientes();
          this.carregarContas();
        } else {
          this.contas = [];
          this.clientes = [];
          this.clientesMap.clear();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  carregarClientes(): void {
    if (!this.empresaId) return;
    this.service.listarClientes(this.empresaId).subscribe({
      next: lista => {
        this.clientes = lista;
        this.clientesMap.clear();
        for (const c of lista) {
          this.clientesMap.set(c.id, c.nome);
        }
      },
      error: () => {
        // Silencioso ou log de clientes
      }
    });
  }

  carregarContas(): void {
    if (!this.empresaId) return;
    this.loading = true;
    this.erro = '';

    this.service.listar(this.empresaId, {
      status: this.status || undefined,
      clienteId: this.clienteId || undefined,
      inicio: this.inicio || undefined,
      fim: this.fim || undefined,
      pagina: this.pagina,
      tamanho: this.tamanho
    }).subscribe({
      next: res => {
        this.contas = res.content || [];
        this.totalPaginas = res.totalPages || 0;
        this.totalElementos = res.totalElements || 0;
        this.loading = false;
      },
      error: err => {
        this.erro = err.error?.message || 'Não foi possível carregar as contas a receber.';
        this.loading = false;
      }
    });
  }

  aplicarFiltros(): void {
    this.pagina = 0;
    this.carregarContas();
  }

  limparFiltros(): void {
    this.status = '';
    this.clienteId = '';
    this.inicio = '';
    this.fim = '';
    this.pagina = 0;
    this.carregarContas();
  }

  // --- Modal Criação Manual ---
  abrirModalCriacao(): void {
    this.novaConta = this.formularioVazio();
    this.mostrarModalCriacao = true;
    this.erro = '';
    this.sucesso = '';
  }

  fecharModalCriacao(): void {
    this.mostrarModalCriacao = false;
    this.novaConta = this.formularioVazio();
  }

  salvarConta(): void {
    if (!this.empresaId) {
      this.erro = 'Selecione uma empresa antes de cadastrar.';
      return;
    }
    if (!this.novaConta.clienteId || !this.novaConta.valorTotal || !this.novaConta.dataEmissao || !this.novaConta.dataVencimento) {
      this.erro = 'Preencha todos os campos obrigatórios.';
      return;
    }

    const payload: CreateContaReceberDTO = {
      ...this.novaConta,
      empresaId: this.empresaId
    };

    this.service.criar(payload).subscribe({
      next: () => {
        this.sucesso = 'Conta a receber cadastrada com sucesso.';
        this.fecharModalCriacao();
        this.carregarContas();
      },
      error: err => {
        this.erro = err.error?.message || 'Erro ao cadastrar conta a receber.';
      }
    });
  }

  // --- Baixa e Cancelamento ---
  baixarConta(conta: ContaReceberDTO): void {
    if (!confirm(`Deseja confirmar a baixa do título ${conta.numeroDocumento || conta.id}?`)) {
      return;
    }
    this.service.baixar(this.empresaId, conta.id).subscribe({
      next: () => {
        this.sucesso = 'Conta baixada com sucesso (PAGO).';
        this.carregarContas();
      },
      error: err => {
        this.erro = err.error?.message || 'Não foi possível efetuar a baixa.';
      }
    });
  }

  cancelarConta(conta: ContaReceberDTO): void {
    if (!confirm(`Deseja realmente cancelar o título ${conta.numeroDocumento || conta.id}?`)) {
      return;
    }
    this.service.cancelar(this.empresaId, conta.id).subscribe({
      next: () => {
        this.sucesso = 'Conta cancelada com sucesso.';
        this.carregarContas();
      },
      error: err => {
        this.erro = err.error?.message || 'Não foi possível cancelar a conta.';
      }
    });
  }

  // --- Modal Upload e Preview XML ---
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
    if (!this.arquivoXml || !this.empresaId) return;

    this.carregandoPreview = true;
    this.erroPreview = '';
    this.previewDados = null;

    this.service.previewXml(this.empresaId, this.arquivoXml).subscribe({
      next: dados => {
        this.previewDados = dados;
        this.carregandoPreview = false;
      },
      error: err => {
        this.carregandoPreview = false;
        this.erroPreview = err.error?.message || 'Erro ao processar pré-visualização do XML.';
      }
    });
  }

  confirmarImportacaoXml(): void {
    if (!this.arquivoXml || !this.empresaId) return;

    this.importandoXml = true;
    this.erroPreview = '';

    this.service.importarXml(this.empresaId, this.arquivoXml, this.atualizarFaturamento).subscribe({
      next: titulos => {
        this.importandoXml = false;
        const total = titulos.length;
        this.sucesso = `Nota fiscal importada com sucesso: ${total} título(s) gerado(s).`;
        this.fecharModalXml();
        this.carregarClientes();
        this.carregarContas();
      },
      error: err => {
        this.importandoXml = false;
        this.erroPreview = err.error?.message || 'Erro ao importar XML da nota fiscal.';
      }
    });
  }

  // --- Paginação ---
  paginaAnterior(): void {
    if (this.pagina > 0) {
      this.pagina--;
      this.carregarContas();
    }
  }

  proximaPagina(): void {
    if (this.pagina + 1 < this.totalPaginas) {
      this.pagina++;
      this.carregarContas();
    }
  }

  irParaPagina(p: number): void {
    if (p >= 0 && p < this.totalPaginas) {
      this.pagina = p;
      this.carregarContas();
    }
  }

  // --- Utilitários ---
  obterNomeCliente(clienteId: string): string {
    return this.clientesMap.get(clienteId) || clienteId || '-';
  }

  private formularioVazio(): CreateContaReceberDTO {
    const hoje = new Date().toISOString().substring(0, 10);
    return {
      empresaId: this.empresaId,
      clienteId: '',
      numeroDocumento: '',
      valorTotal: 0,
      dataEmissao: hoje,
      dataVencimento: hoje
    };
  }
}
