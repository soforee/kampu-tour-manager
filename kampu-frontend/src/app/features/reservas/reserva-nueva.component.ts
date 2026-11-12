import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, signal, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TourStore } from '../../core/services/tour-store.service';
import { AlertService } from '../../core/services/alert.service';
import { Cliente, ContactoReserva, PasajeroDraft, TipoDocumento } from '../../core/models';
import { hoy } from '../../core/utils/date.util';
import { IconComponent } from '../../shared/icon.component';
import { MigasComponent } from '../../shared/migas.component';
import { filtrarEscritura, filtrarPegado } from '../../core/utils/filtro';
import { Errores, FormValidacion, V } from '../../core/utils/validacion';
import { ClienteSelectorComponent } from './cliente-selector.component';
import { TelefonoInputComponent } from '../../shared/telefono-input.component';
import { errorTelefonoPais } from '../../core/utils/paises';

@Component({
  selector: 'app-reserva-nueva',
  standalone: true,
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, IconComponent, ClienteSelectorComponent, TelefonoInputComponent, MigasComponent],
  templateUrl: './reserva-nueva.component.html'
})
export class ReservaNuevaComponent implements OnInit {
  store = inject(TourStore);
  private alerts = inject(AlertService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  tipos: TipoDocumento[] = ['DNI', 'CE', 'PASAPORTE'];
  // 'registrado': cliente existente (buscador) · 'invitado': cliente nuevo (se crea al guardar la reserva)
  modo: 'registrado' | 'invitado' = 'registrado';
  cliente: Cliente | null = null;
  contacto: ContactoReserva = { nombre: '', tipoDocumento: 'DNI', numeroDocumento: '', email: '', telefono: '' };
  salidaId: number | null = null;
  observaciones = '';
  pasajeros: PasajeroDraft[] = [this.vacio()];
  v = new FormValidacion(() => {
    const e: Errores = {
      cliente: this.modo === 'registrado' && !this.cliente ? 'Busca y selecciona un cliente.' : null,
      cNombre: this.modo === 'invitado' ? V.nombrePersona(this.contacto.nombre, 'El nombre del cliente') : null,
      cDocumento: this.modo === 'invitado'
        ? V.documento(this.contacto.tipoDocumento, this.contacto.numeroDocumento)
          ?? (this.store.clientes().some(c => c.numeroDocumento.toUpperCase() === this.contacto.numeroDocumento.trim().toUpperCase())
            ? 'Ya existe un cliente con ese documento: búscalo como registrado.' : null)
        : null,
      cEmail: this.modo === 'invitado'
        ? (this.contacto.email.trim() ? V.email(this.contacto.email) : 'El correo es obligatorio para enviarle la boleta.') : null,
      cTelefono: this.modo === 'invitado' ? errorTelefonoPais(this.contacto.telefono) : null,
      salida: this.salidaId ? null : 'Selecciona una salida.',
      observaciones: this.observaciones.length > 200 ? 'Las observaciones admiten máximo 200 caracteres.' : null
    };
    const docs = new Set<string>();
    this.pasajeros.forEach((p, i) => {
      const doc = p.numeroDocumento.trim().toUpperCase();
      e['p' + i + 'nombre'] = V.nombrePersona(p.nombreCompleto, 'El nombre del pasajero');
      e['p' + i + 'documento'] = V.documento(p.tipoDocumento, p.numeroDocumento)
        ?? (docs.has(doc) ? 'Este documento ya está en otro pasajero de la reserva.' : null)
        ?? (this.yaViaja(p.tipoDocumento, doc) ? 'Este pasajero ya tiene una reserva activa en esta salida.' : null);
      if (doc) docs.add(doc);
      e['p' + i + 'nacionalidad'] = V.nacionalidad(p.nacionalidad);
      e['p' + i + 'nacimiento'] = V.fecha(p.fechaNacimiento, 'La fecha de nacimiento', false)
        ?? (p.fechaNacimiento && p.fechaNacimiento > this.ayer ? 'La fecha de nacimiento debe ser anterior a hoy.' : null)
        ?? (p.fechaNacimiento && p.fechaNacimiento < '1900-01-01' ? 'La fecha de nacimiento no puede ser anterior a 1900.' : null);
    });
    return e;
  });

  ayer = (() => { const d = new Date(); d.setDate(d.getDate() - 1); return d.toISOString().slice(0, 10); })();

  private vacio(): PasajeroDraft {
    return { nombreCompleto: '', tipoDocumento: 'DNI', numeroDocumento: '', nacionalidad: 'Peruana', fechaNacimiento: '' };
  }

  get salidasDisponibles() {
    const t = hoy();
    return this.store.salidas()
      .filter(s => (s.estado === 'PROGRAMADA' || s.estado === 'CONFIRMADA') && s.fechaInicio >= t && s.cuposDisponibles > 0)
      .sort((a, b) => a.fechaInicio.localeCompare(b.fechaInicio));
  }
  get salida() { return this.store.getSalida(this.salidaId); }

  // El mismo DNI/CE/pasaporte no puede ir dos veces en la misma salida
  private yaViaja(tipo: string, doc: string): boolean {
    if (!this.salidaId || !doc) return false;
    const reservas = new Set(this.store.reservas()
      .filter(r => r.salidaId === this.salidaId && r.estadoReserva !== 'ANULADA').map(r => r.reservaId));
    return this.store.pasajeros().some(x => reservas.has(x.reservaId) && x.tipoDocumento === tipo
      && x.numeroDocumento.toUpperCase() === doc);
  }
  get paquete() { return this.store.getPaquete(this.salida?.paqueteId); }
  get total() { return (this.paquete?.precioBase ?? 0) * this.pasajeros.length; }
  get excede() { return !!this.salida && this.pasajeros.length > this.salida.cuposDisponibles; }
  get tieneCliente() { return this.modo === 'registrado' ? !!this.cliente : !!this.contacto.nombre.trim(); }

  ngOnInit() {
    this.store.cargar(false, () => {
      const q = this.route.snapshot.queryParamMap;
      const idSalida = Number(q.get('salida'));
      const idPaquete = Number(q.get('paquete'));
      if (idSalida) {
        this.salidaId = idSalida;
      } else if (idPaquete) {
        const s = this.salidasDisponibles.find(x => x.paqueteId === idPaquete);
        this.salidaId = s ? s.salidaId : null;
      }
    });
  }

  agregar() { if (this.pasajeros.length < 99) this.pasajeros.push(this.vacio()); }
  quitar(i: number) { this.pasajeros.splice(i, 1); }

  cambiarModo(modo: 'registrado' | 'invitado') {
    this.modo = modo;
    this.v.reiniciar();
  }

  // Al elegir un cliente en el buscador se autocompleta el primer pasajero (si está vacío)
  elegirCliente(c: Cliente | null) {
    this.cliente = c;
    this.v.tocar('cliente');
    if (c && this.primeroVacio()) this.agregarCliente();
  }

  // Desde el buscador: "no está registrado" → pasa a invitado con lo que escribió como nombre
  pasarAInvitado(texto: string) {
    this.modo = 'invitado';
    const t = texto.trim();
    if (t.includes('@')) this.contacto.email = t;
    else if (/^[a-zA-ZÀ-ÿ .'-]+$/.test(t)) this.contacto.nombre = t;
    this.v.reiniciar();
  }

  private primeroVacio(): boolean {
    const p = this.pasajeros[0];
    return this.pasajeros.length === 1 && !p.nombreCompleto.trim() && !p.numeroDocumento.trim();
  }

  agregarCliente() {
    let dato: PasajeroDraft;
    if (this.modo === 'registrado') {
      const c = this.cliente;
      if (!c) return;
      // Un cliente con RUC es una empresa: no viaja como pasajero
      if (c.tipoDocumento === 'RUC') { this.alerts.toast('El cliente es una empresa (RUC): agrega a los pasajeros uno por uno', 'info'); return; }
      dato = { nombreCompleto: c.nombreCompleto, tipoDocumento: c.tipoDocumento, numeroDocumento: c.numeroDocumento,
               nacionalidad: 'Peruana', fechaNacimiento: '' };
    } else {
      if (!this.contacto.nombre.trim()) return;
      dato = { nombreCompleto: this.contacto.nombre.trim(), tipoDocumento: this.contacto.tipoDocumento,
               numeroDocumento: this.contacto.numeroDocumento.trim(), nacionalidad: 'Peruana', fechaNacimiento: '' };
    }
    if (this.primeroVacio()) this.pasajeros[0] = dato;
    else this.pasajeros.push(dato);
  }

  // ---------- Asistente: 1 Cliente · 2 Salida · 3 Pasajeros · 4 Revisar ----------
  pasos = ['Cliente', 'Salida', 'Pasajeros', 'Revisar'];
  paso = signal(1);
  private camposPaso(n: number): string[] {
    if (n === 1) return ['cliente', 'cNombre', 'cDocumento', 'cEmail', 'cTelefono'];
    if (n === 2) return ['salida', 'observaciones'];
    if (n === 3) return this.pasajeros.flatMap((_, i) => ['nombre', 'documento', 'nacionalidad', 'nacimiento'].map(c => 'p' + i + c));
    return [];
  }
  private pasoValido(n: number): boolean {
    return this.v.validos(this.camposPaso(n)) && !(n === 3 && this.excede);
  }
  siguiente() {
    if (!this.pasoValido(this.paso())) { this.alerts.toast('Revisa los campos marcados en rojo', 'error'); return; }
    this.paso.set(Math.min(this.paso() + 1, 4));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  anterior() { this.paso.set(Math.max(this.paso() - 1, 1)); }
  // Se puede volver a cualquier paso; avanzar solo si los anteriores están completos
  irA(n: number) {
    for (let i = 1; i < n; i++) {
      if (!this.pasoValido(i)) { this.paso.set(i); this.alerts.toast('Completa este paso primero', 'error'); return; }
    }
    this.paso.set(n);
  }
  nombreCliente(): string {
    return this.modo === 'registrado' ? (this.cliente?.nombreCompleto ?? '') : this.contacto.nombre.trim();
  }

  guardar() {
    if (this.store.ocupado()) return;
    if (!this.v.valido() || this.excede) { this.alerts.toast('Revisa los campos marcados en rojo', 'error'); return; }
    const lista: PasajeroDraft[] = this.pasajeros.map(p => ({
      nombreCompleto: p.nombreCompleto.trim(), tipoDocumento: p.tipoDocumento, numeroDocumento: p.numeroDocumento.trim(),
      nacionalidad: p.nacionalidad.trim(), fechaNacimiento: p.fechaNacimiento
    }));
    this.store.ocupado.set(true);
    const clienteId = this.modo === 'registrado' ? this.cliente!.clienteId : null;
    const contacto: ContactoReserva | null = this.modo === 'invitado'
      ? { nombre: this.contacto.nombre.trim(), tipoDocumento: this.contacto.tipoDocumento, numeroDocumento: this.contacto.numeroDocumento.trim(),
          email: this.contacto.email.trim().toLowerCase(), telefono: this.contacto.telefono.trim() }
      : null;
    this.store.crearReserva(clienteId, contacto, this.salidaId!, this.observaciones.trim(), lista).subscribe({
      next: (reserva) => {
        this.store.ocupado.set(false);
        this.alerts.toast('Reserva creada');
        this.store.cargar(true, () => this.router.navigate(['/reservas', reserva.reservaId]));
      },
      error: (err) => {
        this.store.ocupado.set(false);
        this.alerts.error(this.store.error(err));
      }
    });
  }

  filtrar(evento: Event, tipo: string) { filtrarEscritura(evento, tipo); }
  pegar(evento: Event, tipo: string) { filtrarPegado(evento, tipo); }
}
