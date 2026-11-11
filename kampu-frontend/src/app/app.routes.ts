import { Routes } from '@angular/router';
import { LandingComponent } from './features/landing/landing.component';
import { LoginComponent } from './features/login/login.component';
import { ShellComponent } from './layout/shell.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ReservasComponent } from './features/reservas/reservas.component';
import { ReservaNuevaComponent } from './features/reservas/reserva-nueva.component';
import { ReservaDetalleComponent } from './features/reservas/reserva-detalle.component';
import { SalidasComponent } from './features/salidas/salidas.component';
import { SalidaDetalleComponent } from './features/salidas/salida-detalle.component';
import { PaquetesComponent } from './features/paquetes/paquetes.component';
import { PagosComponent } from './features/pagos/pagos.component';
import { ClientesComponent } from './features/clientes/clientes.component';
import { RecursosComponent } from './features/recursos/recursos.component';
import { OperacionesComponent } from './features/operaciones/operaciones.component';
import { RegistroComponent } from './features/portal/registro.component';
import { MisPaquetesComponent } from './features/portal/mis-paquetes.component';
import { PaqueteClienteComponent } from './features/portal/paquete-cliente.component';
import { clienteGuard, permisoGuard, personalGuard } from './core/services/guards';
import { AuditoriaComponent } from './features/auditoria/auditoria.component';

// Permiso que exige cada pantalla (PKG_SEGURIDAD_ROLES; el backend vuelve a validarlo en cada petición)
const permiso = (p: string) => ({ permiso: p });

export const routes: Routes = [
  { path: '', component: LandingComponent, pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  // Módulo de clientes (v4)
  { path: 'registro', component: RegistroComponent },
  { path: 'mi-cuenta', component: MisPaquetesComponent, canActivate: [clienteGuard] },
  { path: 'mi-cuenta/paquetes/:id', component: PaqueteClienteComponent, canActivate: [clienteGuard] },
  {
    path: '',
    component: ShellComponent,
    canActivate: [personalGuard],
    canActivateChild: [permisoGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'reservas', component: ReservasComponent, data: permiso('VER_RESERVAS') },
      { path: 'reservas/nueva', component: ReservaNuevaComponent, data: permiso('GESTIONAR_RESERVAS') },
      { path: 'reservas/:id', component: ReservaDetalleComponent, data: permiso('VER_RESERVAS') },
      { path: 'salidas', component: SalidasComponent, data: permiso('VER_RESUMEN') },
      { path: 'salidas/:id', component: SalidaDetalleComponent, data: permiso('VER_RESUMEN') },
      { path: 'paquetes', component: PaquetesComponent, data: permiso('GESTIONAR_CATALOGO') },
      { path: 'pagos', component: PagosComponent, data: permiso('VER_MONTOS') },
      { path: 'clientes', component: ClientesComponent, data: permiso('VER_CLIENTES') },
      { path: 'operaciones', component: OperacionesComponent, data: permiso('VER_OPERACIONES') },
      { path: 'recursos', component: RecursosComponent, data: permiso('VER_LOGISTICA') },
      { path: 'auditoria', component: AuditoriaComponent, data: permiso('VER_AUDITORIA') }
    ]
  },
  { path: '**', redirectTo: '' }
];
