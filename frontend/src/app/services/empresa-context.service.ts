import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Empresa } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class EmpresaContextService {
  private empresaSelecionadaSubject = new BehaviorSubject<Empresa | null>(null);
  public empresaSelecionada$ = this.empresaSelecionadaSubject.asObservable();

  private empresasSubject = new BehaviorSubject<Empresa[]>([]);
  public empresas$ = this.empresasSubject.asObservable();

  constructor() {
    // Tenta carregar a última empresa acessada do localStorage
    const saved = localStorage.getItem('empresaSelecionada');
    if (saved) {
      try {
        this.empresaSelecionadaSubject.next(JSON.parse(saved));
      } catch (e) {}
    }
  }

  setEmpresas(empresas: Empresa[]) {
    this.empresasSubject.next(empresas);
    
    const atual = this.empresaSelecionadaSubject.value;
    
    // Se a empresa atual não estiver na nova lista, resetamos ou pegamos a primeira
    if (empresas.length > 0) {
      if (!atual || !empresas.find(e => e.id === atual.id)) {
        this.setEmpresaSelecionada(empresas[0]);
      }
    } else {
      this.setEmpresaSelecionada(null);
    }
  }

  setEmpresaSelecionada(empresa: Empresa | null) {
    this.empresaSelecionadaSubject.next(empresa);
    if (empresa) {
      localStorage.setItem('empresaSelecionada', JSON.stringify(empresa));
    } else {
      localStorage.removeItem('empresaSelecionada');
    }
  }

  getEmpresaSelecionada(): Empresa | null {
    return this.empresaSelecionadaSubject.value;
  }
}
