import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { IconComponent } from '../../shared/icon.component';
import { LogoComponent } from '../../shared/logo.component';

// Cabecera común del módulo de clientes (registro, lista y detalle).
// Requisito v4: "Volver al inicio" visible en todas las pantallas del cliente.
@Component({
  selector: 'app-portal-header',
  standalone: true,
  imports: [RouterLink, IconComponent, LogoComponent],
  template: `
    <header class="sticky top-0 z-20 border-b border-zinc-200/70 bg-white/85 backdrop-blur-md">
      <div class="mx-auto flex max-w-5xl items-center justify-between gap-3 px-4 py-3 sm:px-6">
        <a routerLink="/" class="flex items-center gap-2" aria-label="Kampu - inicio">
          <app-logo variante="horizontal" [alto]="26" />
        </a>
        <nav class="flex items-center gap-2">
          <a routerLink="/" class="btn-light btn-sm" data-testid="volver-inicio">
            <app-icon name="arrow-left" size="w-3.5 h-3.5" /> Volver al inicio
          </a>
          @if (auth.esCliente()) {
            <a routerLink="/mi-cuenta" class="btn-light btn-sm hidden sm:inline-flex"><app-icon name="package" size="w-3.5 h-3.5" /> Mis paquetes</a>
            <button type="button" class="btn-light btn-sm" (click)="salir()" title="Cerrar sesión">
              <app-icon name="logout" size="w-3.5 h-3.5" /><span class="hidden sm:inline">Salir</span>
            </button>
          }
        </nav>
      </div>
    </header>`
})
export class PortalHeaderComponent {
  auth = inject(AuthService);
  private router = inject(Router);

  salir() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
