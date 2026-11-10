import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { EmpresaContextService } from '../../services/empresa-context.service';
import { EmpresaService } from '../../services/empresa.service';
import { AuthService } from '../../services/auth.service';
import { UsuarioService, ConviteEmpresa } from '../../services/usuario.service';
import { FormsModule } from '@angular/forms';
import { filter, Subscription } from 'rxjs';
import { Empresa } from '../../models/models';

interface RouteInfo {
  title: string;
  category: string;
  icon: string;
}

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './main-layout.html',
  styleUrls: ['./main-layout.css']
})
export class MainLayoutComponent implements OnInit, OnDestroy {
  empresas: Empresa[] = [];
  empresaSelecionada: Empresa | null = null;
  empresaSelecionadaId: number | null = null;
  isAdmin = false;
  isEmpresario = false;
  usuarioAtual = '';
  usuarioNome = '';
  usuarioPerfil = '';

  convitesPendentes: ConviteEmpresa[] = [];
  processandoConviteId: string | null = null;

  sidebarCollapsed = false;
  mobileMenuOpen = false;

  currentRouteInfo: RouteInfo = {
    title: 'Dashboard Executivo',
    category: 'Visão Geral',
    icon: 'dashboard'
  };

  private routerSub?: Subscription;
  private empresaSub?: Subscription;

  private routeMap: Record<string, RouteInfo> = {
    '/home': { title: 'Dashboard Executivo', category: 'Visão Geral', icon: 'dashboard' },
    '/simulador': { title: 'Simulador Tributário Fator R', category: 'Planejamento Fiscal', icon: 'calculator' },
    '/payroll': { title: 'Folha de Pagamento & eSocial', category: 'Planejamento Fiscal', icon: 'users' },
    '/reports': { title: 'Dossiês & Laudos Tributários', category: 'Planejamento Fiscal', icon: 'file-text' },
    '/contas-a-receber': { title: 'Contas a Receber & NF-e', category: 'Financeiro & Bancos', icon: 'dollar' },
    '/banking': { title: 'Conciliação Bancária OFX/CSV', category: 'Financeiro & Bancos', icon: 'activity' },
    '/empresas': { title: 'Empresas Cadastradas', category: 'Gestão & Administração', icon: 'briefcase' },
    '/parametros': { title: 'Parâmetros Tributários da Empresa', category: 'Gestão & Administração', icon: 'sliders' },
    '/parametros-padrao': { title: 'Parâmetros Tributários Globais', category: 'Gestão & Administração', icon: 'globe' },
    '/usuarios-admin': { title: 'Gerenciamento de Usuários e Acessos', category: 'Gestão & Administração', icon: 'shield' }
  };

  constructor(
    private empresaContext: EmpresaContextService,
    private empresaService: EmpresaService,
    private authService: AuthService,
    private usuarioService: UsuarioService,
    private router: Router
  ) {}

  ngOnInit() {
    const user = this.authService.getCurrentUser();
    this.usuarioAtual = user?.cpf || '';
    this.usuarioNome = user?.nome || (user?.cpf ? `CPF: ${user.cpf}` : 'Usuário');
    this.isAdmin = this.authService.isAdmin();
    this.isEmpresario = this.authService.isEmpresario();
    
    if (this.authService.isAdmin()) {
      this.usuarioPerfil = 'Contador Responsável';
    } else if (this.authService.isEmpresario()) {
      this.usuarioPerfil = 'Empresário / Gestor';
    } else {
      this.usuarioPerfil = 'Operador / Colaborador';
    }

    this.carregarEmpresas();
    this.carregarConvitesPendentes();

    this.empresaSub = this.empresaContext.empresaSelecionada$.subscribe(emp => {
      this.empresaSelecionada = emp;
      this.empresaSelecionadaId = emp ? emp.id! : null;
    });

    this.updateRouteInfo(this.router.url);

    this.routerSub = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.updateRouteInfo(event.urlAfterRedirects || event.url);
        this.mobileMenuOpen = false;
        this.carregarConvitesPendentes();
      });
  }

  ngOnDestroy() {
    this.routerSub?.unsubscribe();
    this.empresaSub?.unsubscribe();
  }

  toggleSidebar() {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  toggleMobileMenu() {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  closeMobileMenu() {
    this.mobileMenuOpen = false;
  }

  carregarEmpresas() {
    this.empresaService.listarTodas().subscribe({
      next: (dados: any) => {
        this.empresas = dados;
        this.empresaContext.setEmpresas(dados);
      }
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

  aceitarConvite(c: ConviteEmpresa) {
    this.processandoConviteId = c.vinculoId;
    this.usuarioService.responderConvite(c.vinculoId, true).subscribe({
      next: () => {
        this.processandoConviteId = null;
        this.carregarConvitesPendentes();
        this.carregarEmpresas();
      },
      error: () => this.processandoConviteId = null
    });
  }

  recusarConvite(c: ConviteEmpresa) {
    if (confirm(`Deseja realmente recusar a vinculação com a empresa ${c.razaoSocial} e o contador ${c.nomeContador}?`)) {
      this.processandoConviteId = c.vinculoId;
      this.usuarioService.responderConvite(c.vinculoId, false).subscribe({
        next: () => {
          this.processandoConviteId = null;
          this.carregarConvitesPendentes();
        },
        error: () => this.processandoConviteId = null
      });
    }
  }

  onEmpresaChange() {
    const selecionada = this.empresas.find(e => e.id == this.empresaSelecionadaId);
    this.empresaContext.setEmpresaSelecionada(selecionada || null);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private updateRouteInfo(url: string) {
    const cleanUrl = url.split('?')[0];
    const match = this.routeMap[cleanUrl];
    if (match) {
      this.currentRouteInfo = match;
    } else {
      this.currentRouteInfo = {
        title: 'Sistema Fator R',
        category: 'Gestão Inteligente',
        icon: 'layers'
      };
    }
  }
}
