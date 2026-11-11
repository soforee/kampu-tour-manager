import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AfiliacionForm, TipoServicio } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { mensajeError } from '../../core/utils/api';
import { etiqueta } from '../../core/utils/etiquetas';
import { filtrarEscritura, filtrarPegado } from '../../core/utils/filtro';
import { errorTelefonoPais } from '../../core/utils/paises';
import { FormValidacion, V } from '../../core/utils/validacion';
import { IconComponent } from '../../shared/icon.component';
import { TelefonoInputComponent } from '../../shared/telefono-input.component';
import { PortalHeaderComponent } from '../portal/portal-header.component';
import { PASSWORD_MIN } from '../portal/registro.component';

// Afiliación de empresas de transporte: POST /auth/afiliacion. La empresa queda PENDIENTE hasta que
// Kampu la apruebe; mientras tanto ya puede ingresar y registrar su flota y conductores.
@Component({
  selector: 'app-afiliacion',
  standalone: true,
  imports: [FormsModule, RouterLink, IconComponent, TelefonoInputComponent, PortalHeaderComponent],
  templateUrl: './afiliacion.component.html'
})
export class AfiliacionComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  etiqueta = etiqueta;
  servicios: TipoServicio[] = ['TRASLADO', 'TOUR', 'INTERPROVINCIAL'];
  min = PASSWORD_MIN;
  form: AfiliacionForm = {
    nombreComercial: '', ruc: '', telefono: '', email: '', direccion: '', contacto: '', servicios: ['TRASLADO'],
    zonas: '', username: '', password: '', confirmacion: ''
  };
  ver = signal(false);
  cargando = signal(false);
  error = signal('');

  v = new FormValidacion(() => {
    const f = this.form;
    return {
      nombre: V.texto(f.nombreComercial, 'El nombre comercial', 150, true),
      ruc: V.ruc(f.ruc),
      telefono: errorTelefonoPais(f.telefono),
      email: f.email.trim() ? V.email(f.email) : 'El correo es obligatorio.',
      direccion: V.texto(f.direccion, 'La dirección', 200, true),
      contacto: V.nombrePersona(f.contacto, 'La persona de contacto'),
      servicios: f.servicios.length ? null : 'Elige al menos un servicio.',
      zonas: V.texto(f.zonas, 'Las zonas', 300),
      username: /^[a-zA-Z0-9._-]{4,30}$/.test(f.username.trim()) ? null : 'De 4 a 30 caracteres: letras, números y . _ -',
      password: f.password.length < PASSWORD_MIN ? `La contraseña debe tener al menos ${PASSWORD_MIN} caracteres.`
        : f.password.length > 64 ? 'La contraseña admite máximo 64 caracteres.' : null,
      confirmacion: !f.confirmacion ? 'Repite la contraseña.' : f.confirmacion !== f.password ? 'Las contraseñas no coinciden.' : null
    };
  });

  constructor() {
    if (this.auth.autenticado()) this.router.navigateByUrl(this.auth.inicio());
  }

  marcar(s: TipoServicio, marcado: boolean) {
    const lista = this.form.servicios.filter(x => x !== s);
    this.form.servicios = marcado ? [...lista, s] : lista;
    this.v.tocar('servicios');
  }

  enviar() {
    this.error.set('');
    if (!this.v.valido()) return;
    this.cargando.set(true);
    const f = this.form;
    const datos: AfiliacionForm = {
      ...f, nombreComercial: f.nombreComercial.trim(), ruc: f.ruc.trim(), email: f.email.trim().toLowerCase(),
      direccion: f.direccion.trim(), contacto: f.contacto.trim().replace(/\s+/g, ' '), zonas: f.zonas.trim(),
      username: f.username.trim().toLowerCase()
    };
    this.auth.afiliar(datos).subscribe({
      next: sesion => {
        this.auth.guardarSesion(sesion);
        this.router.navigate(['/recursos'], { queryParams: { tab: 'vehiculos' } });
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
