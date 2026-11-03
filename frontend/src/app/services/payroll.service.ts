import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  FolhaDetalhadaDTO,
  CreateFolhaDetalhadaDTO,
  ImportFolhaResponseDTO,
  ProLaboreIdealDTO
} from '../shared/types/payroll.types';

export interface ImportFolhaFullResponseDTO extends ImportFolhaResponseDTO {
  mensagem?: string;
  empresaId?: string;
  valorDecimoTerceiro?: number;
  valorInssPatronal?: number;
  valorFgts?: number;
  xmlChaveRecibo?: string;
  origem?: string;
}

export interface ProLaboreIdealFullDTO extends ProLaboreIdealDTO {
  razaoSocial?: string;
  rbt12?: number;
  folha12Atual?: number;
  fatorRAtual?: number;
  anexoAtual?: string;
  inssSocio?: number;
  irpfSocio?: number;
  custoTributarioTotal?: number;
  aliquotaEfetivaAnexoV?: number;
  aliquotaEfetivaAnexoIII?: number;
  vantajoso?: boolean;
  mensagemRecomendacao?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PayrollService {
  private readonly baseUrl = `${environment.apiUrl}/payroll`;

  constructor(private http: HttpClient) {}

  listarFolhas(empresaId: string): Observable<FolhaDetalhadaDTO[]> {
    return this.http.get<FolhaDetalhadaDTO[]>(`${this.baseUrl}/${empresaId}`);
  }

  buscarPorCompetencia(empresaId: string, competencia: string): Observable<FolhaDetalhadaDTO> {
    return this.http.get<FolhaDetalhadaDTO>(`${this.baseUrl}/${empresaId}/${competencia}`);
  }

  salvarFolha(folha: Partial<FolhaDetalhadaDTO> | CreateFolhaDetalhadaDTO): Observable<FolhaDetalhadaDTO> {
    return this.http.post<FolhaDetalhadaDTO>(this.baseUrl, folha);
  }

  importarEsocial(file: File, empresaId: string): Observable<ImportFolhaFullResponseDTO> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ImportFolhaFullResponseDTO>(
      `${this.baseUrl}/importar/esocial?empresaId=${empresaId}`,
      formData
    );
  }

  importarCsv(file: File, empresaId: string): Observable<ImportFolhaFullResponseDTO> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ImportFolhaFullResponseDTO>(
      `${this.baseUrl}/importar/csv?empresaId=${empresaId}`,
      formData
    );
  }

  calcularProLaboreIdeal(
    empresaId: string,
    competencia: string,
    faturamentoEstimado?: number
  ): Observable<ProLaboreIdealFullDTO> {
    let url = `${this.baseUrl}/${empresaId}/pro-labore-ideal?competencia=${competencia}`;
    if (faturamentoEstimado !== undefined && faturamentoEstimado !== null) {
      url += `&faturamentoEstimado=${faturamentoEstimado}`;
    }
    return this.http.get<ProLaboreIdealFullDTO>(url);
  }

  excluirFolha(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
