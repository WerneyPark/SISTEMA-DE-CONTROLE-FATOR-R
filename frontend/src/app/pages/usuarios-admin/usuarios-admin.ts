import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsuarioService, VinculoPendente } from '../../services/usuario.service';
import { EmpresaService } from '../../services/empresa.service';
import { Empresa, UsuarioAdmin } from '../../models/models';
import { maskCpf, unmaskCpf } from '../../shared/utils/formatters';
import { maskCnpj } from '../../shared/utils/cnpj';

@Component({
  selector: 'app-usuarios-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './usuarios-admin.html',
  styleUrls: ['./usuarios-admin.css']
})
export class UsuariosAdminComponent implements OnInit {
  usuarios: UsuarioAdmin[] = [];
  vinculosPendentes: VinculoPendente[] = [];
  empresas: Empresa[] = [];

  loadingUsuarios = true;
  loadingPendentes = true;
  loadingEmpresas = false;

  // Formulário de Pré-Cadastro por CPF
  conviteCpf = '';
  conviteFuncao = '';
  conviteEmpresaId: string | number = '';
  convidando = false;
  mensagemConviteSucesso = '';
  mensagemConviteErro = '';

  // Mensagens gerais de ação
  mensagemAcaoSucesso = '';
  mensagemAcaoErro = '';

  constructor(
    private usuarioService: UsuarioService,
    private empresaService: EmpresaService
  ) {}

  ngOnInit() {
    this.carregarDados();
  }

  carregarDados() {
    this.carregarUsuarios();
    this.carregarVinculosPendentes();
    this.carregarEmpresas();
  }

  carregarUsuarios() {
    this.loadingUsuarios = true;
    this.usuarioService.listarTodos().subscribe({
      next: (dados) => {
        this.usuarios = dados;
        this.loadingUsuarios = false;
      },
      error: (err) => {
        console.error('Erro ao carregar usuários', err);
        this.loadingUsuarios = false;
      }
    });
  }

  carregarVinculosPendentes() {
    this.loadingPendentes = true;
    this.usuarioService.listarVinculosPendentes().subscribe({
      next: (dados) => {
        this.vinculosPendentes = dados;
        this.loadingPendentes = false;
      },
      error: (err) => {
        console.error('Erro ao carregar vínculos pendentes', err);
        this.loadingPendentes = false;
      }
    });
  }

  carregarEmpresas() {
    this.loadingEmpresas = true;
    this.empresaService.listarTodas().subscribe({
      next: (dados) => {
        this.empresas = dados;
        this.loadingEmpresas = false;
      },
      error: (err) => {
        console.error('Erro ao carregar empresas', err);
        this.loadingEmpresas = false;
      }
    });
  }

  aplicarMascaraCPF(event: any) {
    const v = maskCpf(event.target.value);
    this.conviteCpf = v;
    event.target.value = v;
  }

  formatarCpf(cpf: string): string {
    return maskCpf(cpf);
  }

  formatarCnpj(cnpj: string): string {
    return maskCnpj(cnpj);
  }

  enviarConvitePreCadastro() {
    if (!this.conviteCpf || !this.conviteEmpresaId) {
      this.mensagemConviteErro = 'Preencha o CPF e selecione uma empresa para o pré-cadastro.';
      return;
    }

    this.convidando = true;
    this.mensagemConviteSucesso = '';
    this.mensagemConviteErro = '';

    const cleanCpf = unmaskCpf(this.conviteCpf);

    this.usuarioService.convidarCpf({
      cpf: cleanCpf,
      funcao: this.conviteFuncao || 'Colaborador',
      empresaId: this.conviteEmpresaId
    }).subscribe({
      next: () => {
        this.convidando = false;
        this.mensagemConviteSucesso = `Usuário com CPF ${this.conviteCpf} pré-cadastrado com sucesso!`;
        this.conviteCpf = '';
        this.conviteFuncao = '';
        this.conviteEmpresaId = '';
        this.carregarUsuarios();
        this.carregarVinculosPendentes();
      },
      error: (err) => {
        this.convidando = false;
        this.mensagemConviteErro = err.error?.error || 'Erro ao pré-cadastrar usuário.';
      }
    });
  }

  aprovarVinculo(vinculo: VinculoPendente) {
    this.mensagemAcaoSucesso = '';
    this.mensagemAcaoErro = '';

    this.usuarioService.atualizarStatusVinculo(vinculo.id, 'APROVADO').subscribe({
      next: () => {
        this.mensagemAcaoSucesso = `Vínculo de ${vinculo.usuarioNome} com a empresa ${vinculo.empresaNome} aprovado com sucesso!`;
        this.carregarVinculosPendentes();
        this.carregarUsuarios();
      },
      error: (err) => {
        this.mensagemAcaoErro = err.error?.error || 'Erro ao aprovar vínculo.';
      }
    });
  }

  rejeitarVinculo(vinculo: VinculoPendente) {
    if (!confirm(`Deseja realmente rejeitar a solicitação de vínculo de ${vinculo.usuarioNome}?`)) {
      return;
    }

    this.mensagemAcaoSucesso = '';
    this.mensagemAcaoErro = '';

    this.usuarioService.atualizarStatusVinculo(vinculo.id, 'REJEITADO').subscribe({
      next: () => {
        this.mensagemAcaoSucesso = `Solicitação de vínculo de ${vinculo.usuarioNome} rejeitada.`;
        this.carregarVinculosPendentes();
        this.carregarUsuarios();
      },
      error: (err) => {
        this.mensagemAcaoErro = err.error?.error || 'Erro ao rejeitar vínculo.';
      }
    });
  }
}
