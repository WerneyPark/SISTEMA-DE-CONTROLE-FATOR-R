import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Empresa } from '../models/models';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class EmpresaService {
  private readonly baseUrl = `${environment.apiUrl}/empresas`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  listarTodas(): Observable<Empresa[]> {
    let params = new HttpParams();
    const user = this.authService.getCurrentUser();
    
    // Se for Usuário Empresa, envia o usuarioId para o backend filtrar.
    // Se for Administrador, o backend retorna tudo (não precisa de ID).
    if (user && user.tipo === 'USUARIO_EMPRESA') {
      params = params.set('usuarioId', user.id.toString());
    }

    return this.http.get<Empresa[]>(this.baseUrl, { params });
  }

  criar(empresa: Empresa): Observable<Empresa> {
    return this.http.post<Empresa>(this.baseUrl, empresa);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
