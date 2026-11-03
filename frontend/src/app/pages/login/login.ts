import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { unmaskCpf, maskCpf } from '../../shared/utils/formatters';
import { maskCnpj, unmaskCnpj } from '../../shared/utils/cnpj';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  abaAtiva: 'login' | 'cadastro' | 'recuperar' = 'login';
  perfilCadastro: 'CONTADOR' | 'EMPRESARIO' | 'COLABORADOR' = 'CONTADOR';

  // Login
  loginInput = '';
  senha = '';

  // Cadastro Contador
  contador = {
    nome: '',
    email: '',
    cpf: '',
    senha: '',
    nomeOrganizacao: '',
    cnpj: '',
    crc: ''
  };

  // Cadastro Empresário
  empresario = {
    nome: '',
    email: '',
    cpf: '',
    senha: '',
    razaoSocial: '',
    nomeFantasia: '',
    cnpj: ''
  };

  // Cadastro Colaborador
  colaborador = {
    nome: '',
    email: '',
    cpf: '',
    senha: '',
    empresaId: '',
    funcao: ''
  };

  // Recuperação de Senha
  recuperar = {
    identificador: '',
    token: '',
    novaSenha: '',
    etapa: 'solicitar' as 'solicitar' | 'redefinir'
  };

  erro = '';
  sucesso = '';
  carregando = false;

  constructor(private authService: AuthService, private router: Router) {}

  trocarAba(aba: 'login' | 'cadastro' | 'recuperar') {
    this.abaAtiva = aba;
    this.erro = '';
    this.sucesso = '';
  }

  aplicarMascaraCPF(event: any, targetObj?: any, propName?: string) {
    const v = maskCpf(event.target.value);
    event.target.value = v;
    if (targetObj && propName) {
      targetObj[propName] = v;
    } else {
      this.loginInput = v;
    }
  }

  aplicarMascaraCNPJ(event: any, targetObj: any, propName: string) {
    const v = maskCnpj(event.target.value);
    event.target.value = v;
    targetObj[propName] = v;
  }

  fazerLogin() {
    if (!this.loginInput || !this.senha) {
      this.erro = 'Por favor, preencha o login e a senha.';
      return;
    }

    this.carregando = true;
    this.erro = '';
    this.sucesso = '';

    const clean = this.loginInput.includes('@') ? this.loginInput.trim() : unmaskCpf(this.loginInput);

    this.authService.login(clean, this.senha).subscribe({
      next: () => {
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.erro = err.error?.error || 'CPF/E-mail ou senha incorretos.';
        this.carregando = false;
      }
    });
  }

  cadastrarContador() {
    if (!this.contador.nome || !this.contador.email || !this.contador.cpf || !this.contador.senha
        || !this.contador.nomeOrganizacao || !this.contador.cnpj || !this.contador.crc) {
      this.erro = 'Preencha todos os campos obrigatórios do Contador e Organização Contábil.';
      return;
    }

    this.carregando = true;
    this.erro = '';
    this.sucesso = '';

    this.authService.registerContador({
      nome: this.contador.nome,
      email: this.contador.email,
      cpf: unmaskCpf(this.contador.cpf),
      senha: this.contador.senha,
      nomeOrganizacao: this.contador.nomeOrganizacao,
      cnpj: unmaskCnpj(this.contador.cnpj),
      crc: this.contador.crc
    }).subscribe({
      next: () => {
        this.sucesso = 'Organização Contábil e Contador cadastrados com sucesso! Redirecionando...';
        setTimeout(() => this.router.navigate(['/home']), 1200);
      },
      error: (err) => {
        this.erro = err.error?.error || 'Erro ao realizar cadastro do Contador.';
        this.carregando = false;
      }
    });
  }

  cadastrarEmpresario() {
    if (!this.empresario.nome || !this.empresario.email || !this.empresario.cpf || !this.empresario.senha
        || !this.empresario.razaoSocial || !this.empresario.cnpj) {
      this.erro = 'Preencha todos os campos obrigatórios do Empresário e da Empresa.';
      return;
    }

    this.carregando = true;
    this.erro = '';
    this.sucesso = '';

    this.authService.registerEmpresario({
      nome: this.empresario.nome,
      email: this.empresario.email,
      cpf: unmaskCpf(this.empresario.cpf),
      senha: this.empresario.senha,
      razaoSocial: this.empresario.razaoSocial,
      nomeFantasia: this.empresario.nomeFantasia,
      cnpj: unmaskCnpj(this.empresario.cnpj)
    }).subscribe({
      next: () => {
        this.sucesso = 'Empresário e Empresa cadastrados com sucesso! Redirecionando...';
        setTimeout(() => this.router.navigate(['/home']), 1200);
      },
      error: (err) => {
        this.erro = err.error?.error || 'Erro ao realizar cadastro do Empresário.';
        this.carregando = false;
      }
    });
  }

  cadastrarColaborador() {
    if (!this.colaborador.nome || !this.colaborador.cpf || !this.colaborador.senha || !this.colaborador.empresaId) {
      this.erro = 'Preencha os campos obrigatórios: Nome, CPF, Senha e Identificador/CNPJ da Empresa.';
      return;
    }

    this.carregando = true;
    this.erro = '';
    this.sucesso = '';

    this.authService.registerUsuario({
      nome: this.colaborador.nome,
      email: this.colaborador.email,
      cpf: unmaskCpf(this.colaborador.cpf),
      senha: this.colaborador.senha,
      empresaId: this.colaborador.empresaId.trim(),
      funcao: this.colaborador.funcao || 'Colaborador'
    }).subscribe({
      next: () => {
        this.sucesso = 'Cadastro realizado com sucesso! Redirecionando...';
        setTimeout(() => this.router.navigate(['/home']), 1200);
      },
      error: (err) => {
        this.erro = err.error?.error || 'Erro ao realizar cadastro do Colaborador.';
        this.carregando = false;
      }
    });
  }

  solicitarRecuperacao() {
    if (!this.recuperar.identificador) {
      this.erro = 'Informe seu CPF ou E-mail cadastrado.';
      return;
    }

    this.carregando = true;
    this.erro = '';
    this.sucesso = '';

    this.authService.forgotPassword(this.recuperar.identificador).subscribe({
      next: (res) => {
        this.carregando = false;
        this.sucesso = res.message || 'Código/Token de recuperação gerado.';
        if (res.resetPasswordToken) {
          this.recuperar.token = res.resetPasswordToken;
        }
        this.recuperar.etapa = 'redefinir';
      },
      error: (err) => {
        this.erro = err.error?.error || 'Usuário não encontrado.';
        this.carregando = false;
      }
    });
  }

  redefinirSenha() {
    if (!this.recuperar.token || !this.recuperar.novaSenha) {
      this.erro = 'Informe o token recebido e a nova senha.';
      return;
    }

    this.carregando = true;
    this.erro = '';
    this.sucesso = '';

    this.authService.resetPassword(this.recuperar.token, this.recuperar.novaSenha).subscribe({
      next: (res) => {
        this.carregando = false;
        this.sucesso = res.message || 'Senha redefinida com sucesso! Você já pode entrar.';
        this.recuperar.etapa = 'solicitar';
        setTimeout(() => this.trocarAba('login'), 2000);
      },
      error: (err) => {
        this.erro = err.error?.error || 'Token inválido ou expirado.';
        this.carregando = false;
      }
    });
  }
}
