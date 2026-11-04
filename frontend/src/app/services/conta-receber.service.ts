import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ContaReceberDTO,
  CreateContaReceberDTO,
  ClienteFornecedorDTO
} from '../shared/types/receivables.types';

export interface ParcelaNotaFiscalDTO {
  numero: string;
  dataVencimento: string;
  valor: number;
}

export interface DadosNotaFiscalDTO {
  chaveAcesso: string;
  cnpjEmitente: string;
  nomeEmitente: string;
  cnpjTomador: string;
  nomeTomador: string;
  valorTotal: number;
  dataEmissao: string;
  dataVencimento: string;
  numeroDocumento: string;
  parcelas?: ParcelaNotaFiscalDTO[];
  periodoEncerrado?: boolean;
  mensagemAlertaPeriodo?: string;
}

export interface ContaReceberPage {
  content: ContaReceberDTO[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface FiltrosContaReceber {
  status?: string;
  clienteId?: string;
  inicio?: string;
  fim?: string;
  pagina?: number;
  tamanho?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ContaReceberService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/contas-a-receber`;

  listar(empresaId: string, filtros: FiltrosContaReceber = {}): Observable<ContaReceberPage> {
    let params = new HttpParams().set('empresaId', empresaId);
    if (filtros.status) {
      params = params.set('status', filtros.status);
    }
    if (filtros.clienteId) {
      params = params.set('clienteId', filtros.clienteId);
    }
    if (filtros.inicio) {
      params = params.set('inicio', filtros.inicio);
    }
    if (filtros.fim) {
      params = params.set('fim', filtros.fim);
    }
    if (filtros.pagina !== undefined && filtros.pagina !== null) {
      params = params.set('pagina', String(filtros.pagina));
    }
    if (filtros.tamanho !== undefined && filtros.tamanho !== null) {
      params = params.set('tamanho', String(filtros.tamanho));
    }

    return this.http.get<ContaReceberPage>(this.url, { params });
  }

  buscar(empresaId: string, id: string): Observable<ContaReceberDTO> {
    const params = new HttpParams().set('empresaId', empresaId);
    return this.http.get<ContaReceberDTO>(`${this.url}/${id}`, { params });
  }

  criar(conta: CreateContaReceberDTO): Observable<ContaReceberDTO> {
    return this.http.post<ContaReceberDTO>(this.url, conta);
  }

  atualizar(empresaId: string, id: string, conta: Partial<ContaReceberDTO>): Observable<ContaReceberDTO> {
    const params = new HttpParams().set('empresaId', empresaId);
    return this.http.put<ContaReceberDTO>(`${this.url}/${id}`, conta, { params });
  }

  baixar(empresaId: string, id: string): Observable<ContaReceberDTO> {
    const params = new HttpParams().set('empresaId', empresaId);
    return this.http.post<ContaReceberDTO>(`${this.url}/${id}/baixar`, null, { params });
  }

  cancelar(empresaId: string, id: string): Observable<void> {
    const params = new HttpParams().set('empresaId', empresaId);
    return this.http.delete<void>(`${this.url}/${id}`, { params });
  }

  cancelarNf(empresaId: string, id: string): Observable<ContaReceberDTO> {
    const params = new HttpParams().set('empresaId', empresaId);
    return this.http.post<ContaReceberDTO>(`${this.url}/${id}/cancelar-nf`, null, { params });
  }

  previewXml(empresaId: string, arquivo: File): Observable<DadosNotaFiscalDTO> {
    const formData = new FormData();
    formData.append('empresaId', empresaId);
    formData.append('arquivo', arquivo);
    return this.http.post<DadosNotaFiscalDTO>(`${this.url}/preview-xml`, formData);
  }

  importarXml(empresaId: string, arquivo: File, atualizarFaturamento = false): Observable<ContaReceberDTO[]> {
    const formData = new FormData();
    formData.append('empresaId', empresaId);
    formData.append('arquivo', arquivo);
    let params = new HttpParams();
    if (atualizarFaturamento) {
      params = params.set('atualizarFaturamento', 'true');
    }
    return this.http.post<ContaReceberDTO[]>(`${this.url}/importar-xml`, formData, { params });
  }

  listarClientes(empresaId: string): Observable<ClienteFornecedorDTO[]> {
    const params = new HttpParams().set('empresaId', empresaId);
    return this.http.get<ClienteFornecedorDTO[]>(`${this.url}/clientes`, { params });
  }
}
