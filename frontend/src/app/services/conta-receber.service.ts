import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ContaReceberDTO, CreateContaReceberDTO } from '../shared/types/receivables.types';

export interface ContaReceberPage {
  content: ContaReceberDTO[];
  totalElements: number;
  totalPages: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class ContaReceberService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/contas-a-receber`;

  listar(empresaId: string, filtros: Record<string, string | number | undefined>): Observable<ContaReceberPage> {
    let params = new HttpParams().set('empresaId', empresaId);
    Object.entries(filtros).forEach(([chave, valor]) => {
      if (valor !== undefined && valor !== '') params = params.set(chave, String(valor));
    });
    return this.http.get<ContaReceberPage>(this.url, { params });
  }

  criar(conta: CreateContaReceberDTO): Observable<ContaReceberDTO> {
    return this.http.post<ContaReceberDTO>(this.url, conta);
  }

  baixar(empresaId: string, id: string): Observable<ContaReceberDTO> {
    return this.http.post<ContaReceberDTO>(`${this.url}/${id}/baixar`, null, {
      params: { empresaId }
    });
  }

  importarXml(empresaId: string, arquivo: File): Observable<ContaReceberDTO> {
    const formData = new FormData();
    formData.append('empresaId', empresaId);
    formData.append('arquivo', arquivo);
    return this.http.post<ContaReceberDTO>(`${this.url}/importar-xml`, formData);
  }
}