import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Cliente, ClienteForm, EstadoTransaccion, Guia, GuiaForm, Moneda, PaqueteForm, PaqueteTour, Pago, PasajeroDraft,
  PasajeroReserva, Reserva, SalidaForm, SalidaProgramada, Sesion, Transporte, TransporteForm
} from '../models';
import { redondear } from '../utils/moneda';
import { API, mensajeError } from '../utils/api';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class TourStore {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  usuarios = signal<Sesion[]>([]);
  paquetes = signal<PaqueteTour[]>([]);
  guias = signal<Guia[]>([]);
  transportes = signal<Transporte[]>([]);
  clientes = signal<Cliente[]>([]);
  salidas = signal<SalidaProgramada[]>([]);
  reservas = signal<Reserva[]>([]);
  pasajeros = signal<PasajeroReserva[]>([]);
  pagos = signal<Pago[]>([]);

  ocupado = signal(false);
  cargado = signal(false);

  private cargando = false;
  private pendientes = 0;
  private errorCarga = '';
  private avisos: ((error: string) => void)[] = [];

  error(e: unknown): string {
    return mensajeError(e);
  }

  cargar(forzar = false, alTerminar?: (error: string) => void) {
    if (alTerminar) this.avisos.push(alTerminar);
    if (this.cargado() && !forzar) {
      this.avisar();
      return;
    }
    if (this.cargando) return;
    this.cargando = true;
    this.errorCarga = '';
    this.pendientes = 9;
    this.cargarUsuarios(true);
    this.cargarPaquetes(true);
    this.cargarGuias(true);
    this.cargarTransportes(true);
    this.cargarClientes(true);
    this.cargarSalidas(true);
    this.cargarReservas(true);
    this.cargarPasajeros(true);
    this.cargarPagos(true);
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
    this.usuarios.set([]);
    this.paquetes.set([]);
    this.guias.set([]);
    this.transportes.set([]);
    this.clientes.set([]);
    this.salidas.set([]);
    this.reservas.set([]);
    this.pasajeros.set([]);
    this.pagos.set([]);
  }

  cargarUsuarios(contar = false) {
    this.http.get<Sesion[]>(`${API}/usuarios`).subscribe({
      next: data => { this.usuarios.set(data); if (contar) this.terminoTabla(); },
      error: e => { if (contar) this.terminoTabla(e); else this.error(e); }
    });
  }
  cargarPaquetes(contar = false) {
    this.http.get<PaqueteTour[]>(`${API}/paquetes`).subscribe({
      next: data => { this.paquetes.set(data); if (contar) this.terminoTabla(); },
      error: e => { if (contar) this.terminoTabla(e); else this.error(e); }
    });
  }
  cargarGuias(contar = false) {
    this.http.get<Guia[]>(`${API}/guias`).subscribe({
      next: data => { this.guias.set(data); if (contar) this.terminoTabla(); },
      error: e => { if (contar) this.terminoTabla(e); else this.error(e); }
    });
  }
  cargarTransportes(contar = false) {
    this.http.get<Transporte[]>(`${API}/transportes`).subscribe({
      next: data => { this.transportes.set(data); if (contar) this.terminoTabla(); },
      error: e => { if (contar) this.terminoTabla(e); else this.error(e); }
    });
  }
  cargarClientes(contar = false) {
    this.http.get<Cliente[]>(`${API}/clientes`).subscribe({
      next: data => { this.clientes.set(data); if (contar) this.terminoTabla(); },
      error: e => { if (contar) this.terminoTabla(e); else this.error(e); }
    });
  }
  cargarSalidas(contar = false) {
    this.http.get<SalidaProgramada[]>(`${API}/salidas`).subscribe({
      next: data => { this.salidas.set(data); if (contar) this.terminoTabla(); },
      error: e => { if (contar) this.terminoTabla(e); else this.error(e); }
    });
  }
  cargarReservas(contar = false) {
    this.http.get<Reserva[]>(`${API}/reservas`).subscribe({
      next: data => { this.reservas.set(data); if (contar) this.terminoTabla(); },
      error: e => { if (contar) this.terminoTabla(e); else this.error(e); }
    });
  }
  cargarPasajeros(contar = false) {
    this.http.get<PasajeroReserva[]>(`${API}/pasajeros`).subscribe({
      next: data => { this.pasajeros.set(data); if (contar) this.terminoTabla(); },
      error: e => { if (contar) this.terminoTabla(e); else this.error(e); }
    });
  }
  cargarPagos(contar = false) {
    this.http.get<Pago[]>(`${API}/pagos`).subscribe({
      next: data => { this.pagos.set(data); if (contar) this.terminoTabla(); },
      error: e => { if (contar) this.terminoTabla(e); else this.error(e); }
    });
  }

  getPaquete(id?: number | null) { return this.paquetes().find(p => p.paqueteId === id); }
  getGuia(id?: number | null) { return this.guias().find(g => g.guiaId === id); }
  getTransporte(id?: number | null) { return this.transportes().find(t => t.transporteId === id); }
  getCliente(id?: number | null) { return this.clientes().find(c => c.clienteId === id); }
  getSalida(id?: number | null) { return this.salidas().find(s => s.salidaId === id); }
  getReserva(id?: number | null) { return this.reservas().find(r => r.reservaId === id); }

  pasajerosDe(reservaId: number) { return this.pasajeros().filter(p => p.reservaId === reservaId); }
  pagosDe(reservaId: number) { return this.pagos().filter(p => p.reservaId === reservaId); }
  salidasDe(paqueteId: number) { return this.salidas().filter(s => s.paqueteId === paqueteId); }
  usernameDe(id?: number | null): string {
    const u = this.usuarios().find(x => x.usuarioId === id);
    return u ? u.username : 'usuario #' + id;
  }

  codigoReserva(id: number) { return 'R-' + String(id).padStart(4, '0'); }
  saldo(r: Reserva): number {
    return r.estadoReserva === 'ANULADA' ? 0 : Math.max(redondear(r.montoTotal - r.montoPagado), 0);
  }
  cuposOcupados(salidaId: number): number {
    return this.reservas()
      .filter(r => r.salidaId === salidaId && r.estadoReserva !== 'ANULADA')
      .reduce((a, r) => a + r.numeroViajeros, 0);
  }

  guardarPaquete(d: PaqueteForm, id: number | null): Observable<PaqueteTour> {
    return id
      ? this.http.put<PaqueteTour>(`${API}/paquetes/${id}`, d)
      : this.http.post<PaqueteTour>(`${API}/paquetes`, d);
  }
  eliminarPaquete(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/paquetes/${id}`);
  }

  guardarGuia(d: GuiaForm, id: number | null): Observable<Guia> {
    return id
      ? this.http.put<Guia>(`${API}/guias/${id}`, d)
      : this.http.post<Guia>(`${API}/guias`, d);
  }
  eliminarGuia(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/guias/${id}`);
  }
  guardarTransporte(d: TransporteForm, id: number | null): Observable<Transporte> {
    return id
      ? this.http.put<Transporte>(`${API}/transportes/${id}`, d)
      : this.http.post<Transporte>(`${API}/transportes`, d);
  }
  eliminarTransporte(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/transportes/${id}`);
  }
  guardarCliente(d: ClienteForm, id: number | null): Observable<Cliente> {
    return id
      ? this.http.put<Cliente>(`${API}/clientes/${id}`, d)
      : this.http.post<Cliente>(`${API}/clientes`, d);
  }
  eliminarCliente(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/clientes/${id}`);
  }

  guardarSalida(d: SalidaForm, id: number | null): Observable<SalidaProgramada> {
    return id
      ? this.http.put<SalidaProgramada>(`${API}/salidas/${id}`, d)
      : this.http.post<SalidaProgramada>(`${API}/salidas`, d);
  }
  eliminarSalida(id: number): Observable<void> {
    return this.http.delete<void>(`${API}/salidas/${id}`);
  }

  crearReserva(clienteId: number, salidaId: number, observaciones: string, pasajeros: PasajeroDraft[]): Observable<Reserva> {
    const lista = pasajeros.map(p => ({
      nombreCompleto: p.nombreCompleto, tipoDocumento: p.tipoDocumento, numeroDocumento: p.numeroDocumento,
      nacionalidad: p.nacionalidad, fechaNacimiento: p.fechaNacimiento ? p.fechaNacimiento : null
    }));
    const cuerpo = { clienteId: clienteId, salidaId: salidaId, observaciones: observaciones, pasajeros: lista };
    return this.http.post<Reserva>(`${API}/reservas`, cuerpo);
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
