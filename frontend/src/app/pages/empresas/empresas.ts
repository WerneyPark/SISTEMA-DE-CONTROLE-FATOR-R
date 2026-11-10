import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EmpresaService, CriarEmpresaDTO } from '../../services/empresa.service';
import { AuthService, ContadorResumo } from '../../services/auth.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import { UsuarioService, Usuario, NovoUsuarioEmpresaDTO, ConviteEmpresa } from '../../services/usuario.service';
import { Empresa } from '../../models/models';
import { maskCnpj, unmaskCnpj } from '../../shared/utils/cnpj';
import { maskCpf, unmaskCpf } from '../../shared/utils/formatters';

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
  isEmpresario = false;
  podeCriarEmpresa = false;

  erro: string | null = null;
  sucesso: string | null = null;

  // --- CONVITES PENDENTES (Empresário) ---
  convitesPendentes: ConviteEmpresa[] = [];
  processandoConviteId: string | null = null;

  // --- MODAL EMPRESA ---
  mostrarModalEmpresa = false;
  novaEmpresaForm = {
    razaoSocial: '',
    nomeFantasia: '',
    cnpj: '',
    dataInicioAtividade: '',
    cpfEmpresario: '',
    nomeEmpresario: '',
    contadorIdentificador: ''
  };
  contadorManual = '';
  contadores: ContadorResumo[] = [];

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
    this.isEmpresario = this.authService.isEmpresario();
    this.podeCriarEmpresa = this.isAdmin || this.isEmpresario;

    this.carregarEmpresas();
    this.carregarConvitesPendentes();
    this.carregarContadores();
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

  carregarConvitesPendentes() {
    if (this.isEmpresario) {
      this.usuarioService.listarMeusConvites().subscribe({
        next: (convites) => this.convitesPendentes = convites,
        error: () => this.convitesPendentes = []
      });
    }
  }

  carregarContadores() {
    this.authService.listarContadores().subscribe({
      next: (dados) => {
        this.contadores = dados;
        if (dados.length > 0 && !this.novaEmpresaForm.contadorIdentificador) {
          this.novaEmpresaForm.contadorIdentificador = dados[0].id;
        }
      },
      error: () => {}
    });
  }

  aceitarConvite(convite: ConviteEmpresa) {
    this.processandoConviteId = convite.vinculoId;
    this.usuarioService.responderConvite(convite.vinculoId, true).subscribe({
      next: () => {
        this.processandoConviteId = null;
        this.sucesso = 'Empresa e contador aceitos com sucesso!';
        this.carregarConvitesPendentes();
        this.carregarEmpresas();
        setTimeout(() => this.sucesso = null, 3000);
      },
      error: (err) => {
        this.processandoConviteId = null;
        this.erro = err.error?.error || 'Erro ao aceitar convite.';
      }
    });
  }

  recusarConvite(convite: ConviteEmpresa) {
    if (confirm(`Deseja realmente recusar o vínculo com a empresa ${convite.razaoSocial} e o contador ${convite.nomeContador}?`)) {
      this.processandoConviteId = convite.vinculoId;
      this.usuarioService.responderConvite(convite.vinculoId, false).subscribe({
        next: () => {
          this.processandoConviteId = null;
          this.sucesso = 'Convite recusado.';
          this.carregarConvitesPendentes();
          setTimeout(() => this.sucesso = null, 3000);
        },
        error: (err) => {
          this.processandoConviteId = null;
          this.erro = err.error?.error || 'Erro ao recusar convite.';
        }
      });
    }
  }

  abrirModalEmpresa(): void {
    this.novaEmpresaForm = {
      razaoSocial: '',
      nomeFantasia: '',
      cnpj: '',
      dataInicioAtividade: '',
      cpfEmpresario: '',
      nomeEmpresario: '',
      contadorIdentificador: this.contadores.length > 0 ? this.contadores[0].id : ''
    };
    this.contadorManual = '';
    this.mostrarModalEmpresa = true;
  }

  fecharModalEmpresa(): void {
    this.mostrarModalEmpresa = false;
  }

  aplicarMascaraCNPJ(event: any): void {
    this.novaEmpresaForm.cnpj = maskCnpj(event.target.value);
  }

  aplicarMascaraCPFEmpresario(event: any): void {
    this.novaEmpresaForm.cpfEmpresario = maskCpf(event.target.value);
  }

  onContadorManualChange(): void {
    if (this.contadorManual) {
      this.novaEmpresaForm.contadorIdentificador = this.contadorManual.trim();
    }
  }

  salvarNovaEmpresa(): void {
    if (!this.novaEmpresaForm.razaoSocial || !this.novaEmpresaForm.cnpj) {
      this.erro = 'Preencha a Razão Social e o CNPJ da empresa.';
      return;
    }

    if (this.isAdmin && !this.novaEmpresaForm.cpfEmpresario) {
      this.erro = 'O Contador deve informar o CPF do Empresário responsável pela empresa.';
      return;
    }

    let contadorFinal = this.novaEmpresaForm.contadorIdentificador;
    if (this.isEmpresario) {
      if (this.novaEmpresaForm.contadorIdentificador === 'OUTRO') {
        contadorFinal = this.contadorManual.trim();
      }
      if (!contadorFinal) {
        this.erro = 'O Empresário deve indicar o Contador ou Escritório Contábil responsável.';
        return;
      }
    }

    const payload: CriarEmpresaDTO = {
      razaoSocial: this.novaEmpresaForm.razaoSocial.trim(),
      nomeFantasia: this.novaEmpresaForm.nomeFantasia ? this.novaEmpresaForm.nomeFantasia.trim() : undefined,
      cnpj: unmaskCnpj(this.novaEmpresaForm.cnpj),
      dataInicioAtividade: this.novaEmpresaForm.dataInicioAtividade,
      cpfEmpresario: this.isAdmin ? unmaskCpf(this.novaEmpresaForm.cpfEmpresario) : undefined,
      nomeEmpresario: this.isAdmin ? this.novaEmpresaForm.nomeEmpresario : undefined,
      contadorIdentificador: this.isEmpresario ? contadorFinal : undefined
    };

    if (payload.dataInicioAtividade && payload.dataInicioAtividade.length === 7) {
      payload.dataInicioAtividade = payload.dataInicioAtividade + '-01';
    }

    this.empresaService.criar(payload).subscribe({
      next: (novaEmpresa) => {
        if (this.isAdmin) {
          this.sucesso = `Empresa ${novaEmpresa.razaoSocial} cadastrada! O empresário receberá um aviso ao entrar para aceitar a empresa e sua contabilidade.`;
        } else {
          this.sucesso = `Empresa ${novaEmpresa.razaoSocial} cadastrada com sucesso e vinculada ao contador indicado!`;
        }
        this.carregarEmpresas();
        this.fecharModalEmpresa();
        setTimeout(() => this.sucesso = null, 5000);
      },
      error: (err) => {
        this.erro = err.error?.error || 'Erro ao cadastrar nova empresa.';
      }
    });
  }

  excluirEmpresa(empresa: Empresa): void {
    if (!this.isAdmin && !this.isEmpresario) return;
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
    this.novoUsuarioForm.cpf = maskCpf(event.target.value);
  }

  vincularNovoUsuario(): void {
    if (!this.novoUsuarioForm.cpf || !this.novoUsuarioForm.senha) return;
    
    const cpfLimpo = unmaskCpf(this.novoUsuarioForm.cpf);
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
