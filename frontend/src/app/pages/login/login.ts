import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { unmaskCpf, maskCpf } from '../../shared/utils/formatters';

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
    const v = maskCpf(event.target.value);
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

    const cpfLimpo = unmaskCpf(this.cpf);

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
