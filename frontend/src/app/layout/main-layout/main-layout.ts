import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { EmpresaContextService } from '../../services/empresa-context.service';
import { EmpresaService } from '../../services/empresa.service';
import { AuthService } from '../../services/auth.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './main-layout.html',
  styleUrls: ['./main-layout.css']
})
export class MainLayoutComponent implements OnInit {
  empresas: any[] = [];
  empresaSelecionadaId: number | null = null;
  isAdmin = false;
  usuarioAtual = '';

  constructor(
    private empresaContext: EmpresaContextService,
    private empresaService: EmpresaService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.usuarioAtual = this.authService.getCurrentUser()?.cpf || '';
    this.isAdmin = this.authService.isAdmin();

    this.carregarEmpresas();

    this.empresaContext.empresaSelecionada$.subscribe(emp => {
      this.empresaSelecionadaId = emp ? emp.id! : null;
    });
  }

  carregarEmpresas() {
    this.empresaService.listarTodas().subscribe({
      next: (dados: any) => {
        this.empresas = dados;
        this.empresaContext.setEmpresas(dados);
      }
    });
  }

  onEmpresaChange() {
    const selecionada = this.empresas.find(e => e.id == this.empresaSelecionadaId);
    this.empresaContext.setEmpresaSelecionada(selecionada || null);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
