import { AuthService } from '../../core/services/auth.service';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { EstadoOperacion, EstimacionDuracion, HistorialOperacion, Operacion, OperacionForm, RecursoCandidato, TipoServicio, TipoVehiculo } from '../../core/models';
import { TourStore } from '../../core/services/tour-store.service';
import { AlertService } from '../../core/services/alert.service';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';
import { ModalComponent } from '../../shared/modal.component';
import { filtrarEscritura, filtrarPegado, sinRueda } from '../../core/utils/filtro';
import { FormValidacion, V } from '../../core/utils/validacion';
import { etiqueta } from '../../core/utils/etiquetas';
import { hoy } from '../../core/utils/date.util';
import { aInputFecha, CERRADOS, EN_TRANSITO, estaRetrasada, mismoDia, ocupaAhora, POR_SALIR, salidaEfectiva } from '../../core/utils/trafico';
import { TableroFidsComponent } from './tablero-fids.component';
import { CalendarioOperacionesComponent } from './calendario-operaciones.component';
import { FlotaDisponibilidadComponent } from './flota-disponibilidad.component';

type Grupo = '' | 'PENDIENTES' | 'POR_CONFIRMAR' | 'EN_CURSO' | 'RETRASADAS' | 'CONFLICTOS' | 'CERRADAS';
type Ventana = 'form' | 'asignar' | 'estado' | 'historial' | 'responder' | null;
type Vista = 'panel' | 'tablero' | 'calendario' | 'flota';

const EN_CURSO: EstadoOperacion[] = ['ASIGNADO', 'EMBARQUE', 'EN_ESPERA_SALIDA', 'EN_RUTA', 'LLEGADA_PROXIMA'];

/**
 * Centro de operaciones de tráfico (OPERACIONES_TRANSPORTE), con varias empresas de transporte.
 * Vistas: panel (indicadores, filtros y tabla), tablero de salidas/llegadas, calendario semanal
 * y disponibilidad de la flota. Las reglas las aplica el backend: transiciones, cruces de horario,
 * reposicionamiento, licencias, horas del conductor, mantenimiento, capacidad, confirmación de la
 * empresa e historial. La empresa proveedora (rol PROVEEDOR) ve solo sus servicios y los acepta o rechaza.
 */
@Component({
  selector: 'app-operaciones',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink, BadgeComponent, IconComponent, ModalComponent,
    TableroFidsComponent, CalendarioOperacionesComponent, FlotaDisponibilidadComponent],
  templateUrl: './operaciones.component.html'
})
export class OperacionesComponent implements OnDestroy {
  store = inject(TourStore);
  private alerts = inject(AlertService);
  // GESTIONAR_OPERACIONES: Admin y Operador. Ventas solo consulta el tráfico (VER_OPERACIONES)
  private auth = inject(AuthService);
  gestiona = this.auth.puede('GESTIONAR_OPERACIONES');
  // RESPONDER_SERVICIO: la empresa proveedora (y el operador por ella) acepta o rechaza
  responde = this.auth.puede('RESPONDER_SERVICIO');
  esProveedor = this.auth.esProveedor();
  sesion = this.auth.usuario();
  etiqueta = etiqueta;
  hoy = hoy;

  vistas: { valor: Vista; texto: string; icono: string }[] = [
    { valor: 'panel', texto: 'Panel de tráfico', icono: 'chart' }, { valor: 'tablero', texto: 'Salidas y llegadas', icono: 'flag' },
    { valor: 'calendario', texto: 'Calendario', icono: 'calendar' }, { valor: 'flota', texto: 'Flota y empresas', icono: 'truck' }
  ];
  vista = signal<Vista>('panel');

  grupos: { valor: Grupo; texto: string }[] = [
    { valor: '', texto: 'Todas' }, { valor: 'PENDIENTES', texto: 'Por asignar' }, { valor: 'POR_CONFIRMAR', texto: 'Por confirmar' },
    { valor: 'EN_CURSO', texto: 'En curso' }, { valor: 'RETRASADAS', texto: 'Retrasadas' }, { valor: 'CONFLICTOS', texto: 'Conflictos' },
    { valor: 'CERRADAS', texto: 'Cerradas' }
  ];
  grupo = signal<Grupo>('');
  filtroSalida = signal<number | null>(null);
  // Filtros del centro de operaciones
  fFecha = signal<string>(hoy());
  fProveedor = signal<number | null>(null);
  fVehiculo = signal<number | null>(null);
  fConductor = signal<number | null>(null);
  fEstado = signal<EstadoOperacion | ''>('');
  fRuta = signal('');
  tiposServicio: TipoServicio[] = ['TRASLADO', 'TOUR', 'INTERPROVINCIAL'];
  tiposVehiculo: TipoVehiculo[] = ['AUTO', 'CAMIONETA_4X4', 'VAN', 'MINIBUS', 'BUS'];
  estadosLista: EstadoOperacion[] = ['PROGRAMADO', 'ASIGNADO', 'EMBARQUE', 'EN_ESPERA_SALIDA', 'EN_RUTA', 'LLEGADA_PROXIMA', 'FINALIZADO', 'RETRASADO', 'CANCELADO'];
  // Reloj para los indicadores y recarga automática del tablero (30 s)
  ahora = signal(new Date());
  private recarga = setInterval(() => {
    this.ahora.set(new Date());
    if (!this.ventana() && !this.store.ocupado()) { this.store.cargarOperaciones(); if (this.vista() === 'flota') this.store.cargarProveedores(); }
  }, 30000);
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

  // Cambio de estado (RETRASADO pide la nueva hora de salida)
  nuevoEstado: EstadoOperacion | null = null;
  observacion = '';
  nuevaHoraSalida = '';

  // Respuesta de la empresa
  aceptar = true;
  motivoRechazo = '';

  historial = signal<HistorialOperacion[]>([]);

  constructor() {
    const salida = Number(inject(ActivatedRoute).snapshot.queryParamMap.get('salida'));
    if (salida) { this.filtroSalida.set(salida); this.fFecha.set(''); }
    this.store.cargar();
  }

  ngOnDestroy() { clearInterval(this.recarga); }

  // ---------- Filtros ----------
  // Empresa, vehículo, conductor, ruta y estado (sin la fecha: el tablero y el calendario usan su propio rango)
  filtradas = computed(() => {
    const prov = this.fProveedor(), veh = this.fVehiculo(), cond = this.fConductor(), est = this.fEstado();
    const ruta = this.fRuta().trim().toLowerCase();
    const s = this.filtroSalida();
    return this.store.operaciones()
      .filter(o => !s || o.salidaId === s)
      .filter(o => !prov || o.proveedorId === prov || (!o.proveedorId && o.proveedorPrefId === prov))
      .filter(o => !veh || o.vehiculoId === veh)
      .filter(o => !cond || o.conductorId === cond)
      .filter(o => !est || o.estadoActual === est)
      .filter(o => !ruta || [o.codigoOperacion, o.origen, o.destino, o.ruta, o.ciudadOrigen, o.ciudadDestino]
        .some(t => (t ?? '').toLowerCase().includes(ruta)));
  });
  // Del día elegido (vacío = todas las fechas); en curso y retrasadas siempre se muestran
  delDia = computed(() => {
    const f = this.fFecha();
    return this.filtradas().filter(o => !f || mismoDia(o.horaProgramadaSalida, f)
      || (f === hoy() && (EN_TRANSITO.includes(o.estadoActual) || o.estadoActual === 'RETRASADO')));
  });
  hayFiltros = computed(() => !!(this.fProveedor() || this.fVehiculo() || this.fConductor() || this.fEstado() || this.fRuta() || this.fFecha() !== hoy()));
  limpiarFiltros() {
    this.fProveedor.set(null); this.fVehiculo.set(null); this.fConductor.set(null);
    this.fEstado.set(''); this.fRuta.set(''); this.fFecha.set(hoy()); this.filtroSalida.set(null); this.grupo.set('');
  }
  vehiculosFiltro = computed(() => this.store.vehiculos().filter(v => !this.fProveedor() || v.proveedorId === this.fProveedor()));
  conductoresFiltro = computed(() => this.store.conductores().filter(c => !this.fProveedor() || c.proveedorId === this.fProveedor()));
  proveedoresFiltro = computed(() => this.store.proveedores().filter(p => !this.fProveedor() || p.proveedorId === this.fProveedor()));

  // ---------- Tabla del panel ----------
  // Colas de trabajo (por asignar, por confirmar, conflictos): todas las fechas, no solo el día elegido
  esCola = computed(() => ['PENDIENTES', 'POR_CONFIRMAR', 'CONFLICTOS'].includes(this.grupo()));
  filas = computed(() => {
    const g = this.grupo();
    const ahora = this.ahora();
    return (this.esCola() ? this.filtradas() : this.delDia())
      .filter(o => {
        if (g === 'PENDIENTES') return !o.vehiculoId && !CERRADOS.includes(o.estadoActual);
        if (g === 'POR_CONFIRMAR') return o.confirmacion === 'PENDIENTE';
        if (g === 'EN_CURSO') return EN_CURSO.includes(o.estadoActual);
        if (g === 'RETRASADAS') return estaRetrasada(o, ahora);
        if (g === 'CONFLICTOS') return !!o.conflicto;
        if (g === 'CERRADAS') return CERRADOS.includes(o.estadoActual);
        return true;
      })
      .map(o => ({ op: o, retrasada: estaRetrasada(o, ahora), salida: this.store.getSalida(o.salidaId), paquete: this.store.getPaquete(this.store.getSalida(o.salidaId)?.paqueteId) }));
  });

  // ---------- Indicadores (dashboard del tráfico) ----------
  resumen = computed(() => {
    const ahora = this.ahora();
    const en2h = ahora.getTime() + 2 * 3600000;
    const dia = this.delDia();
    const todas = this.filtradas();
    const vigentes = todas.filter(o => !CERRADOS.includes(o.estadoActual));
    const vehiculos = this.vehiculosFiltro().filter(v => v.estado === 'OPERATIVO');
    const ocupados = new Set(vigentes.filter(o => ocupaAhora(o, ahora)).map(o => o.vehiculoId));
    const conductoresOcupados = new Set(vigentes.filter(o => ocupaAhora(o, ahora)).map(o => o.conductorId));
    return {
      total: dia.filter(o => o.estadoActual !== 'CANCELADO').length,
      enRuta: vigentes.filter(o => EN_TRANSITO.includes(o.estadoActual)).length,
      proximasSalidas: vigentes.filter(o => POR_SALIR.includes(o.estadoActual) && salidaEfectiva(o).getTime() >= ahora.getTime() - 15 * 60000
        && salidaEfectiva(o).getTime() <= en2h).length,
      proximasLlegadas: vigentes.filter(o => EN_TRANSITO.includes(o.estadoActual) && new Date(o.horaEstimadaLlegada).getTime() <= en2h).length,
      vehiculosLibres: vehiculos.filter(v => !ocupados.has(v.vehiculoId)).length,
      vehiculosOcupados: vehiculos.filter(v => ocupados.has(v.vehiculoId)).length,
      conductoresLibres: this.conductoresFiltro().filter(c => c.estado === 'DISPONIBLE' && !conductoresOcupados.has(c.personalId)).length,
      empresasActivas: this.proveedoresFiltro().filter(p => p.estado === 'ACTIVO').length,
      retrasadas: vigentes.filter(o => estaRetrasada(o, ahora)).length,
      finalizadas: dia.filter(o => o.estadoActual === 'FINALIZADO').length,
      porAsignar: vigentes.filter(o => !o.vehiculoId).length,
      porConfirmar: vigentes.filter(o => o.confirmacion === 'PENDIENTE').length,
      conflictos: vigentes.filter(o => !!o.conflicto).length
    };
  });

  contadoresFids = computed(() => {
    const r = this.resumen();
    return { transito: r.enRuta, retrasados: r.retrasadas, disponibles: r.vehiculosLibres, ocupados: r.vehiculosOcupados, empresas: r.empresasActivas };
  });

  cambiarVista(v: Vista) {
    this.vista.set(v);
    if (v === 'flota') this.store.cargarProveedores();
  }

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
      paxProgramados: null, horaProgramadaSalida: '', horaEstimadaLlegada: null, observaciones: '',
      tipoServicio: 'TRASLADO', ruta: '', tipoVehiculoReq: null, proveedorPrefId: null };
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
    this.store.estimarDuracion(f.origenId, f.destinoId, f.tipoServicio).subscribe({
      next: e => {
        this.estimacion.set(e);
        if (e.minutos && f.horaProgramadaSalida) {
          const d = new Date(f.horaProgramadaSalida);
          d.setMinutes(d.getMinutes() + e.minutos);
          this.form.horaEstimadaLlegada = aInputFecha(d);
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
    this.proveedorSel = null;
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
      horaEstimadaLlegada: o.horaEstimadaLlegada ? o.horaEstimadaLlegada.slice(0, 16) : null, observaciones: o.observaciones,
      tipoServicio: o.tipoServicio, ruta: o.ruta, tipoVehiculoReq: o.tipoVehiculoReq, proveedorPrefId: o.proveedorPrefId
    };
    this.v.reiniciar();
    this.ventana.set('form');
  }

  guardar() {
    if (this.store.ocupado()) return;
    if (!this.v.valido()) { this.alerts.toast('Revisa los campos marcados en rojo', 'error'); return; }
    const f = this.form;
    const datos: OperacionForm = { ...f, horaEstimadaLlegada: f.horaEstimadaLlegada || null, observaciones: f.observaciones.trim(), ruta: f.ruta.trim() };
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
  // Asigna en orden de salida todas las operaciones futuras sin vehículo (del día filtrado o todas)
  async asignarPendientes() {
    if (this.store.ocupado()) return;
    const fecha = this.fFecha() || null;
    const texto = fecha ? `las operaciones pendientes del ${fecha.split('-').reverse().join('/')}` : 'todas las operaciones pendientes';
    if (!await this.alerts.confirm(`Se buscará vehículo y conductor para ${texto}, en orden de salida y con las reglas de prioridad.`,
      'Asignación automática', 'Asignar')) return;
    this.store.ocupado.set(true);
    this.store.asignarPendientes(fecha).subscribe({
      next: r => {
        this.store.ocupado.set(false);
        this.store.cargarOperaciones();
        this.store.cargarSalidas();
        if (r.revisadas === 0) { this.alerts.toast('No hay operaciones pendientes de asignar', 'info'); return; }
        const detalle = r.sinAsignar.length ? '\n\nSin asignar:\n• ' + r.sinAsignar.join('\n• ') : '';
        if (r.sinAsignar.length) this.alerts.warning(`${r.asignadas.length} de ${r.revisadas} asignadas.${detalle}`, 'Asignación automática');
        else this.alerts.success(`Operaciones asignadas: ${r.asignadas.length} de ${r.revisadas}.`, 'Asignación automática');
      },
      error: err => { this.store.ocupado.set(false); this.alerts.error(this.store.error(err)); }
    });
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
  // Desde RETRASADO también se puede volver a reprogramar la salida
  opcionesEstado(o: Operacion): EstadoOperacion[] {
    const s = this.siguientes(o);
    return o.estadoActual === 'RETRASADO' ? ['RETRASADO', ...s] : s;
  }
  abrirEstado(o: Operacion, estado?: EstadoOperacion) {
    this.actual.set(o);
    this.nuevoEstado = estado ?? this.opcionesEstado(o)[0] ?? null;
    this.observacion = '';
    // Propuesta: 30 min después de la salida vigente
    const base = salidaEfectiva(o);
    this.nuevaHoraSalida = aInputFecha(new Date(Math.max(base.getTime(), Date.now()) + 30 * 60000));
    this.ventana.set('estado');
  }
  cambiarEstado() {
    const o = this.actual();
    if (!o || !this.nuevoEstado) return;
    if (this.observacion.length > 200) { this.alerts.toast('La observación admite máximo 200 caracteres', 'error'); return; }
    const retraso = this.nuevoEstado === 'RETRASADO';
    if (retraso && (!this.nuevaHoraSalida || new Date(this.nuevaHoraSalida) <= new Date(o.horaProgramadaSalida))) {
      this.alerts.toast('La nueva salida debe ser posterior a la programada', 'error');
      return;
    }
    this.ejecutar(this.store.cambiarEstadoOperacion(o.operacionId, this.nuevoEstado, this.observacion.trim(), retraso ? this.nuevaHoraSalida : null),
      `${o.codigoOperacion}: ${etiqueta(this.nuevoEstado)}`);
  }

  // ---------- Respuesta de la empresa proveedora ----------
  abrirResponder(o: Operacion, aceptar: boolean) {
    this.actual.set(o);
    this.aceptar = aceptar;
    this.motivoRechazo = '';
    this.ventana.set('responder');
  }
  responder() {
    const o = this.actual();
    if (!o) return;
    if (!this.aceptar && !this.motivoRechazo.trim()) { this.alerts.toast('Indica el motivo del rechazo', 'error'); return; }
    this.ejecutar(this.store.responderServicio(o.operacionId, this.aceptar, this.motivoRechazo.trim()),
      this.aceptar ? `${o.codigoOperacion}: servicio confirmado` : `${o.codigoOperacion}: servicio rechazado`);
  }

  // Desde el calendario o la flota: abre el historial (o la asignación si está pendiente)
  abrirDesdeVista(o: Operacion) {
    if (this.gestiona && !o.vehiculoId && !CERRADOS.includes(o.estadoActual)) this.abrirAsignar(o);
    else this.verHistorial(o);
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
      next: () => {
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
