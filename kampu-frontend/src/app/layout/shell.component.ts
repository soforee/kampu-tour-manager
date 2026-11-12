import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { TourStore } from '../core/services/tour-store.service';
import { AlertService } from '../core/services/alert.service';
import { PreferenciasService } from '../core/services/preferencias.service';
import { IconComponent } from '../shared/icon.component';
import { LogoComponent } from '../shared/logo.component';
import { etiqueta } from '../core/utils/etiquetas';
import { Permiso } from '../core/models';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent, LogoComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css'
})
export class ShellComponent {
  auth = inject(AuthService);
  pref = inject(PreferenciasService);
  private store = inject(TourStore);
  private router = inject(Router);
  private alerts = inject(AlertService);

  abierto = signal(false);

  // Menú según los permisos de la sesión (mismos que exigen app.routes.ts y el backend)
  private todo: { ruta: string; texto: string; icono: string; permiso?: Permiso; exact?: boolean }[] = [
    { ruta: '/dashboard', texto: 'Panel', icono: 'chart' },
    { ruta: '/reservas', texto: 'Reservas', icono: 'ticket', permiso: 'VER_RESERVAS' },
    { ruta: '/salidas', texto: 'Salidas', icono: 'calendar', permiso: 'VER_RESUMEN' },
    { ruta: '/paquetes', texto: 'Paquetes', icono: 'package', permiso: 'GESTIONAR_CATALOGO' },
    { ruta: '/pagos', texto: 'Pagos', icono: 'card', permiso: 'VER_MONTOS' },
    { ruta: '/clientes', texto: 'Clientes', icono: 'users', permiso: 'VER_CLIENTES' },
    { ruta: '/operaciones', texto: 'Tráfico', icono: 'truck', permiso: 'VER_OPERACIONES' },
    { ruta: '/recursos', texto: 'Logística', icono: 'map-pin', permiso: 'VER_LOGISTICA' },
    { ruta: '/auditoria', texto: 'Auditoría', icono: 'file-text', permiso: 'VER_AUDITORIA' }
  ];
  menu = computed(() => this.todo.filter(m => !m.permiso || this.auth.puede(m.permiso)));
  rolTexto = computed(() => etiqueta(this.auth.rol()));

  pendientes = computed(() => this.store.reservas().filter(r => r.estadoReserva === 'PENDIENTE').length);
  iniciales = computed(() => (this.auth.usuario()?.username ?? '?').slice(0, 2).toUpperCase());

  constructor() {
    if (!this.auth.autenticado()) {
      this.router.navigate(['/login']);
      return;
    }
    // Primero se actualizan los permisos (pudieron cambiar en PKG_SEGURIDAD_ROLES) y luego se cargan los datos
    this.auth.refrescar().subscribe({
      next: () => this.cargarDatos(),
      error: () => { if (this.auth.autenticado()) this.cargarDatos(); }
    });
  }

  private cargarDatos() {
    this.store.cargar(true, error => {
      if (error && this.auth.autenticado()) this.alerts.error(error, 'No se pudieron cargar los datos');
    });
  }

  async salir() {
    if (await this.alerts.confirm('Se cerrará tu sesión actual.', 'Cerrar sesión', 'Cerrar sesión')) {
      this.auth.logout();
      this.store.limpiar();
      this.router.navigate(['/login']);
    }
  }
}
