import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  ContaReceberService,
  DadosNotaFiscalDTO,
  ItemPreviewXmlLoteDTO
} from '../../services/conta-receber.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import {
  ContaReceberDTO,
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

  // Modal Importação XML em Lote
  mostrarModalXml = false;
  arquivosXml: File[] = [];
  carregandoPreview = false;
  importandoXml = false;
  itensPreviewLote: ItemPreviewXmlLoteDTO[] = [];
  erroPreview = '';

  get totalArquivos(): number {
    return this.arquivosXml.length;
  }

  get totalValidas(): number {
    return this.itensPreviewLote.filter(i => i.valido).length;
  }

  get totalErrosOuBloqueios(): number {
    return this.itensPreviewLote.filter(i => !i.valido).length;
  }

  get valorTotalValidas(): number {
    return this.itensPreviewLote
      .filter(i => i.valido && i.dados?.valorTotal)
      .reduce((acc, curr) => acc + (curr.dados?.valorTotal || 0), 0);
  }

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
        // Silencioso
      }
    });
  }

  carregarContas(): void {
    if (!this.empresaId) return;
    this.loading = true;
    this.erro = '';

    const filtros = {
      status: this.status || undefined,
      clienteId: this.clienteId || undefined,
      inicio: this.inicio || undefined,
      fim: this.fim || undefined,
      pagina: this.pagina,
      tamanho: this.tamanho
    };

    this.service.listar(this.empresaId, filtros).subscribe({
      next: pag => {
        this.loading = false;
        this.contas = pag.content || [];
        this.totalElementos = pag.totalElements || 0;
        this.totalPaginas = pag.totalPages || 0;
      },
      error: err => {
        this.loading = false;
        this.erro = err.error?.message || err.error?.erro || 'Erro ao carregar contas a receber.';
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

  // --- Ações de Contas a Receber ---
  baixarConta(conta: ContaReceberDTO): void {
    if (!this.empresaId || !conta.id) return;
    if (!confirm(`Deseja confirmar o recebimento do título ${conta.numeroDocumento || conta.id}?`)) {
      return;
    }

    this.service.baixar(this.empresaId, conta.id).subscribe({
      next: () => {
        this.sucesso = `Título ${conta.numeroDocumento || conta.id} recebido com sucesso.`;
        this.carregarContas();
      },
      error: err => {
        this.erro = err.error?.message || err.error?.erro || 'Erro ao baixar título.';
      }
    });
  }

  cancelarConta(conta: ContaReceberDTO): void {
    if (!this.empresaId || !conta.id) return;
    if (!confirm(`Deseja cancelar o título ${conta.numeroDocumento || conta.id}?`)) {
      return;
    }

    this.service.cancelar(this.empresaId, conta.id).subscribe({
      next: () => {
        this.sucesso = `Título cancelado com sucesso.`;
        this.carregarContas();
      },
      error: err => {
        this.erro = err.error?.message || err.error?.erro || 'Erro ao cancelar título.';
      }
    });
  }

  cancelarNotaFiscal(conta: ContaReceberDTO): void {
    if (!this.empresaId || !conta.id) return;
    const msg = conta.xmlChaveAcesso
      ? `Atenção: Cancelar esta NF cancelará todas as parcelas e abaterá o valor do Faturamento Mensal. Deseja prosseguir?`
      : `Deseja cancelar esta nota fiscal?`;

    if (!confirm(msg)) {
      return;
    }

    this.service.cancelarNf(this.empresaId, conta.id).subscribe({
      next: () => {
        this.sucesso = `Nota fiscal cancelada e faturamento estornado com sucesso.`;
        this.carregarContas();
      },
      error: err => {
        this.erro = err.error?.message || err.error?.erro || 'Não foi possível cancelar a nota fiscal.';
      }
    });
  }

  // --- Modal Upload e Preview XML em Lote ---
  abrirModalXml(): void {
    this.mostrarModalXml = true;
    this.arquivosXml = [];
    this.itensPreviewLote = [];
    this.erroPreview = '';
    this.carregandoPreview = false;
    this.importandoXml = false;
  }

  fecharModalXml(): void {
    this.mostrarModalXml = false;
    this.arquivosXml = [];
    this.itensPreviewLote = [];
    this.erroPreview = '';
    this.carregandoPreview = false;
    this.importandoXml = false;
  }

  selecionarArquivosXml(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.arquivosXml = Array.from(input.files);
      this.obterPreviewLote();
    }
  }

  removerArquivo(index: number): void {
    if (index >= 0 && index < this.arquivosXml.length) {
      this.arquivosXml.splice(index, 1);
      this.itensPreviewLote.splice(index, 1);
      if (this.arquivosXml.length === 0) {
        this.itensPreviewLote = [];
        this.erroPreview = '';
      }
    }
  }

  obterPreviewLote(): void {
    if (this.arquivosXml.length === 0 || !this.empresaId) return;

    this.carregandoPreview = true;
    this.erroPreview = '';
    this.itensPreviewLote = [];

    this.service.previewXmlLote(this.empresaId, this.arquivosXml).subscribe({
      next: itens => {
        this.itensPreviewLote = itens;
        this.carregandoPreview = false;
      },
      error: err => {
        this.carregandoPreview = false;
        this.erroPreview = err.error?.message || 'Erro ao processar pré-visualização dos arquivos XML.';
      }
    });
  }

  confirmarImportacaoXml(): void {
    if (this.arquivosXml.length === 0 || !this.empresaId) return;

    // Filtra apenas os arquivos válidos
    const nomesValidos = new Set(
      this.itensPreviewLote
        .filter(item => item.valido)
        .map(item => item.nomeArquivo)
    );

    const arquivosAImportar = this.arquivosXml.filter(arq => nomesValidos.has(arq.name));

    if (arquivosAImportar.length === 0) {
      this.erroPreview = 'Nenhuma nota fiscal válida para importar no lote.';
      return;
    }

    this.importandoXml = true;
    this.erroPreview = '';

    this.service.importarXmlLote(this.empresaId, arquivosAImportar).subscribe({
      next: res => {
        this.importandoXml = false;
        const msgSucesso = `Importação em lote concluída: ${res.totalImportados} nota(s) importada(s) com sucesso. ${res.titulosGerados.length} título(s) gerado(s) e Faturamento Mensal atualizado!`;
        this.sucesso = msgSucesso;
        if (res.mensagensErro && res.mensagensErro.length > 0) {
          this.erro = `Algumas notas tiveram restrições: ${res.mensagensErro.join('; ')}`;
        }
        this.fecharModalXml();
        this.carregarClientes();
        this.carregarContas();
      },
      error: err => {
        this.importandoXml = false;
        this.erroPreview = err.error?.message || err.error?.erro || 'Erro ao importar lote de notas fiscais.';
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
}
