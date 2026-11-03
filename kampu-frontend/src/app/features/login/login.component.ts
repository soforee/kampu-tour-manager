import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { mensajeError } from '../../core/utils/api';
import { AuthService } from '../../core/services/auth.service';
import { IconComponent } from '../../shared/icon.component';
import { LogoComponent } from '../../shared/logo.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, IconComponent, LogoComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  username = '';
  password = '';
  ver = signal(false);
  error = signal('');
  cargando = signal(false);

  rellenar(username: string, password: string) {
    this.username = username;
    this.password = password;
    this.error.set('');
  }

  constructor() {
    if (this.auth.autenticado()) this.router.navigate(['/dashboard']);
  }

  entrar() {
    if (!this.username.trim() || !this.password) {
      this.error.set('Ingresa tu usuario y contraseña.');
      return;
    }
    this.cargando.set(true);
    this.auth.login(this.username, this.password).subscribe({
      next: (sesion) => {
        this.auth.guardarSesion(sesion);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.error.set(mensajeError(err));
        this.cargando.set(false);
      }
    });
  }
}
