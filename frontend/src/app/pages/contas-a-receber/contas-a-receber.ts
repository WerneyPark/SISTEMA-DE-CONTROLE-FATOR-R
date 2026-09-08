import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ContaReceberService } from '../../services/conta-receber.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import { ContaReceberDTO, CreateContaReceberDTO } from '../../shared/types/receivables.types';

@Component({
  selector: 'app-contas-a-receber',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './contas-a-receber.html',
  styleUrls: ['./contas-a-receber.css']
})
export class ContasAReceberComponent implements OnInit {
  private readonly service = inject(ContaReceberService);
  private readonly empresaContext = inject(EmpresaContextService);
  contas: ContaReceberDTO[] = [];
  empresaId = '';
  erro = '';
  sucesso = '';
  status = '';
  inicio = '';
  fim = '';
  pagina = 0;
  totalPaginas = 0;
  arquivo: File | null = null;
  mostrarFormulario = false;
  novaConta: CreateContaReceberDTO = this.formularioVazio();

  ngOnInit(): void {
    const empresa = this.empresaContext.getEmpresaSelecionada();
    this.empresaId = String(empresa?.id || '');
    if (this.empresaId) this.carregar();
  }

  carregar(): void {
    this.service.listar(this.empresaId, {
      pagina: this.pagina, tamanho: 20, status: this.status,
      inicio: this.inicio, fim: this.fim
    }).subscribe({
      next: resultado => {
        this.contas = resultado.content;
        this.totalPaginas = resultado.totalPages;
      },
      error: () => this.erro = 'Não foi possível carregar as contas a receber.'
    });
  }

  salvar(): void {
    this.service.criar({ ...this.novaConta, empresaId: this.empresaId }).subscribe({
      next: () => {
        this.sucesso = 'Conta criada com sucesso.';
        this.mostrarFormulario = false;
        this.novaConta = this.formularioVazio();
        this.carregar();
      },
      error: () => this.erro = 'Não foi possível criar a conta.'
    });
  }

  baixar(conta: ContaReceberDTO): void {
    this.service.baixar(this.empresaId, conta.id).subscribe({
      next: () => { this.sucesso = 'Conta baixada como paga.'; this.carregar(); },
      error: () => this.erro = 'Não foi possível efetuar a baixa.'
    });
  }

  selecionarArquivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.arquivo = input.files?.[0] || null;
  }

  importar(): void {
    if (!this.arquivo) return;
    this.service.importarXml(this.empresaId, this.arquivo).subscribe({
      next: () => { this.sucesso = 'XML importado com sucesso.'; this.arquivo = null; this.carregar(); },
      error: () => this.erro = 'Não foi possível importar o XML.'
    });
  }

  paginaAnterior(): void { if (this.pagina > 0) { this.pagina--; this.carregar(); } }
  proximaPagina(): void { if (this.pagina + 1 < this.totalPaginas) { this.pagina++; this.carregar(); } }

  private formularioVazio(): CreateContaReceberDTO {
    return { empresaId: '', clienteId: '', numeroDocumento: '', valorTotal: 0,
      dataEmissao: '', dataVencimento: '' };
  }
}