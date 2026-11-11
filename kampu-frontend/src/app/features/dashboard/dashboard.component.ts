import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EstadoEjecucion, ResumenPaquete } from '../../core/models';
import { TourStore } from '../../core/services/tour-store.service';
import { AuthService } from '../../core/services/auth.service';
import { AlertService } from '../../core/services/alert.service';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';

// Panel por rol (v4). El bloque "Paquetes" sale de VW_RESUMEN_PAQUETES_POR_ROL:
//  - ADMIN / VENDEDOR: todos los estados y montos.
//  - OPERADOR: solo POR_EMPEZAR y EN_CURSO, montos en NULL (los filtra Oracle, no Angular).
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, BadgeComponent, IconComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  store = inject(TourStore);
  auth = inject(AuthService);
  private alerts = inject(AlertService);

  // Bloques comerciales (dinero y reservas) solo con VER_MONTOS / VER_RESERVAS
  comercial = computed(() => this.auth.puede('VER_MONTOS') && this.auth.puede('VER_RESERVAS'));
  nuevaReserva = computed(() => this.auth.puede('GESTIONAR_RESERVAS'));
  ejecuta = computed(() => this.auth.puede('ACTUALIZAR_EJECUCION'));
  traficoVisible = computed(() => this.auth.puede('VER_OPERACIONES'));
  filtro = signal<'TODOS' | EstadoEjecucion>('TODOS');
  procesando = signal<number | null>(null);

  saludo = computed(() => {
    const h = new Date().getHours();
    return h < 12 ? 'Buenos días' : h < 19 ? 'Buenas tardes' : 'Buenas noches';
  });

  kpis = computed(() => {
    const activas = this.store.reservas().filter(r => r.estadoReserva !== 'ANULADA');
    const resumen = this.store.resumen();
    return {
      porEmpezar: resumen.filter(r => r.estadoEjecucion === 'POR_EMPEZAR').length,
      enCurso: resumen.filter(r => r.estadoEjecucion === 'EN_CURSO').length,
      pasajeros: resumen.filter(r => r.estadoEjecucion === 'EN_CURSO' || r.estadoEjecucion === 'POR_EMPEZAR').reduce((a, r) => a + r.pasajeros, 0),
      reservas: activas.filter(r => r.estadoReserva === 'PENDIENTE' || r.estadoReserva === 'CONFIRMADA').length,
      cobrado: activas.reduce((a, r) => a + r.montoPagado, 0),
      porCobrar: activas.reduce((a, r) => a + this.store.saldo(r), 0)
    };
  });

  filtros = computed(() => {
    const base: { valor: 'TODOS' | EstadoEjecucion; texto: string }[] = [
      { valor: 'TODOS', texto: 'Todos' }, { valor: 'POR_EMPEZAR', texto: 'Por empezar' }, { valor: 'EN_CURSO', texto: 'En curso' }];
    const extra: { valor: 'TODOS' | EstadoEjecucion; texto: string }[] = [{ valor: 'TERMINADO', texto: 'Terminados' }, { valor: 'CANCELADO', texto: 'Cancelados' }];
    return this.comercial() ? [...base, ...extra] : base;
  });

  paquetes = computed(() => {
    const f = this.filtro();
    const orden: { [e: string]: number } = { EN_CURSO: 0, POR_EMPEZAR: 1, TERMINADO: 2, CANCELADO: 3 };
    return this.store.resumen()
      .filter(r => f === 'TODOS' || r.estadoEjecucion === f)
      .sort((a, b) => (orden[a.estadoEjecucion] - orden[b.estadoEjecucion])
        || (a.estadoEjecucion === 'TERMINADO' ? b.fechaInicio.localeCompare(a.fechaInicio) : a.fechaInicio.localeCompare(b.fechaInicio)))
      .slice(0, 12);
  });

  recientes = computed(() =>
    [...this.store.reservas()]
      .sort((a, b) => b.fechaReserva.localeCompare(a.fechaReserva))
      .slice(0, 5)
      .map(r => ({
        reserva: r,
        codigo: this.store.codigoReserva(r.reservaId),
        cliente: this.store.nombreCliente(r),
        paquete: this.store.getPaquete(this.store.getSalida(r.salidaId)?.paqueteId)
      })));

  ocupacion(r: ResumenPaquete) {
    return r.cuposMaximos ? Math.round((r.cuposMaximos - r.cuposDisponibles) * 100 / r.cuposMaximos) : 0;
  }

  // Actualizar el estado de ejecución (ACTUALIZAR_EJECUCION: Admin, Ventas y Operador)
  // Confirmar: PROGRAMADA -> CONFIRMADA (exige transporte) · Terminar: EN_CURSO -> COMPLETADA
  puedeIniciar(r: ResumenPaquete) { return this.ejecuta() && r.estadoSalida === 'PROGRAMADA' && r.estadoEjecucion !== 'TERMINADO'; }
  puedeTerminar(r: ResumenPaquete) { return this.ejecuta() && r.estadoEjecucion === 'EN_CURSO' && r.estadoSalida !== 'COMPLETADA'; }

  async cambiar(r: ResumenPaquete, estado: 'CONFIRMADA' | 'COMPLETADA') {
    const texto = estado === 'COMPLETADA' ? 'Se marcará la salida como terminada.' : 'Se confirmará la salida (debe tener transporte asignado para todos los pasajeros).';
    if (!(await this.alerts.confirm(texto, r.paqueteNombre, 'Continuar'))) return;
    this.procesando.set(r.salidaId);
    this.store.marcarEjecucion(r.salidaId, estado).subscribe({
      next: () => {
        this.procesando.set(null);
        this.alerts.success(estado === 'COMPLETADA' ? 'Salida terminada.' : 'Salida confirmada.');
        this.store.cargarSalidas();
      },
      error: err => { this.procesando.set(null); this.alerts.error(this.store.error(err)); }
    });
  }
}
