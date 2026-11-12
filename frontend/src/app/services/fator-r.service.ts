import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SimulacaoTributaria, FaturamentoMensal, FolhaPagamentoMensal, ParametroTributario, ProjecaoCenario } from '../models/models';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FatorRService {
  private readonly empresasUrl = `${environment.apiUrl}/empresas`;
  private readonly faturamentosUrl = `${environment.apiUrl}/faturamentos`;
  private readonly folhasUrl = `${environment.apiUrl}/folhas`;
  private readonly parametrosUrl = `${environment.apiUrl}/parametros`;

  constructor(private http: HttpClient) {}

  simularTributacao(empresaId: number, ano?: number, mes?: number): Observable<SimulacaoTributaria> {
    let params = new HttpParams();
    if (ano !== undefined && mes !== undefined) {
      params = params.set('ano', ano.toString()).set('mes', mes.toString());
    }
    return this.http.get<SimulacaoTributaria>(
      `${this.empresasUrl}/${empresaId}/simulacao-tributaria`,
      { params }
    );
  }

  simularFuturo(empresaId: number, ano?: number, mes?: number): Observable<ProjecaoCenario[]> {
    let params = new HttpParams();
    if (ano !== undefined && mes !== undefined) {
      params = params.set('ano', ano.toString()).set('mes', mes.toString());
    }
    return this.http.get<ProjecaoCenario[]>(
      `${this.empresasUrl}/${empresaId}/simulacao-futura`,
      { params }
    );
  }

  encerrarCompetencia(empresaId: number, competencia: string): Observable<{ mensagem: string }> {
    return this.http.post<{ mensagem: string }>(`${this.empresasUrl}/${empresaId}/competencias/${competencia}/encerrar`, {});
  }

  reabrirCompetencia(empresaId: number, competencia: string): Observable<{ mensagem: string }> {
    return this.http.post<{ mensagem: string }>(`${this.empresasUrl}/${empresaId}/competencias/${competencia}/reabrir`, {});
  }

  listarCompetenciasEncerradas(empresaId: number): Observable<string[]> {
    return this.http.get<string[]>(`${this.empresasUrl}/${empresaId}/competencias-encerradas`);
  }

  salvarDadosSimulacao(empresaId: number, dto: { competencia: string; valorFaturamento?: number; valorFolha?: number }): Observable<{ mensagem: string }> {
    return this.http.post<{ mensagem: string }>(`${this.empresasUrl}/${empresaId}/dados-simulacao`, dto);
  }

  // Faturamentos
  listarFaturamentos(empresaId: number): Observable<FaturamentoMensal[]> {
    return this.http.get<FaturamentoMensal[]>(`${this.faturamentosUrl}/empresa/${empresaId}`);
  }

  criarFaturamento(dto: { empresaId: number, competencia: string, valor: number }): Observable<FaturamentoMensal> {
    return this.http.post<FaturamentoMensal>(this.faturamentosUrl, dto);
  }

  excluirFaturamento(id: number): Observable<void> {
    return this.http.delete<void>(`${this.faturamentosUrl}/${id}`);
  }

  // Folhas
  listarFolhas(empresaId: number): Observable<FolhaPagamentoMensal[]> {
    return this.http.get<FolhaPagamentoMensal[]>(`${this.folhasUrl}/empresa/${empresaId}`);
  }

  criarFolha(dto: { empresaId: number, competencia: string, valor: number }): Observable<FolhaPagamentoMensal> {
    return this.http.post<FolhaPagamentoMensal>(this.folhasUrl, dto);
  }

  excluirFolha(id: number): Observable<void> {
    return this.http.delete<void>(`${this.folhasUrl}/${id}`);
  }

  // Parametros Tributarios Padrão
  obterParametrosPadrao(): Observable<ParametroTributario> {
    return this.http.get<ParametroTributario>(`${this.parametrosUrl}/padrao`);
  }

  salvarParametrosPadrao(dto: ParametroTributario): Observable<ParametroTributario> {
    return this.http.post<ParametroTributario>(`${this.parametrosUrl}/padrao`, dto);
  }

  // Parametros Tributarios por Empresa
  obterParametrosPorEmpresa(empresaId: number): Observable<ParametroTributario> {
    return this.http.get<ParametroTributario>(`${this.parametrosUrl}/empresa/${empresaId}`);
  }

  salvarParametrosPorEmpresa(empresaId: number, dto: ParametroTributario): Observable<ParametroTributario> {
    return this.http.post<ParametroTributario>(`${this.parametrosUrl}/empresa/${empresaId}`, dto);
  }

  // Compatibilidade com código antigo (se necessário)
  obterParametrosAtuais(): Observable<ParametroTributario> {
    return this.http.get<ParametroTributario>(`${this.parametrosUrl}/atual`);
  }

  salvarParametros(dto: ParametroTributario): Observable<ParametroTributario> {
    return this.http.post<ParametroTributario>(this.parametrosUrl, dto);
  }
}
