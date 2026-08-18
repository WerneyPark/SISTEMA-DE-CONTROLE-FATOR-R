import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Usuario {
  id?: number;
  cpf: string;
  tipo: string;
}

export interface NovoUsuarioEmpresaDTO {
  cpf: string;
  senha?: string;
  empresaId: number;
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
}
