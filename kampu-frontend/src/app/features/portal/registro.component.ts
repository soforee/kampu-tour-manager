import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { RegistroForm, TipoDocumento } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { mensajeError } from '../../core/utils/api';
import { filtrarEscritura, filtrarPegado } from '../../core/utils/filtro';
import { errorTelefonoPais } from '../../core/utils/paises';
import { FormValidacion, V } from '../../core/utils/validacion';
import { IconComponent } from '../../shared/icon.component';
import { TelefonoInputComponent } from '../../shared/telefono-input.component';
import { PortalHeaderComponent } from './portal-header.component';

export const PASSWORD_MIN = 8;

// "Registrarse" (reemplaza a "Probar cuenta demo").
// POST /auth/registro -> SP_REGISTRAR_CLIENTE_EXTERNO. Si todo sale bien el cliente
// queda autenticado y pasa directo a "Mis paquetes".
@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [FormsModule, RouterLink, IconComponent, TelefonoInputComponent, PortalHeaderComponent],
  templateUrl: './registro.component.html'
})
export class RegistroComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  tipos: TipoDocumento[] = ['DNI', 'CE', 'PASAPORTE'];
  min = PASSWORD_MIN;
  form: RegistroForm = { nombreCompleto: '', tipoDocumento: 'DNI', numeroDocumento: '', email: '', telefono: '', password: '', confirmacion: '' };
  ver = signal(false);
  cargando = signal(false);
  error = signal('');

  v = new FormValidacion(() => ({
    nombre: V.nombrePersona(this.form.nombreCompleto, 'Tu nombre'),
    documento: V.documento(this.form.tipoDocumento, this.form.numeroDocumento),
    email: this.form.email.trim() ? V.email(this.form.email) : 'El correo es obligatorio.',
    telefono: errorTelefonoPais(this.form.telefono),
    password: this.form.password.length < PASSWORD_MIN
      ? `La contraseña debe tener al menos ${PASSWORD_MIN} caracteres.`
      : this.form.password.length > 64 ? 'La contraseña admite máximo 64 caracteres.' : null,
    confirmacion: !this.form.confirmacion ? 'Repite tu contraseña.'
      : this.form.confirmacion !== this.form.password ? 'Las contraseñas no coinciden.' : null
  }));

  constructor() {
    if (this.auth.autenticado()) this.router.navigateByUrl(this.auth.inicio());
  }

  // Indicador simple de fortaleza (solo orientativo)
  fuerza(): number {
    const p = this.form.password;
    let n = 0;
    if (p.length >= PASSWORD_MIN) n++;
    if (/[A-Z]/.test(p) && /[a-z]/.test(p)) n++;
    if (/\d/.test(p)) n++;
    if (/[^A-Za-z0-9]/.test(p) || p.length >= 12) n++;
    return n;
  }

  registrar() {
    this.error.set('');
    if (!this.v.valido()) return;
    this.cargando.set(true);
    const datos: RegistroForm = {
      ...this.form,
      nombreCompleto: this.form.nombreCompleto.trim().replace(/\s+/g, ' '),
      numeroDocumento: this.form.numeroDocumento.trim().toUpperCase(),
      email: this.form.email.trim().toLowerCase()
    };
    this.auth.registrar(datos).subscribe({
      next: sesion => {
        // Autenticación automática tras el registro
        this.auth.guardarSesion(sesion);
        this.router.navigate(['/mi-cuenta'], { queryParams: { bienvenido: 1 } });
      },
      error: err => {
        this.error.set(mensajeError(err));
        this.cargando.set(false);
      }
    });
  }

  filtrar(e: Event, tipo: string) { filtrarEscritura(e, tipo); }
  pegar(e: Event, tipo: string) { filtrarPegado(e, tipo); }
}
