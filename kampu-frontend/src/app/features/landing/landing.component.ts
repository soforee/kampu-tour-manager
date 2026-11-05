import { CurrencyPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PaqueteTour } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { ImagenService } from '../../core/services/imagen.service';
import { API } from '../../core/utils/api';
import { IconComponent } from '../../shared/icon.component';
import { LogoComponent } from '../../shared/logo.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, IconComponent, LogoComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.css'
})
export class LandingComponent {
  private http = inject(HttpClient);
  auth = inject(AuthService);
  imagenes = inject(ImagenService);

  private gradientes = [
    'from-zinc-800 to-zinc-900', 'from-brand-500 to-brand-700', 'from-earth-500 to-earth-700',
    'from-emerald-600 to-zinc-900', 'from-brand-600 to-zinc-900', 'from-earth-400 to-zinc-900'
  ];

  paquetes = signal<PaqueteTour[]>([]);
  cargando = signal(true);
  menu = signal(false);
  anio = new Date().getFullYear();

  destinos = computed(() =>
    this.paquetes()
      .filter(p => p.estado === 'ACTIVO')
      .slice(0, 6)
      .map(p => ({ paquete: p, gradiente: this.gradientes[p.paqueteId % this.gradientes.length] }))
  );

  ventajas = [
    { icono: 'mountain', titulo: 'Destinos auténticos', texto: 'Rutas pensadas con guías locales que conocen cada sendero.' },
    { icono: 'calendar', titulo: 'Salidas programadas', texto: 'Fechas fijas, cupos claros y confirmación de tu lugar al reservar.' },
    { icono: 'card', titulo: 'Pagos flexibles', texto: 'Paga en soles, dólares o euros, en una o varias cuotas.' }
  ];

  pasos = [
    { numero: '01', titulo: 'Elige tu destino', texto: 'Revisa los paquetes y la fecha de salida que mejor te acomode.' },
    { numero: '02', titulo: 'Reserva tu cupo', texto: 'Registramos a cada pasajero y separamos tu lugar en la salida.' },
    { numero: '03', titulo: 'Viaja tranquilo', texto: 'Tu guía y tu transporte te esperan el día de la salida.' }
  ];

  constructor() {
    this.http.get<PaqueteTour[]>(`${API}/paquetes`).subscribe({
      next: lista => {
        this.paquetes.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  duracion(p: PaqueteTour): string {
    const dias = p.dias === 1 ? '1 día' : `${p.dias} días`;
    if (!p.noches) return dias;
    return `${dias} · ${p.noches === 1 ? '1 noche' : p.noches + ' noches'}`;
  }

  cerrarMenu() {
    this.menu.set(false);
  }
}
