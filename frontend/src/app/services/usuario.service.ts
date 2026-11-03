import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Usuario {
  id?: number;
  uuidId?: string;
  nome?: string;
  email?: string;
  cpf: string;
  tipo: string;
  tipoPerfil?: string;
  status?: string;
}

export interface NovoUsuarioEmpresaDTO {
  cpf: string;
  senha?: string;
  empresaId: number;
}

export interface ConvidarCpfDTO {
  cpf: string;
  funcao: string;
  empresaId: string | number;
}

export interface VinculoPendente {
  id: string;
  empresaId: string;
  empresaNome: string;
  empresaCnpj: string;
  usuarioId: string;
  usuarioNome: string;
  usuarioCpf: string;
  usuarioEmail: string;
  funcao: string;
  statusVinculo: 'PENDENTE' | 'APROVADO' | 'REJEITADO';
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  private readonly baseUrl = `${environment.apiUrl}/usuarios`;

  constructor(private http: HttpClient) {}

  vincularEmpresa(dto: NovoUsuarioEmpresaDTO): Observable<any> {
    return this.http.post(`${this.baseUrl}/vincular-empresa`, dto, { responseType: 'text' });
  }

  listarPorEmpresa(empresaId: number): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.baseUrl}/empresa/${empresaId}`);
  }

  listarTodos(): Observable<import('../models/models').UsuarioAdmin[]> {
    return this.http.get<import('../models/models').UsuarioAdmin[]>(this.baseUrl);
  }

  desvincularEmpresa(usuarioId: number, empresaId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${usuarioId}/vincular-empresa/${empresaId}`, { responseType: 'text' });
  }

  convidarCpf(dto: ConvidarCpfDTO): Observable<any> {
    return this.http.post(`${this.baseUrl}/convidar-cpf`, dto);
  }

  listarVinculosPendentes(): Observable<VinculoPendente[]> {
    return this.http.get<VinculoPendente[]>(`${this.baseUrl}/vinculos-pendentes`);
  }

  atualizarStatusVinculo(id: string, status: 'APROVADO' | 'REJEITADO', aprovadoPor?: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/vinculos/${id}/status`, {
      status,
      aprovadoPor
    });
  }
}
