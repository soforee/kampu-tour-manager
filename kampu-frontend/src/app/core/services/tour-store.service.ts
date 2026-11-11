import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal, WritableSignal } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Cliente, ClienteForm, ContactoReserva, EstadoOperacion, EstadoOperacionInfo, EstadoTransaccion, EstimacionDuracion,
  HistorialOperacion, Lugar, LugarForm, Manifiesto, Moneda, Operacion, OperacionForm, PaqueteForm, PaqueteTour, Pago, PasajeroDraft,
  PasajeroReserva, Permiso, Personal, PersonalForm, Proveedor, ProveedorForm, RecursoCandidato, Reserva, ResumenPaquete, Rol,
  SalidaForm, SalidaProgramada, Sesion, Vehiculo, VehiculoForm
} from '../models';
import { redondear } from '../utils/moneda';
import { API, mensajeError } from '../utils/api';
import { AuthService } from './auth.service';

// Permiso que exige cada lista (mismo mapeo que SeguridadInterceptor en el backend).
// null = catálogo público. Los permisos de la sesión vienen de PKG_SEGURIDAD_ROLES.permisos.
const LISTAS: { [lista: string]: Permiso | null } = {
  paquetes: null,
  salidas: null,
  proveedores: 'VER_LOGISTICA',
  personal: 'VER_LOGISTICA',
  vehiculos: 'VER_LOGISTICA',
  lugares: 'VER_LOGISTICA',
  operaciones: 'VER_OPERACIONES',
  'operaciones/estados': 'VER_OPERACIONES',
  'resumen-paquetes': 'VER_RESUMEN',
  clientes: 'VER_CLIENTES',
  reservas: 'VER_RESERVAS',
  pasajeros: 'VER_RESERVAS',
  pagos: 'VER_MONTOS'
};

@Injectable({ providedIn: 'root' })
export class TourStore {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  paquetes = signal<PaqueteTour[]>([]);
  // Proveedores (Kampu Tours propio + terceros), su personal y su flota
  proveedores = signal<Proveedor[]>([]);
  personal = signal<Personal[]>([]);
  vehiculos = signal<Vehiculo[]>([]);
  lugares = signal<Lugar[]>([]);
  // Tráfico
  operaciones = signal<Operacion[]>([]);
  estadosOperacion = signal<EstadoOperacionInfo[]>([]);
  // Comercial
  clientes = signal<Cliente[]>([]);
  salidas = signal<SalidaProgramada[]>([]);
  reservas = signal<Reserva[]>([]);
  pasajeros = signal<PasajeroReserva[]>([]);
  pagos = signal<Pago[]>([]);
  // Vista VW_RESUMEN_PAQUETES_POR_ROL
  resumen = signal<ResumenPaquete[]>([]);

  // Personal que puede guiar o conducir
  guias = computed(() => this.personal().filter(p => p.rol !== 'CONDUCTOR'));
  conductores = computed(() => this.personal().filter(p => p.rol !== 'GUIA'));

  ocupado = signal(false);
  cargado = signal(false);

  private cargando = false;
  private pendientes = 0;
  private errorCarga = '';
  private avisos: ((error: string) => void)[] = [];

  error(e: unknown): string {
    return mensajeError(e);
  }

  // ¿La sesión puede leer esta lista?
  puede(lista: string): boolean {
    if (!(lista in LISTAS)) return false;
    const permiso = LISTAS[lista];
    return this.auth.autenticado() && (permiso === null || this.auth.puede(permiso));
  }

  cargar(forzar = false, alTerminar?: (error: string) => void) {
    if (alTerminar) this.avisos.push(alTerminar);
    if (this.cargado() && !forzar) {
      this.avisar();
      return;
    }
    if (this.cargando) return;
    const tareas: [string, WritableSignal<any[]>][] = [
      ['paquetes', this.paquetes], ['salidas', this.salidas],
      ['proveedores', this.proveedores], ['personal', this.personal], ['vehiculos', this.vehiculos],
      ['lugares', this.lugares], ['operaciones', this.operaciones], ['operaciones/estados', this.estadosOperacion],
      ['resumen-paquetes', this.resumen], ['clientes', this.clientes], ['reservas', this.reservas],
      ['pasajeros', this.pasajeros], ['pagos', this.pagos]
    ];
    const permitidas = tareas.filter(([url]) => this.puede(url));
    this.cargando = true;
    this.errorCarga = '';
    this.pendientes = permitidas.length;
    if (this.pendientes === 0) { this.terminoTabla(); return; }
    for (const [url, destino] of permitidas) this.cargarLista(url, destino, true);
  }

  private terminoTabla(e?: unknown) {
    if (e !== undefined && !this.errorCarga) this.errorCarga = this.error(e);
    this.pendientes--;
    if (this.pendientes > 0) return;
    this.cargando = false;
    this.cargado.set(this.errorCarga === '');
    this.avisar();
  }

  private avisar() {
    const lista = this.avisos;
    this.avisos = [];
    for (const aviso of lista) aviso(this.errorCarga);
  }

  limpiar() {
    this.cargado.set(false);
    this.cargando = false;
    for (const s of [this.paquetes, this.proveedores, this.personal, this.vehiculos, this.lugares,
      this.operaciones, this.estadosOperacion, this.clientes, this.salidas, this.reservas, this.pasajeros, this.pagos,
      this.resumen] as WritableSignal<unknown[]>[]) {
      s.set([]);
    }
  }

  private cargarLista<T>(url: string, destino: WritableSignal<T[]>, contar: boolean) {
    if (!this.puede(url)) { if (contar) this.terminoTabla(); return; }
    this.http.get<T[]>(`${API}/${url}`).subscribe({
      next: data => { destino.set(data); if (contar) this.terminoTabla(); },
      error: e => { if (contar) this.terminoTabla(e); else this.error(e); }
    });
  }
  cargarPaquetes() { this.cargarLista('paquetes', this.paquetes, false); }
  cargarProveedores() { this.cargarLista('proveedores', this.proveedores, false); }
  cargarPersonal() { this.cargarLista('personal', this.personal, false); }
  cargarVehiculos() { this.cargarLista('vehiculos', this.vehiculos, false); }
  cargarLugares() { this.cargarLista('lugares', this.lugares, false); }
  cargarOperaciones() { this.cargarLista('operaciones', this.operaciones, false); }
  cargarResumen() { this.cargarLista('resumen-paquetes', this.resumen, false); }
  cargarClientes() { this.cargarLista('clientes', this.clientes, false); }
  cargarSalidas() { this.cargarLista('salidas', this.salidas, false); this.cargarResumen(); }
  cargarReservas() { this.cargarLista('reservas', this.reservas, false); }
  cargarPasajeros() { this.cargarLista('pasajeros', this.pasajeros, false); }
  cargarPagos() { this.cargarLista('pagos', this.pagos, false); }

  getPaquete(id?: number | null) { return this.paquetes().find(p => p.paqueteId === id); }
  getGuia(id?: number | null) { return this.personal().find(p => p.personalId === id); }
  getPersonal(id?: number | null) { return this.personal().find(p => p.personalId === id); }
  getProveedor(id?: number | null) { return this.proveedores().find(p => p.proveedorId === id); }
  getLugar(id?: number | null) { return this.lugares().find(l => l.lugarId === id); }
  getVehiculo(id?: number | null) { return this.vehiculos().find(v => v.vehiculoId === id); }
  getOperacion(id?: number | null) { return this.operaciones().find(o => o.operacionId === id); }
  getEstadoOperacion(codigo?: string | null) { return this.estadosOperacion().find(e => e.codigo === codigo); }
  getCliente(id?: number | null) { return this.clientes().find(c => c.clienteId === id); }
  getSalida(id?: number | null) { return this.salidas().find(s => s.salidaId === id); }
  getReserva(id?: number | null) { return this.reservas().find(r => r.reservaId === id); }
  operacionesDe(salidaId: number) { return this.operaciones().filter(o => o.salidaId === salidaId); }
  nombreLugar(id?: number | null): string {
    const l = this.getLugar(id);
    return l ? `${l.nombre} (${l.ciudad})` : '';
  }
  nombreProveedor(id?: number | null): string { return this.getProveedor(id)?.nombreComercial ?? '—'; }

  nombreCliente(r?: Reserva | null): string {
    return r ? this.getCliente(r.clienteId)?.nombreCompleto ?? '' : '';
  }
  pasajerosDe(reservaId: number) { return this.pasajeros().filter(p => p.reservaId === reservaId); }
  pagosDe(reservaId: number) { return this.pagos().filter(p => p.reservaId === reservaId); }
  salidasDe(paqueteId: number) { return this.salidas().filter(s => s.paqueteId === paqueteId); }

  codigoReserva(id: number) { return 'R-' + String(id).padStart(4, '0'); }
  saldo(r: Reserva): number {
    return r.estadoReserva === 'ANULADA' ? 0 : Math.max(redondear(r.montoTotal - r.montoPagado), 0);
  }
  cuposOcupados(salidaId: number): number {
    const s = this.getSalida(salidaId);
    return s ? s.cuposMaximos - s.cuposDisponibles : 0;
  }

  private guardarEn<T>(url: string, d: unknown, id: number | null): Observable<T> {
    return id ? this.http.put<T>(`${API}/${url}/${id}`, d) : this.http.post<T>(`${API}/${url}`, d);
  }

  guardarPaquete(d: PaqueteForm, id: number | null) { return this.guardarEn<PaqueteTour>('paquetes', d, id); }
  eliminarPaquete(id: number) { return this.http.delete<void>(`${API}/paquetes/${id}`); }

  // ---------- Proveedores, personal, flota y lugares ----------
  guardarProveedor(d: ProveedorForm, id: number | null) { return this.guardarEn<Proveedor>('proveedores', d, id); }
  eliminarProveedor(id: number) { return this.http.delete<void>(`${API}/proveedores/${id}`); }
  guardarPersonal(d: PersonalForm, id: number | null) { return this.guardarEn<Personal>('personal', d, id); }
  eliminarPersonal(id: number) { return this.http.delete<void>(`${API}/personal/${id}`); }
  guardarVehiculo(d: VehiculoForm, id: number | null) { return this.guardarEn<Vehiculo>('vehiculos', d, id); }
  eliminarVehiculo(id: number) { return this.http.delete<void>(`${API}/vehiculos/${id}`); }
  guardarLugar(d: LugarForm, id: number | null) { return this.guardarEn<Lugar>('lugares', d, id); }
  eliminarLugar(id: number) { return this.http.delete<void>(`${API}/lugares/${id}`); }

  // ---------- Tráfico ----------
  guardarOperacion(d: OperacionForm, id: number | null) { return this.guardarEn<Operacion>('operaciones', d, id); }
  cambiarEstadoOperacion(id: number, estado: EstadoOperacion, observacion: string): Observable<Operacion> {
    return this.http.patch<Operacion>(`${API}/operaciones/${id}/estado`, { estado: estado, observacion: observacion });
  }
  asignarAutomatico(id: number): Observable<Operacion> {
    return this.http.post<Operacion>(`${API}/operaciones/${id}/asignar-automatico`, {});
  }
  asignarRecursos(id: number, vehiculoId: number, conductorId: number): Observable<Operacion> {
    return this.http.patch<Operacion>(`${API}/operaciones/${id}/asignar`, { vehiculoId: vehiculoId, conductorId: conductorId });
  }
  candidatos(id: number): Observable<RecursoCandidato[]> {
    return this.http.get<RecursoCandidato[]>(`${API}/operaciones/${id}/candidatos`);
  }
  historialOperacion(id: number): Observable<HistorialOperacion[]> {
    return this.http.get<HistorialOperacion[]>(`${API}/operaciones/${id}/historial`);
  }
  // Duración con datos reales (OSRM / coordenadas de los lugares)
  estimarDuracion(origenId: number, destinoId: number): Observable<EstimacionDuracion> {
    return this.http.get<EstimacionDuracion>(`${API}/operaciones/estimar-duracion`,
      { params: { origenId: origenId, destinoId: destinoId } });
  }

  // Autocompletado: busca por nombre, DNI/pasaporte o correo (mínimo 2 caracteres)
  buscarClientes(texto: string): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(`${API}/clientes/buscar`, { params: { q: texto } });
  }
  guardarCliente(d: ClienteForm, id: number | null) { return this.guardarEn<Cliente>('clientes', d, id); }
  eliminarCliente(id: number) { return this.http.delete<void>(`${API}/clientes/${id}`); }

  guardarSalida(d: SalidaForm, id: number | null) { return this.guardarEn<SalidaProgramada>('salidas', d, id); }
  eliminarSalida(id: number) { return this.http.delete<void>(`${API}/salidas/${id}`); }
  // Operador / Ventas: salida en ejecución (CONFIRMADA) o terminada (COMPLETADA)
  marcarEjecucion(id: number, estado: 'CONFIRMADA' | 'COMPLETADA'): Observable<SalidaProgramada> {
    return this.http.patch<SalidaProgramada>(`${API}/salidas/${id}/ejecucion`, { estado: estado });
  }

  // Lista de pasajeros de la salida (para el guía y el conductor)
  manifiesto(salidaId: number): Observable<Manifiesto> {
    return this.http.get<Manifiesto>(`${API}/salidas/${salidaId}/manifiesto`);
  }

  // Se envía clienteId (cliente registrado) o contacto (cliente nuevo), nunca ambos
  crearReserva(clienteId: number | null, contacto: ContactoReserva | null, salidaId: number, observaciones: string,
               pasajeros: PasajeroDraft[]): Observable<Reserva> {
    const lista = pasajeros.map(p => ({
      nombreCompleto: p.nombreCompleto, tipoDocumento: p.tipoDocumento, numeroDocumento: p.numeroDocumento,
      nacionalidad: p.nacionalidad, fechaNacimiento: p.fechaNacimiento ? p.fechaNacimiento : null
    }));
    const cuerpo = { clienteId: clienteId, contacto: contacto, salidaId: salidaId, observaciones: observaciones, pasajeros: lista };
    return this.http.post<Reserva>(`${API}/reservas`, cuerpo);
  }

  urlBoleta(id: number): string { return `${API}/reservas/${id}/boleta`; }
  verBoleta(id: number): Observable<string> {
    return this.http.get(`${API}/reservas/${id}/boleta`, { responseType: 'text' });
  }
  enviarBoleta(id: number): Observable<{ destinatario: string; enviadaEn: string }> {
    return this.http.post<{ destinatario: string; enviadaEn: string }>(`${API}/reservas/${id}/boleta`, {});
  }
  confirmarReserva(id: number): Observable<Reserva> {
    return this.http.patch<Reserva>(`${API}/reservas/${id}/confirmar`, {});
  }
  completarReserva(id: number): Observable<Reserva> {
    return this.http.patch<Reserva>(`${API}/reservas/${id}/completar`, {});
  }
  anularReserva(id: number, formaReembolso: string | null): Observable<Reserva> {
    return this.http.patch<Reserva>(`${API}/reservas/${id}/anular`, { formaReembolso: formaReembolso, usuarioId: this.auth.usuario()?.usuarioId ?? null });
  }

  registrarPago(reservaId: number, monto: number, moneda: Moneda, metodoPago: string,
                codigoTransaccion: string, estadoTransaccion: EstadoTransaccion): Observable<Pago> {
    const cuerpo = {
      reservaId: reservaId, monto: monto, moneda: moneda, metodoPago: metodoPago,
      codigoTransaccion: codigoTransaccion, estadoTransaccion: estadoTransaccion
    };
    return this.http.post<Pago>(`${API}/pagos`, cuerpo);
  }
  resolverPago(pagoId: number, estado: 'APROBADO' | 'RECHAZADO'): Observable<Pago> {
    return this.http.patch<Pago>(`${API}/pagos/${pagoId}/resolver`, { estado: estado });
  }
}
