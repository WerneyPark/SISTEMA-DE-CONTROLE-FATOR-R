import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FatorRService } from '../../services/fator-r.service';
import { AuthService } from '../../services/auth.service';
import { ParametroTributario, FaixaTributaria } from '../../models/models';

@Component({
  selector: 'app-parametros-padrao',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './parametros-padrao.html',
  styleUrls: ['./parametros-padrao.css']
})
export class ParametrosPadraoComponent implements OnInit {
  isAdmin = false;
  erro: string | null = null;
  sucesso: string | null = null;

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
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.isAdmin = this.authService.isAdmin();
    this.carregarParametros();
  }

  carregarParametros() {
    this.fatorRService.obterParametrosPadrao().subscribe({
      next: (params) => {
        if (params) {
          this.parametrosForm = { ...params };
        }
      },
      error: () => this.erro = 'Erro ao carregar parâmetros.'
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
    this.fatorRService.salvarParametrosPadrao(this.parametrosForm).subscribe({
      next: (paramsAtualizados: any) => {
        this.parametrosForm = { ...paramsAtualizados };
        this.sucesso = 'Parâmetros Padrão atualizados com sucesso!';
        setTimeout(() => this.sucesso = null, 3000);
      },
      error: () => this.erro = 'Erro ao atualizar parâmetros.'
    });
  }
}
