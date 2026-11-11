import { Routes } from '@angular/router';
import { LandingComponent } from './features/landing/landing.component';
import { LoginComponent } from './features/login/login.component';
import { ShellComponent } from './layout/shell.component';
import { clienteGuard, permisoGuard, personalGuard } from './core/services/guards';

// Permiso que exige cada pantalla (PKG_SEGURIDAD_ROLES; el backend vuelve a validarlo en cada petición)
const permiso = (p: string) => ({ permiso: p });

export const routes: Routes = [
  { path: '', component: LandingComponent, pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  // Módulo de clientes (v4)
  { path: 'afiliacion', loadComponent: () => import('./features/afiliacion/afiliacion.component').then(m => m.AfiliacionComponent) },
  { path: 'registro', loadComponent: () => import('./features/portal/registro.component').then(m => m.RegistroComponent) },
  { path: 'mi-cuenta', loadComponent: () => import('./features/portal/mis-paquetes.component').then(m => m.MisPaquetesComponent), canActivate: [clienteGuard] },
  { path: 'mi-cuenta/paquetes/:id', loadComponent: () => import('./features/portal/paquete-cliente.component').then(m => m.PaqueteClienteComponent), canActivate: [clienteGuard] },
  {
    path: '',
    component: ShellComponent,
    canActivate: [personalGuard],
    canActivateChild: [permisoGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent), data: { soloKampu: true } },
      { path: 'reservas', loadComponent: () => import('./features/reservas/reservas.component').then(m => m.ReservasComponent), data: permiso('VER_RESERVAS') },
      { path: 'reservas/nueva', loadComponent: () => import('./features/reservas/reserva-nueva.component').then(m => m.ReservaNuevaComponent), data: permiso('GESTIONAR_RESERVAS') },
      { path: 'reservas/:id', loadComponent: () => import('./features/reservas/reserva-detalle.component').then(m => m.ReservaDetalleComponent), data: permiso('VER_RESERVAS') },
      { path: 'salidas', loadComponent: () => import('./features/salidas/salidas.component').then(m => m.SalidasComponent), data: permiso('VER_RESUMEN') },
      { path: 'salidas/:id', loadComponent: () => import('./features/salidas/salida-detalle.component').then(m => m.SalidaDetalleComponent), data: permiso('VER_RESUMEN') },
      { path: 'paquetes', loadComponent: () => import('./features/paquetes/paquetes.component').then(m => m.PaquetesComponent), data: permiso('GESTIONAR_CATALOGO') },
      { path: 'pagos', loadComponent: () => import('./features/pagos/pagos.component').then(m => m.PagosComponent), data: permiso('VER_MONTOS') },
      { path: 'clientes', loadComponent: () => import('./features/clientes/clientes.component').then(m => m.ClientesComponent), data: permiso('VER_CLIENTES') },
      { path: 'operaciones', loadComponent: () => import('./features/operaciones/operaciones.component').then(m => m.OperacionesComponent), data: permiso('VER_OPERACIONES') },
      { path: 'recursos', loadComponent: () => import('./features/recursos/recursos.component').then(m => m.RecursosComponent), data: permiso('VER_LOGISTICA') },
      { path: 'auditoria', loadComponent: () => import('./features/auditoria/auditoria.component').then(m => m.AuditoriaComponent), data: permiso('VER_AUDITORIA') }
    ]
  },
  { path: '**', redirectTo: '' }
];
