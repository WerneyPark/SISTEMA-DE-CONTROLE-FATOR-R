import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UsuarioService } from '../../services/usuario.service';
import { UsuarioAdmin } from '../../models/models';
import { maskCpf } from '../../shared/utils/formatters';
import { maskCnpj } from '../../shared/utils/cnpj';

@Component({
  selector: 'app-usuarios-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './usuarios-admin.html',
  styleUrls: ['./usuarios-admin.css']
})
export class UsuariosAdminComponent implements OnInit {
  usuarios: UsuarioAdmin[] = [];
  loading = true;

  constructor(private usuarioService: UsuarioService) {}

  formatarCpf(cpf: string): string {
    return maskCpf(cpf);
  }

  formatarCnpj(cnpj: string): string {
    return maskCnpj(cnpj);
  }

  ngOnInit() {
    this.carregarUsuarios();
  }

  carregarUsuarios() {
    this.loading = true;
    this.usuarioService.listarTodos().subscribe({
      next: (dados) => {
        this.usuarios = dados;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erro ao carregar usuarios', err);
        this.loading = false;
      }
    });
  }
}
