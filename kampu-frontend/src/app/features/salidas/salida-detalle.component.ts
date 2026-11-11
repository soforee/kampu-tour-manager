import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EstadoSalida, Manifiesto, SalidaForm } from '../../core/models';
import { AlertService } from '../../core/services/alert.service';
import { AuthService } from '../../core/services/auth.service';
import { TourStore } from '../../core/services/tour-store.service';
import { cuando, diasHasta } from '../../core/utils/date.util';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';
import { LogoComponent } from '../../shared/logo.component';

interface Paso { ok: boolean; titulo: string; detalle: string; }

// Ficha de una salida: todo lo que el tour manager revisa antes de que el grupo salga
// (lista de verificación), el manifiesto de pasajeros para imprimir y los traslados.
@Component({
  selector: 'app-salida-detalle',
  standalone: true,
  imports: [RouterLink, DatePipe, CurrencyPipe, BadgeComponent, IconComponent, LogoComponent],
  templateUrl: './salida-detalle.component.html',
  styleUrl: './salida-detalle.component.css'
})
export class SalidaDetalleComponent {
  store = inject(TourStore);
  private auth = inject(AuthService);
  private alerts = inject(AlertService);
  private router = inject(Router);
  private id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  ejecuta = this.auth.puede('ACTUALIZAR_EJECUCION');
  gestiona = this.auth.puede('GESTIONAR_CATALOGO');
  veReservas = this.auth.puede('VER_RESERVAS');
  veTrafico = this.auth.puede('VER_OPERACIONES');
  vende = this.auth.puede('GESTIONAR_RESERVAS');

  manifiesto = signal<Manifiesto | null>(null);
  cargando = signal(true);
  error = signal('');
  procesando = signal(false);
  hoy = new Date();

  salida = computed(() => this.store.getSalida(this.id));
  paquete = computed(() => this.store.getPaquete(this.salida()?.paqueteId));
  guia = computed(() => this.store.getPersonal(this.salida()?.guiaId));
  operaciones = computed(() => this.store.operacionesDe(this.id)
    .filter(o => o.estadoActual !== 'CANCELADO')
    .sort((a, b) => a.horaProgramadaSalida.localeCompare(b.horaProgramadaSalida)));
  vendidos = computed(() => { const s = this.salida(); return s ? s.cuposMaximos - s.cuposDisponibles : 0; });
  ocupacion = computed(() => { const s = this.salida(); return s ? Math.round(this.vendidos() * 100 / s.cuposMaximos) : 0; });
  conMontos = computed(() => (this.manifiesto()?.pasajeros ?? []).some(p => p.estadoPago !== null));
  // Saldo por cobrar: una vez por reserva (el manifiesto lo repite en cada pasajero)
  porCobrar = computed(() => {
    const vistos = new Set<number>();
    let total = 0;
    for (const p of this.manifiesto()?.pasajeros ?? []) {
      if (p.saldo === null || vistos.has(p.reservaId)) continue;
      vistos.add(p.reservaId);
      total += p.saldo;
    }
    return total;
  });
  notas = computed(() => {
    const vistos = new Set<number>();
    const lista: { reservaId: number; titular: string; texto: string }[] = [];
    for (const p of this.manifiesto()?.pasajeros ?? []) {
      if (!p.observaciones || vistos.has(p.reservaId)) continue;
      vistos.add(p.reservaId);
      lista.push({ reservaId: p.reservaId, titular: p.titular ?? '', texto: p.observaciones });
    }
    return lista;
  });

  cuandoSale = computed(() => {
    const s = this.salida();
    if (!s) return '';
    const inicio = diasHasta(s.fechaInicio);
    const fin = diasHasta(s.fechaFin);
    if (s.estado === 'COMPLETADA') return 'Viaje terminado';
    if (s.estado === 'CANCELADA') return 'Salida cancelada';
    if (inicio > 0) return 'Sale ' + cuando(inicio);
    if (fin >= 0) return fin === 0 ? 'En curso · termina hoy' : 'En curso · termina ' + cuando(fin);
    return 'El viaje terminó ' + cuando(fin);
  });

  // Lista de verificación: lo mismo que exige el backend para confirmar la salida
  pasos = computed<Paso[]>(() => {
    const s = this.salida();
    const m = this.manifiesto();
    if (!s) return [];
    const pendientes = m?.pasajerosPendientes ?? 0;
    const pasos: Paso[] = [
      { ok: !!s.guiaId, titulo: 'Guía asignado', detalle: this.guia()?.nombreCompleto ?? m?.guia ?? 'Asigna un guía a la salida.' },
      {
        ok: this.vendidos() > 0, titulo: 'Pasajeros',
        detalle: this.vendidos() > 0 ? `${this.vendidos()} de ${s.cuposMaximos} cupos vendidos.` : 'Todavía no hay reservas.'
      },
      {
        ok: !s.faltanteTransporte && (s.operaciones > 0 || this.vendidos() === 0), titulo: 'Transporte',
        detalle: s.faltanteTransporte ? 'Falta en el ' + s.faltanteTransporte + '.'
          : s.operaciones > 0 ? `${s.operaciones} traslado(s) con capacidad suficiente.` : 'No hay traslados programados para esta salida.'
      },
      {
        ok: pendientes === 0 && this.vendidos() > 0, titulo: 'Reservas confirmadas',
        detalle: this.vendidos() === 0 ? 'Sin reservas.' : pendientes === 0 ? 'Todas las reservas están confirmadas.'
          : `${pendientes} pasajero(s) en reservas pendientes de adelanto.`
      }
    ];
    if (this.conMontos()) {
      pasos.push({
        ok: this.porCobrar() <= 0.005 && this.vendidos() > 0, titulo: 'Pagos al día',
        detalle: this.vendidos() === 0 ? 'Sin reservas.' : this.porCobrar() <= 0.005 ? 'No queda saldo por cobrar.'
          : `Queda por cobrar S/ ${this.porCobrar().toFixed(2)}.`
      });
    }
    return pasos;
  });
  listos = computed(() => this.pasos().filter(p => p.ok).length);

  puedeConfirmar = computed(() => this.ejecuta && this.salida()?.estado === 'PROGRAMADA');
  puedeTerminar = computed(() => {
    const s = this.salida();
    return this.ejecuta && !!s && s.estado === 'CONFIRMADA' && diasHasta(s.fechaInicio) <= 0;
  });
  puedeReabrir = computed(() => this.gestiona && this.salida()?.estado === 'CONFIRMADA' && diasHasta(this.salida()!.fechaInicio) > 0);
  puedeCancelar = computed(() => {
    const e = this.salida()?.estado;
    return this.gestiona && (e === 'PROGRAMADA' || e === 'CONFIRMADA');
  });
  puedeVender = computed(() => {
    const s = this.salida();
    return this.vende && !!s && (s.estado === 'PROGRAMADA' || s.estado === 'CONFIRMADA') && s.cuposDisponibles > 0 && diasHasta(s.fechaInicio) >= 0;
  });

  constructor() {
    this.store.cargar(false, error => {
      if (!error && !this.store.getSalida(this.id)) { this.router.navigate(['/salidas']); return; }
      this.cargarManifiesto();
    });
  }

  private cargarManifiesto() {
    this.store.manifiesto(this.id).subscribe({
      next: m => { this.manifiesto.set(m); this.cargando.set(false); },
      error: e => { this.error.set(this.store.error(e)); this.cargando.set(false); }
    });
  }

  private recargar() {
    this.store.cargarSalidas();
    this.store.cargarOperaciones();
    this.store.cargarReservas();
    this.cargarManifiesto();
  }

  async ejecucion(estado: 'CONFIRMADA' | 'COMPLETADA') {
    const texto = estado === 'CONFIRMADA'
      ? 'El grupo queda confirmado: guía, pasajeros y transporte deben estar listos.'
      : 'Las reservas confirmadas pasarán a COMPLETADA y la salida ya no podrá modificarse.';
    const titulo = estado === 'CONFIRMADA' ? 'Confirmar salida' : 'Marcar como terminada';
    if (!(await this.alerts.confirm(texto, titulo, 'Continuar'))) return;
    this.procesando.set(true);
    this.store.marcarEjecucion(this.id, estado).subscribe({
      next: () => { this.procesando.set(false); this.recargar(); this.alerts.toast(estado === 'CONFIRMADA' ? 'Salida confirmada' : 'Salida terminada'); },
      error: e => { this.procesando.set(false); this.alerts.error(this.store.error(e)); }
    });
  }

  // Reabrir y cancelar pasan por la edición de la salida (GESTIONAR_CATALOGO)
  async cambiar(estado: EstadoSalida) {
    const s = this.salida();
    if (!s) return;
    const cancelar = estado === 'CANCELADA';
    const texto = cancelar
      ? 'La salida se cierra y sus traslados pendientes se cancelan. No se puede deshacer.'
      : 'Vuelve a PROGRAMADA para poder ajustar guía, cupos o transporte.';
    if (!(await this.alerts.confirm(texto, cancelar ? 'Cancelar salida' : 'Reabrir salida', cancelar ? 'Cancelar salida' : 'Reabrir'))) return;
    const datos: SalidaForm = { paqueteId: s.paqueteId, fechaInicio: s.fechaInicio, cuposMaximos: s.cuposMaximos, guiaId: s.guiaId ?? null, estado: estado };
    this.procesando.set(true);
    this.store.guardarSalida(datos, s.salidaId).subscribe({
      next: () => { this.procesando.set(false); this.recargar(); this.alerts.toast(cancelar ? 'Salida cancelada' : 'Salida reabierta'); },
      error: e => { this.procesando.set(false); this.alerts.error(this.store.error(e)); }
    });
  }

  imprimir() {
    window.print();
  }
}
