import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EmpresaService } from '../../services/empresa.service';
import { AuthService } from '../../services/auth.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import { UsuarioService, Usuario, NovoUsuarioEmpresaDTO } from '../../services/usuario.service';
import { Empresa } from '../../models/models';

@Component({
  selector: 'app-empresas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './empresas.html',
  styleUrls: ['./empresas.css']
})
export class EmpresasComponent implements OnInit {
  empresas: Empresa[] = [];
  isAdmin = false;
  erro: string | null = null;
  sucesso: string | null = null;

  // --- MODAL EMPRESA ---
  mostrarModalEmpresa = false;
  novaEmpresaForm: Empresa = { razaoSocial: '', cnpj: '', dataInicioAtividade: '' };

  // --- MODAL USUÁRIO ---
  mostrarModalUsuario = false;
  empresaSelecionadaParaAcesso: Empresa | null = null;
  novoUsuarioForm: NovoUsuarioEmpresaDTO = { cpf: '', senha: '', empresaId: 0 };
  usuariosVinculados: Usuario[] = [];

  constructor(
    private empresaService: EmpresaService,
    private authService: AuthService,
    private empresaContext: EmpresaContextService,
    private usuarioService: UsuarioService
  ) {}

  ngOnInit() {
    this.isAdmin = this.authService.isAdmin();
    this.carregarEmpresas();
  }

  carregarEmpresas() {
    this.empresaService.listarTodas().subscribe({
      next: (dados: any) => {
        this.empresas = dados;
        this.empresaContext.setEmpresas(dados);
      },
      error: () => this.erro = 'Erro ao carregar lista de empresas.'
    });
  }

  abrirModalEmpresa(): void {
    this.novaEmpresaForm = { razaoSocial: '', cnpj: '', dataInicioAtividade: '' };
    this.mostrarModalEmpresa = true;
  }

  fecharModalEmpresa(): void {
    this.mostrarModalEmpresa = false;
  }

  aplicarMascaraCNPJ(event: any): void {
    let valor = event.target.value.replace(/\D/g, '');
    if (valor.length > 14) valor = valor.substring(0, 14);
    valor = valor.replace(/^(\d{2})(\d)/, '$1.$2');
    valor = valor.replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3');
    valor = valor.replace(/\.(\d{3})(\d)/, '.$1/$2');
    valor = valor.replace(/(\d{4})(\d)/, '$1-$2');
    this.novaEmpresaForm.cnpj = valor;
  }

  salvarNovaEmpresa(): void {
    if (!this.novaEmpresaForm.razaoSocial || !this.novaEmpresaForm.cnpj) return;
    
    const empresaParaSalvar = { ...this.novaEmpresaForm };
    if (empresaParaSalvar.dataInicioAtividade && empresaParaSalvar.dataInicioAtividade.length === 7) {
      empresaParaSalvar.dataInicioAtividade = empresaParaSalvar.dataInicioAtividade + '-01';
    }

    this.empresaService.criar(empresaParaSalvar).subscribe({
      next: (novaEmpresa) => {
        this.sucesso = 'Empresa cadastrada com sucesso!';
        this.carregarEmpresas();
        this.fecharModalEmpresa();
        setTimeout(() => this.sucesso = null, 3000);
      },
      error: () => this.erro = 'Erro ao cadastrar nova empresa.'
    });
  }

  excluirEmpresa(empresa: Empresa): void {
    if (!this.isAdmin) return;
    if (confirm(`Tem certeza que deseja excluir a empresa ${empresa.razaoSocial}? Esta ação não pode ser desfeita e removerá todos os dados associados.`)) {
      this.empresaService.excluir(empresa.id!).subscribe({
        next: () => {
          this.sucesso = 'Empresa excluída com sucesso!';
          this.carregarEmpresas();
          setTimeout(() => this.sucesso = null, 3000);
        },
        error: () => this.erro = 'Erro ao excluir empresa.'
      });
    }
  }

  abrirModalUsuario(empresa: Empresa): void {
    this.empresaSelecionadaParaAcesso = empresa;
    this.novoUsuarioForm = { cpf: '', senha: '', empresaId: empresa.id! };
    this.carregarUsuariosDaEmpresa(empresa.id!);
    this.mostrarModalUsuario = true;
  }

  carregarUsuariosDaEmpresa(empresaId: number): void {
    this.usuarioService.listarPorEmpresa(empresaId).subscribe({
      next: (usuarios) => this.usuariosVinculados = usuarios,
      error: () => this.erro = 'Erro ao carregar usuários vinculados.'
    });
  }

  desvincularUsuario(usuarioId: number): void {
    if (!this.empresaSelecionadaParaAcesso) return;
    this.usuarioService.desvincularEmpresa(usuarioId, this.empresaSelecionadaParaAcesso.id!).subscribe({
      next: () => {
        this.sucesso = 'Usuário desvinculado com sucesso.';
        this.carregarUsuariosDaEmpresa(this.empresaSelecionadaParaAcesso!.id!);
        setTimeout(() => this.sucesso = null, 3000);
      },
      error: () => this.erro = 'Erro ao desvincular usuário.'
    });
  }

  fecharModalUsuario(): void {
    this.mostrarModalUsuario = false;
    this.empresaSelecionadaParaAcesso = null;
  }

  aplicarMascaraCPF(event: any): void {
    let valor = event.target.value.replace(/\D/g, '');
    if (valor.length > 11) valor = valor.substring(0, 11);
    valor = valor.replace(/(\d{3})(\d)/, '$1.$2');
    valor = valor.replace(/(\d{3})\.(\d{3})(\d)/, '$1.$2.$3');
    valor = valor.replace(/(\d{3})\.(\d{3})\.(\d{3})(\d{1,2})$/, '$1.$2.$3-$4');
    this.novoUsuarioForm.cpf = valor;
  }

  vincularNovoUsuario(): void {
    if (!this.novoUsuarioForm.cpf || !this.novoUsuarioForm.senha) return;
    
    const cpfLimpo = this.novoUsuarioForm.cpf.replace(/\D/g, '');
    const dtoParaSalvar = { ...this.novoUsuarioForm, cpf: cpfLimpo };

    this.usuarioService.vincularEmpresa(dtoParaSalvar).subscribe({
      next: () => {
        this.sucesso = 'Acesso vinculado com sucesso!';
        this.carregarUsuariosDaEmpresa(this.empresaSelecionadaParaAcesso!.id!);
        this.novoUsuarioForm.cpf = '';
        this.novoUsuarioForm.senha = '';
        setTimeout(() => this.sucesso = null, 3000);
      },
      error: () => this.erro = 'Erro ao vincular acesso (CPF já existe?).'
    });
  }
}
