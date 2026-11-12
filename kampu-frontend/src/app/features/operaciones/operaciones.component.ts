import { AuthService } from '../../core/services/auth.service';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { EstadoOperacion, EstimacionDuracion, HistorialOperacion, Operacion, OperacionForm, RecursoCandidato } from '../../core/models';
import { TourStore } from '../../core/services/tour-store.service';
import { AlertService } from '../../core/services/alert.service';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';
import { EsqueletoComponent } from '../../shared/esqueleto.component';
import { ModalComponent } from '../../shared/modal.component';
import { filtrarEscritura, filtrarPegado, sinRueda } from '../../core/utils/filtro';
import { FormValidacion, V } from '../../core/utils/validacion';
import { etiqueta, iconoLugar } from '../../core/utils/etiquetas';
import { hoy } from '../../core/utils/date.util';

type Grupo = '' | 'PENDIENTES' | 'EN_CURSO' | 'CERRADAS';
type Ventana = 'form' | 'asignar' | 'estado' | 'historial' | null;

const EN_CURSO: EstadoOperacion[] = ['ASIGNADO', 'EMBARQUE', 'EN_RUTA'];

/**
 * Tráfico de transporte (OPERACIONES_TRANSPORTE). En v4 las reglas las aplica el backend:
 * transiciones de estado, cruces de horario, horas del conductor, mantenimiento, capacidad
 * de la salida, asignación automática (flota propia primero) e historial.
 */
@Component({
  selector: 'app-operaciones',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink, BadgeComponent, IconComponent, ModalComponent, EsqueletoComponent],
  templateUrl: './operaciones.component.html'
})
export class OperacionesComponent {
  store = inject(TourStore);
  private alerts = inject(AlertService);
  // GESTIONAR_OPERACIONES: Admin y Operador. Ventas solo consulta el tráfico (VER_OPERACIONES)
  gestiona = inject(AuthService).puede('GESTIONAR_OPERACIONES');
  etiqueta = etiqueta;
  iconoLugar = iconoLugar;

  grupos: { valor: Grupo; texto: string }[] = [
    { valor: '', texto: 'Todas' }, { valor: 'PENDIENTES', texto: 'Por asignar' },
    { valor: 'EN_CURSO', texto: 'En curso' }, { valor: 'CERRADAS', texto: 'Cerradas' }
  ];
  grupo = signal<Grupo>('');
  filtroSalida = signal<number | null>(null);
  ventana = signal<Ventana>(null);
  actual = signal<Operacion | null>(null);

  // Formulario de operación
  editId: number | null = null;
  form: OperacionForm = this.vacio();
  v = new FormValidacion(() => {
    const f = this.form;
    const llegadaMala = !!f.horaEstimadaLlegada && !!f.horaProgramadaSalida && f.horaEstimadaLlegada <= f.horaProgramadaSalida;
    return {
      origen: f.origenId ? null : 'Selecciona el origen.',
      destino: !f.destinoId ? 'Selecciona el destino.' : (f.destinoId === f.origenId ? 'El destino debe ser distinto del origen.' : null),
      salida: f.horaProgramadaSalida ? null : 'La hora de salida es obligatoria.',
      llegada: llegadaMala ? 'La llegada debe ser posterior a la salida.' : null,
      pax: V.entero(f.paxProgramados, 'Los pasajeros', 1, 999),
      recursos: (f.vehiculoId === null) !== (f.conductorId === null) ? 'Elige vehículo y conductor juntos (o deja ambos vacíos).' : null,
      observaciones: V.texto(f.observaciones, 'Las observaciones', 200)
    };
  });

  // Asignación
  candidatos = signal<RecursoCandidato[]>([]);
  cargandoCandidatos = signal(false);
  vehiculoSel: number | null = null;
  conductorSel: number | null = null;

  // Cambio de estado
  nuevoEstado: EstadoOperacion | null = null;
  observacion = '';

  historial = signal<HistorialOperacion[]>([]);

  constructor() {
    const salida = Number(inject(ActivatedRoute).snapshot.queryParamMap.get('salida'));
    if (salida) this.filtroSalida.set(salida);
    this.store.cargar();
  }

  // ---------- Tablero ----------
  filas = computed(() => {
    const g = this.grupo();
    const s = this.filtroSalida();
    return this.store.operaciones()
      .filter(o => !s || o.salidaId === s)
      .filter(o => {
        if (g === 'PENDIENTES') return o.estadoActual === 'PROGRAMADO';
        if (g === 'EN_CURSO') return EN_CURSO.includes(o.estadoActual);
        if (g === 'CERRADAS') return o.estadoActual === 'FINALIZADO' || o.estadoActual === 'CANCELADO';
        return true;
      })
      .map(o => ({ op: o, salida: this.store.getSalida(o.salidaId), paquete: this.store.getPaquete(this.store.getSalida(o.salidaId)?.paqueteId) }));
  });

  resumen = computed(() => {
    const ops = this.store.operaciones();
    return {
      porAsignar: ops.filter(o => o.estadoActual === 'PROGRAMADO').length,
      enCurso: ops.filter(o => EN_CURSO.includes(o.estadoActual)).length,
      // Salió más de 15 min tarde, o ya debió salir y sigue sin partir
      retrasadas: ops.filter(o => (o.minRetrasoSalida ?? 0) > 15
        || (['PROGRAMADO', 'ASIGNADO', 'EMBARQUE'].includes(o.estadoActual) && new Date(o.horaProgramadaSalida) < new Date())).length,
      hoy: ops.filter(o => o.horaProgramadaSalida.startsWith(hoy())).length
    };
  });

  salidasActivas = computed(() => this.store.salidas()
    .filter(s => s.estado === 'PROGRAMADA' || s.estado === 'CONFIRMADA')
    .sort((a, b) => a.fechaInicio.localeCompare(b.fechaInicio)));

  nombreSalida(id?: number | null): string {
    const s = this.store.getSalida(id);
    if (!s) return '';
    return `${this.store.getPaquete(s.paqueteId)?.nombre ?? 'Salida'} · ${s.fechaInicio.split('-').reverse().join('/')}`;
  }

  esFinal(o: Operacion) { return !!this.store.getEstadoOperacion(o.estadoActual)?.esFinal; }
  siguientes(o: Operacion) { return this.store.getEstadoOperacion(o.estadoActual)?.siguientes ?? []; }

  // ---------- Crear / editar ----------
  private vacio(): OperacionForm {
    return { salidaId: this.filtroSalida(), origenId: null, destinoId: null, vehiculoId: null, conductorId: null,
      paxProgramados: null, horaProgramadaSalida: '', horaEstimadaLlegada: null, observaciones: '' };
  }

  // Recursos del proveedor elegido (Kampu Tours u otro): vehículo y conductor deben ser del mismo
  proveedorSel: number | null = null;
  estimacion = signal<EstimacionDuracion | null>(null);
  get vehiculosProveedor() { return this.store.vehiculos().filter(x => x.proveedorId === this.proveedorSel); }
  get conductoresProveedor() { return this.store.conductores().filter(c => c.proveedorId === this.proveedorSel); }

  cambioRuta() { this.estimacion.set(null); }
  cambioProveedor(id: number | null) {
    this.proveedorSel = id;
    this.form.vehiculoId = null;
    this.form.conductorId = null;
  }

  // Duración con datos reales (OSRM / coordenadas): propone la llegada
  calcularLlegada() {
    const f = this.form;
    if (!f.origenId || !f.destinoId || f.origenId === f.destinoId) { this.alerts.toast('Elige origen y destino', 'info'); return; }
    this.store.estimarDuracion(f.origenId, f.destinoId).subscribe({
      next: e => {
        this.estimacion.set(e);
        if (e.minutos && f.horaProgramadaSalida) {
          const d = new Date(f.horaProgramadaSalida);
          d.setMinutes(d.getMinutes() + e.minutos);
          const p = (n: number) => String(n).padStart(2, '0');
          this.form.horaEstimadaLlegada = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
        }
      },
      error: err => this.alerts.error(this.store.error(err))
    });
  }
  cambioSalida() {
    const s = this.store.getSalida(this.form.salidaId);
    if (s && !this.form.paxProgramados) this.form.paxProgramados = Math.max(1, s.cuposMaximos - s.cuposDisponibles) || null;
    if (s && !this.form.horaProgramadaSalida) this.form.horaProgramadaSalida = s.fechaInicio + 'T08:00';
  }

  nueva() {
    this.editId = null;
    this.proveedorSel = this.store.proveedores().find(p => p.esPropio)?.proveedorId ?? null;
    this.estimacion.set(null);
    this.form = this.vacio();
    this.cambioSalida();
    this.v.reiniciar();
    this.ventana.set('form');
  }

  editar(o: Operacion) {
    this.editId = o.operacionId;
    this.proveedorSel = o.proveedorId;
    this.estimacion.set(null);
    this.form = {
      salidaId: o.salidaId, origenId: o.origenId, destinoId: o.destinoId, vehiculoId: o.vehiculoId,
      conductorId: o.conductorId, paxProgramados: o.paxProgramados, horaProgramadaSalida: o.horaProgramadaSalida.slice(0, 16),
      horaEstimadaLlegada: o.horaEstimadaLlegada ? o.horaEstimadaLlegada.slice(0, 16) : null, observaciones: o.observaciones
    };
    this.v.reiniciar();
    this.ventana.set('form');
  }

  guardar() {
    if (this.store.ocupado()) return;
    if (!this.v.valido()) { this.alerts.toast('Revisa los campos marcados en rojo', 'error'); return; }
    const f = this.form;
    const datos: OperacionForm = { ...f, horaEstimadaLlegada: f.horaEstimadaLlegada || null, observaciones: f.observaciones.trim() };
    const editando = this.editId !== null;
    this.ejecutar(this.store.guardarOperacion(datos, this.editId), editando ? 'Operación actualizada' : 'Operación creada');
  }

  // ---------- Asignación ----------
  abrirAsignar(o: Operacion) {
    this.actual.set(o);
    this.vehiculoSel = null;
    this.conductorSel = null;
    this.candidatos.set([]);
    this.cargandoCandidatos.set(true);
    this.ventana.set('asignar');
    this.store.candidatos(o.operacionId).subscribe({
      next: lista => { this.candidatos.set(lista); this.cargandoCandidatos.set(false); },
      error: err => { this.cargandoCandidatos.set(false); this.alerts.error(this.store.error(err)); }
    });
  }

  vehiculosCand = computed(() => this.candidatos().filter(c => c.tipoRecurso === 'VEHICULO'));
  conductoresCand() {
    const v = this.vehiculosCand().find(c => c.recursoId === this.vehiculoSel);
    return this.candidatos().filter(c => c.tipoRecurso === 'CONDUCTOR' && (!v || c.proveedorId === v.proveedorId));
  }
  elegirVehiculo(c: RecursoCandidato) {
    if (c.motivo) return;
    this.vehiculoSel = c.recursoId;
    const cond = this.candidatos().find(x => x.recursoId === this.conductorSel && x.tipoRecurso === 'CONDUCTOR');
    if (cond && cond.proveedorId !== c.proveedorId) this.conductorSel = null;
  }
  elegirConductor(c: RecursoCandidato) { if (!c.motivo) this.conductorSel = c.recursoId; }

  asignarAutomatico() {
    const o = this.actual();
    if (o) this.ejecutar(this.store.asignarAutomatico(o.operacionId), 'Recursos asignados automáticamente');
  }
  asignarManual() {
    const o = this.actual();
    if (!o || !this.vehiculoSel || !this.conductorSel) { this.alerts.toast('Elige un vehículo y un conductor', 'error'); return; }
    this.ejecutar(this.store.asignarRecursos(o.operacionId, this.vehiculoSel, this.conductorSel), 'Recursos asignados');
  }

  // ---------- Estados ----------
  abrirEstado(o: Operacion, estado?: EstadoOperacion) {
    this.actual.set(o);
    this.nuevoEstado = estado ?? this.siguientes(o)[0] ?? null;
    this.observacion = '';
    this.ventana.set('estado');
  }
  cambiarEstado() {
    const o = this.actual();
    if (!o || !this.nuevoEstado) return;
    if (this.observacion.length > 200) { this.alerts.toast('La observación admite máximo 200 caracteres', 'error'); return; }
    this.ejecutar(this.store.cambiarEstadoOperacion(o.operacionId, this.nuevoEstado, this.observacion.trim()),
      `${o.codigoOperacion}: ${etiqueta(this.nuevoEstado)}`);
  }

  verHistorial(o: Operacion) {
    this.actual.set(o);
    this.historial.set([]);
    this.ventana.set('historial');
    this.store.historialOperacion(o.operacionId).subscribe({
      next: h => this.historial.set(h),
      error: err => this.alerts.error(this.store.error(err))
    });
  }

  // Tras cualquier cambio: el tablero y el faltante de transporte de las salidas
  private ejecutar(peticion: Observable<unknown>, mensaje: string) {
    if (this.store.ocupado()) return;
    this.store.ocupado.set(true);
    peticion.subscribe({
      next: (r) => {
        const op = r as { operacionId?: number } | null;
        if (op?.operacionId) this.store.resaltar('operacion-' + op.operacionId);
        this.store.ocupado.set(false);
        this.store.cargarOperaciones();
        this.store.cargarSalidas();
        this.ventana.set(null);
        this.alerts.toast(mensaje);
      },
      error: err => {
        this.store.ocupado.set(false);
        this.alerts.error(this.store.error(err), 'La base de datos rechazó la operación');
      }
    });
  }

  filtrar(evento: Event, tipo: string) { filtrarEscritura(evento, tipo); }
  pegar(evento: Event, tipo: string) { filtrarPegado(evento, tipo); }
  rueda(evento: Event) { sinRueda(evento); }
}
