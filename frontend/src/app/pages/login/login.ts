import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  cpf = '';
  senha = '';
  erro = '';
  carregando = false;

  constructor(private authService: AuthService, private router: Router) {}

  aplicarMascaraCPF(event: any) {
    let v = event.target.value.replace(/\D/g, '');
    if (v.length > 11) v = v.substring(0, 11);
    
    if (v.length > 9) {
      v = v.replace(/(\d{3})(\d{3})(\d{3})(\d{1,2})/, '$1.$2.$3-$4');
    } else if (v.length > 6) {
      v = v.replace(/(\d{3})(\d{3})(\d{1,3})/, '$1.$2.$3');
    } else if (v.length > 3) {
      v = v.replace(/(\d{3})(\d{1,3})/, '$1.$2');
    }
    
    this.cpf = v;
    event.target.value = v;
  }

  fazerLogin() {
    if (!this.cpf || !this.senha) {
      this.erro = 'Por favor, preencha todos os campos.';
      return;
    }

    this.carregando = true;
    this.erro = '';

    const cpfLimpo = this.cpf.replace(/\D/g, '');

    this.authService.login(cpfLimpo, this.senha).subscribe({
      next: (user) => {
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.erro = 'CPF ou senha incorretos.';
        this.carregando = false;
      }
    });
  }
}
