import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ExtratoLancamentoDTO,
  ConciliacaoBancariaDTO,
  ImportExtratoResponseDTO
} from '../shared/types/banking.types';
import { ContaReceberDTO } from '../shared/types/receivables.types';

@Injectable({
  providedIn: 'root'
})
export class BankingService {
  private readonly baseUrl = `${environment.apiUrl}/banking`;

  constructor(private http: HttpClient) {}

  importarCsv(file: File, empresaId: string): Observable<ImportExtratoResponseDTO> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ImportExtratoResponseDTO>(
      `${this.baseUrl}/importar/csv?empresaId=${empresaId}`,
      formData
    );
  }

  importarOfx(file: File, empresaId: string): Observable<ImportExtratoResponseDTO> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ImportExtratoResponseDTO>(
      `${this.baseUrl}/importar/ofx?empresaId=${empresaId}`,
      formData
    );
  }

  listarLancamentosPendentes(empresaId: string): Observable<ExtratoLancamentoDTO[]> {
    return this.http.get<ExtratoLancamentoDTO[]>(`${this.baseUrl}/extrato/${empresaId}/pendentes`);
  }

  listarContasPendentes(empresaId: string): Observable<ContaReceberDTO[]> {
    return this.http.get<ContaReceberDTO[]>(`${this.baseUrl}/contas-receber/${empresaId}/pendentes`);
  }

  conciliar(
    extratoLancamentoId: string,
    contaReceberId: string,
    usuarioId: string,
    observacao?: string
  ): Observable<ConciliacaoBancariaDTO> {
    return this.http.post<ConciliacaoBancariaDTO>(
      `${this.baseUrl}/conciliar?usuarioId=${usuarioId}`,
      { extratoLancamentoId, contaReceberId, observacao }
    );
  }
}