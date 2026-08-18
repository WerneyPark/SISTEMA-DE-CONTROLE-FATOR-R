import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FatorRService } from '../../services/fator-r.service';
import { EmpresaContextService } from '../../services/empresa-context.service';
import { AuthService } from '../../services/auth.service';
import { ParametroTributario, FaixaTributaria, Empresa } from '../../models/models';

@Component({
  selector: 'app-parametros',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './parametros.html',
  styleUrls: ['./parametros.css']
})
export class ParametrosComponent implements OnInit {
  isAdmin = false;
  erro: string | null = null;
  sucesso: string | null = null;
  empresaSelecionada: Empresa | null = null;
  usandoPadrao: boolean = false;

  parametrosForm: ParametroTributario = {
    aliquotaPis: 0.65,
    aliquotaCofins: 3.0,
    aliquotaIss: 5.0,
    aliquotaIrpj: 15.0,
    aliquotaAdicionalIrpj: 10.0,
    aliquotaCsll: 9.0,
    faixas: [],
    ativo: true
  };

  constructor(
    private fatorRService: FatorRService,
    private empresaContext: EmpresaContextService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.isAdmin = this.authService.isAdmin();
    this.empresaContext.empresaSelecionada$.subscribe(empresa => {
      this.empresaSelecionada = empresa;
      if (empresa) {
        this.carregarParametros();
      }
    });
  }

  carregarParametros() {
    if (!this.empresaSelecionada) return;
    
    this.fatorRService.obterParametrosPorEmpresa(this.empresaSelecionada.id!).subscribe({
      next: (params) => {
        if (params) {
          this.parametrosForm = { ...params };
          this.usandoPadrao = false;
        }
      },
      error: () => {
        // Se não tem específico, carrega o padrão
        this.fatorRService.obterParametrosPadrao().subscribe({
          next: (params) => {
            if (params) {
              this.parametrosForm = { ...params };
              this.usandoPadrao = true;
            }
          }
        });
      }
    });
  }

  getFaixas(anexo: string): FaixaTributaria[] {
    if (!this.parametrosForm.faixas) return [];
    const anexoCurto = anexo.replace('Anexo ', '');
    return this.parametrosForm.faixas
      .filter(f => f.anexo === anexo || f.anexo === anexoCurto)
      .sort((a, b) => a.numeroFaixa - b.numeroFaixa);
  }

  adicionarFaixa(anexo: string): void {
    if (!this.isAdmin) return;
    if (!this.parametrosForm.faixas) this.parametrosForm.faixas = [];
    
    const faixasAtuais = this.getFaixas(anexo);
    const proximaFaixa = faixasAtuais.length > 0 ? faixasAtuais[faixasAtuais.length - 1].numeroFaixa + 1 : 1;
    
    this.parametrosForm.faixas.push({
      anexo: anexo,
      numeroFaixa: proximaFaixa,
      aliquotaNominal: 0,
      valorDeduzir: 0
    });
  }

  removerFaixa(faixa: FaixaTributaria): void {
    if (!this.isAdmin) return;
    if (!this.parametrosForm.faixas) return;
    this.parametrosForm.faixas = this.parametrosForm.faixas.filter(f => f !== faixa);
  }

  salvarParametros(): void {
    if (!this.isAdmin) return;
    if (!this.empresaSelecionada) return;
    this.fatorRService.salvarParametrosPorEmpresa(this.empresaSelecionada.id!, this.parametrosForm).subscribe({
      next: (paramsAtualizados: any) => {
        this.parametrosForm = { ...paramsAtualizados };
        this.sucesso = 'Parâmetros Específicos atualizados com sucesso!';
        this.usandoPadrao = false;
        setTimeout(() => this.sucesso = null, 3000);
      },
      error: () => this.erro = 'Erro ao atualizar parâmetros.'
    });
  }
}
