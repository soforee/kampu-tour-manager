import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EstadoEjecucion, ResumenPaquete } from '../../core/models';
import { TourStore } from '../../core/services/tour-store.service';
import { AuthService } from '../../core/services/auth.service';
import { AlertService } from '../../core/services/alert.service';
import { AlertasService } from '../../core/services/alertas.service';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';

// Panel por rol (v4). El bloque "Paquetes" sale de VW_RESUMEN_PAQUETES_POR_ROL:
//  - ADMIN / VENDEDOR: todos los estados y montos.
//  - OPERADOR: solo POR_EMPEZAR y EN_CURSO, montos en NULL (los filtra Oracle, no Angular).
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, BadgeComponent, IconComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
  store = inject(TourStore);
  auth = inject(AuthService);
  private alerts = inject(AlertService);
  private avisos = inject(AlertasService);

  // Lo que hay que resolver hoy (salidas sin guía o transporte, reservas sin confirmar, pagos por validar...)
  alertas = this.avisos.lista;
  verTodas = signal(false);
  alertasVisibles = computed(() => this.verTodas() ? this.alertas() : this.alertas().slice(0, 4));
  urgentes = computed(() => this.alertas().filter(a => a.nivel === 'alta').length);

  // Bloques comerciales (dinero y reservas) solo con VER_MONTOS / VER_RESERVAS
  comercial = computed(() => this.auth.puede('VER_MONTOS') && this.auth.puede('VER_RESERVAS'));
  nuevaReserva = computed(() => this.auth.puede('GESTIONAR_RESERVAS'));
  ejecuta = computed(() => this.auth.puede('ACTUALIZAR_EJECUCION'));
  traficoVisible = computed(() => this.auth.puede('VER_OPERACIONES'));
  filtro = signal<'TODOS' | EstadoEjecucion>('TODOS');
  procesando = signal<number | null>(null);
  hoy = new Date();

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

  // ---------- Comparación con el mes anterior (pagos aprobados, en soles) ----------
  private mes(fecha: string) { return fecha.slice(0, 7); }
  private claveMes(d: Date) { return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`; }
  cobros = computed(() => {
    const hoy = new Date();
    const actual = this.claveMes(hoy);
    const anterior = this.claveMes(new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1));
    let esteMes = 0, mesPasado = 0;
    for (const p of this.store.pagos()) {
      if (p.estadoTransaccion !== 'APROBADO') continue;
      if (this.mes(p.fechaPago) === actual) esteMes += p.montoPen;
      if (this.mes(p.fechaPago) === anterior) mesPasado += p.montoPen;
    }
    const variacion = mesPasado > 0 ? Math.round((esteMes - mesPasado) * 100 / mesPasado) : null;
    return { esteMes, mesPasado, variacion };
  });
  reservasConSaldo = computed(() => this.store.reservas().filter(r => r.estadoReserva !== 'ANULADA' && this.store.saldo(r) > 0).length);

  // ---------- Hoy: traslados del día, salidas que empiezan y pagos por validar ----------
  hoyIso = new Date().toISOString().slice(0, 10);
  private fechaLocal(d: Date) { return `${this.claveMes(d)}-${String(d.getDate()).padStart(2, '0')}`; }
  hoyDatos = computed(() => {
    const dia = this.fechaLocal(new Date());
    const operaciones = this.store.operaciones()
      .filter(o => o.horaProgramadaSalida.startsWith(dia) && o.estadoActual !== 'CANCELADO')
      .sort((a, b) => a.horaProgramadaSalida.localeCompare(b.horaProgramadaSalida));
    const salidas = this.store.resumen().filter(r => r.fechaInicio.startsWith(dia) && r.estadoSalida !== 'CANCELADA');
    const pagosPorValidar = this.store.pagos().filter(p => p.estadoTransaccion === 'PENDIENTE').length;
    return { operaciones, salidas, pagosPorValidar };
  });

  // ---------- Gráfico 1: vendido por mes (reservas no anuladas, últimos 6 meses) ----------
  verTablaVentas = signal(false);
  ventasPorMes = computed(() => {
    const hoy = new Date();
    const meses = Array.from({ length: 6 }, (_, i) => {
      const d = new Date(hoy.getFullYear(), hoy.getMonth() - 5 + i, 1);
      return { clave: this.claveMes(d), fecha: d, monto: 0, reservas: 0 };
    });
    for (const r of this.store.reservas()) {
      if (r.estadoReserva === 'ANULADA') continue;
      const m = meses.find(x => x.clave === this.mes(r.fechaReserva));
      if (m) { m.monto += r.montoTotal; m.reservas++; }
    }
    const max = Math.max(...meses.map(m => m.monto), 1);
    const tope = this.escala(max);
    const pico = meses.reduce((a, b) => (b.monto > a.monto ? b : a), meses[0]);
    return {
      tope,
      marcas: [tope, tope / 2, 0],
      barras: meses.map((m, i) => ({ ...m, alto: (m.monto / tope) * 100, etiqueta: m.monto > 0 && (i === meses.length - 1 || m === pico) }))
    };
  });

  // ---------- Gráfico 2: pasajeros por paquete (salidas no canceladas) ----------
  pasajerosPorPaquete = computed(() => {
    const mapa = new Map<string, number>();
    for (const r of this.store.resumen()) {
      if (r.estadoSalida === 'CANCELADA') continue;
      mapa.set(r.paqueteNombre, (mapa.get(r.paqueteNombre) ?? 0) + r.pasajeros);
    }
    const filas = [...mapa.entries()].map(([nombre, pax]) => ({ nombre, pax })).filter(f => f.pax > 0).sort((a, b) => b.pax - a.pax).slice(0, 6);
    const max = Math.max(...filas.map(f => f.pax), 1);
    return filas.map(f => ({ ...f, ancho: (f.pax / max) * 100 }));
  });

  // Tope "redondo" del eje: 1, 2 o 5 por potencia de 10
  private escala(max: number) {
    const p = Math.pow(10, Math.floor(Math.log10(max)));
    return [1, 2, 5, 10].map(x => x * p).find(x => x >= max) ?? max;
  }
  compacto(n: number) { return n >= 1000 ? `${Math.round(n / 100) / 10}K` : `${Math.round(n)}`; }

  ocupacion(r: ResumenPaquete) {
    return r.cuposMaximos ? Math.round((r.cuposMaximos - r.cuposDisponibles) * 100 / r.cuposMaximos) : 0;
  }

  // Actualizar el estado de ejecución (ACTUALIZAR_EJECUCION: Admin, Ventas y Operador)
  // Confirmar: PROGRAMADA -> CONFIRMADA (exige transporte) · Terminar: EN_CURSO -> COMPLETADA
  // Solo se ofrece confirmar si hay pasajeros (el backend además exige guía y transporte)
  puedeIniciar(r: ResumenPaquete) { return this.ejecuta() && r.estadoSalida === 'PROGRAMADA' && r.estadoEjecucion !== 'TERMINADO' && r.pasajeros > 0; }
  puedeTerminar(r: ResumenPaquete) { return this.ejecuta() && r.estadoEjecucion === 'EN_CURSO' && r.estadoSalida !== 'COMPLETADA'; }

  async cambiar(r: ResumenPaquete, estado: 'CONFIRMADA' | 'COMPLETADA') {
    const texto = estado === 'COMPLETADA'
      ? 'Las reservas confirmadas pasarán a COMPLETADA y la salida ya no podrá modificarse.'
      : 'El grupo queda confirmado: debe tener guía, pasajeros y transporte para todos.';
    if (!(await this.alerts.confirm(texto, r.paqueteNombre, 'Continuar'))) return;
    this.procesando.set(r.salidaId);
    this.store.marcarEjecucion(r.salidaId, estado).subscribe({
      next: () => {
        this.procesando.set(null);
        this.alerts.toast(estado === 'COMPLETADA' ? 'Salida terminada' : 'Salida confirmada');
        this.store.cargarSalidas();
        this.store.cargarReservas();   // al terminar, sus reservas pasan a COMPLETADA
      },
      error: err => { this.procesando.set(null); this.alerts.error(this.store.error(err)); }
    });
  }
}
