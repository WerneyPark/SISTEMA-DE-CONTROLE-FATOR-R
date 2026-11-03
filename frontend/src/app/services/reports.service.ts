import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ParametrosEmissaoLaudoDTO,
  LaudoTributarioDTO,
  ValidacaoLaudoResponseDTO
} from '../shared/types/reports.types';

@Injectable({
  providedIn: 'root'
})
export class ReportsService {
  private readonly baseUrl = `${environment.apiUrl}/reports`;

  constructor(private http: HttpClient) {}

  /**
   * Emite laudo em PDF ou planilha Excel e retorna o Blob binário
   */
  emitirLaudo(parametros: ParametrosEmissaoLaudoDTO): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/emitir`, parametros, {
      responseType: 'blob'
    });
  }

  /**
   * Baixa documento PDF previamente gerado pelo ID único
   */
  downloadLaudo(id: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/download/${id}`, {
      responseType: 'blob'
    });
  }

  /**
   * Baixa diretamente a planilha Excel formatada (.xlsx) da competência
   */
  baixarExcelCompetencia(empresaId: string, competencia: string): Observable<Blob> {
    const params = new HttpParams().set('competencia', competencia);
    return this.http.get(`${this.baseUrl}/excel/${empresaId}`, {
      params,
      responseType: 'blob'
    });
  }

  /**
   * Lista o histórico de laudos fiscais gerados para a empresa
   */
  listarHistorico(empresaId: string): Observable<LaudoTributarioDTO[]> {
    return this.http.get<LaudoTributarioDTO[]>(`${this.baseUrl}/historico/${empresaId}`);
  }

  /**
   * Consulta pública de validação de autenticidade documental por Hash SHA-256
   */
  validarAutenticidade(hash: string): Observable<ValidacaoLaudoResponseDTO> {
    const hashSanitizado = encodeURIComponent(hash.trim());
    return this.http.get<ValidacaoLaudoResponseDTO>(`${this.baseUrl}/validar/${hashSanitizado}`);
  }
}
