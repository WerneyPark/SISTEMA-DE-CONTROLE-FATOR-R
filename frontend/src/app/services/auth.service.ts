import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, BehaviorSubject } from 'rxjs';
import { environment } from '../../environments/environment';

export interface EmpresaVinculada {
  id: number;
  uuidId?: string;
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string;
  statusVinculo?: 'PENDENTE' | 'APROVADO' | 'REJEITADO';
  funcao?: string;
}

export interface AuthResponse {
  id: number;
  uuidId?: string;
  nome?: string;
  email?: string;
  cpf: string;
  tipoPerfil?: 'CONTABILISTA' | 'EMPRESARIO' | 'USUARIO';
  tipo: 'ADMINISTRADOR_CONTABIL' | 'USUARIO_EMPRESA';
  status?: string;
  organizacaoContabilId?: string;
  token: string;
  empresas?: EmpresaVinculada[];
}

export interface ContadorResumo {
  id: string;
  usuarioId: string;
  nomeContador: string;
  cpfContador: string;
  nomeOrganizacao: string;
  cnpjOrganizacao: string;
  crc: string;
}

export interface RegisterContadorDTO {
  nome: string;
  email: string;
  cpf: string;
  senha: string;
  nomeOrganizacao: string;
  cnpj: string;
  crc: string;
}

export interface RegisterEmpresarioDTO {
  nome: string;
  email: string;
  cpf: string;
  senha: string;
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string;
  contadorIdentificador: string;
}

export interface RegisterUsuarioDTO {
  nome: string;
  email: string;
  cpf: string;
  senha: string;
  empresaId: string | number;
  funcao?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  private currentUserSubject = new BehaviorSubject<AuthResponse | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadUserFromStorage();
  }

  private loadUserFromStorage(): void {
    const userStr = localStorage.getItem('fator_r_user');
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        this.currentUserSubject.next(user);
      } catch (e) {
        this.logout();
      }
    }
  }

  private setSession(response: AuthResponse): void {
    if (response.token) {
      localStorage.setItem('fator_r_token', response.token);
    }
    localStorage.setItem('fator_r_user', JSON.stringify(response));
    localStorage.removeItem('empresaSelecionada');
    this.currentUserSubject.next(response);
  }

  login(loginOuCpf: string, senha: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, {
      login: loginOuCpf,
      cpf: loginOuCpf,
      senha
    }).pipe(
      tap(response => this.setSession(response))
    );
  }

  registerContador(dto: RegisterContadorDTO): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register/contador`, dto).pipe(
      tap(response => this.setSession(response))
    );
  }

  registerEmpresario(dto: RegisterEmpresarioDTO): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register/empresario`, dto).pipe(
      tap(response => this.setSession(response))
    );
  }

  registerUsuario(dto: RegisterUsuarioDTO): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register/usuario`, dto).pipe(
      tap(response => this.setSession(response))
    );
  }

  listarContadores(): Observable<ContadorResumo[]> {
    return this.http.get<ContadorResumo[]>(`${this.baseUrl}/contadores`);
  }

  forgotPassword(identificador: string): Observable<{ message: string; resetPasswordToken?: string }> {
    return this.http.post<{ message: string; resetPasswordToken?: string }>(`${this.baseUrl}/forgot-password`, {
      login: identificador,
      email: identificador,
      cpf: identificador
    });
  }

  resetPassword(token: string, novaSenha: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/reset-password`, {
      token,
      novaSenha
    });
  }

  logout(): void {
    localStorage.removeItem('fator_r_user');
    localStorage.removeItem('fator_r_token');
    localStorage.removeItem('empresaSelecionada');
    this.currentUserSubject.next(null);
  }

  getToken(): string | null {
    const token = localStorage.getItem('fator_r_token');
    if (token) return token;
    const user = this.getCurrentUser();
    return user?.token || null;
  }

  getCurrentUser(): AuthResponse | null {
    return this.currentUserSubject.value;
  }

  isLoggedIn(): boolean {
    return !!this.getCurrentUser();
  }

  isAdmin(): boolean {
    const user = this.getCurrentUser();
    return user?.tipoPerfil === 'CONTABILISTA' || user?.tipo === 'ADMINISTRADOR_CONTABIL';
  }

  isEmpresario(): boolean {
    const user = this.getCurrentUser();
    return user?.tipoPerfil === 'EMPRESARIO';
  }

  isColaborador(): boolean {
    const user = this.getCurrentUser();
    return user?.tipoPerfil === 'USUARIO';
  }
}
