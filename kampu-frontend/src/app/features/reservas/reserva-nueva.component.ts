import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TourStore } from '../../core/services/tour-store.service';
import { AlertService } from '../../core/services/alert.service';
import { PasajeroDraft, TipoDocumento } from '../../core/models';
import { hoy } from '../../core/utils/date.util';
import { IconComponent } from '../../shared/icon.component';
import { filtrarEscritura, filtrarPegado } from '../../core/utils/filtro';
import { Errores, FormValidacion, V } from '../../core/utils/validacion';

@Component({
  selector: 'app-reserva-nueva',
  standalone: true,
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, IconComponent],
  templateUrl: './reserva-nueva.component.html',
  styleUrl: './reserva-nueva.component.css'
})
export class ReservaNuevaComponent implements OnInit {
  store = inject(TourStore);
  private alerts = inject(AlertService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  tipos: TipoDocumento[] = ['DNI', 'CE', 'PASAPORTE'];
  clienteId: number | null = null;
  salidaId: number | null = null;
  observaciones = '';
  pasajeros: PasajeroDraft[] = [this.vacio()];
  v = new FormValidacion(() => {
    const e: Errores = {
      cliente: this.clienteId ? null : 'Selecciona un cliente.',
      salida: this.salidaId ? null : 'Selecciona una salida.',
      observaciones: this.observaciones.length > 200 ? 'Las observaciones admiten máximo 200 caracteres.' : null
    };
    const docs = new Set<string>();
    this.pasajeros.forEach((p, i) => {
      const doc = p.numeroDocumento.trim().toUpperCase();
      e['p' + i + 'nombre'] = V.nombrePersona(p.nombreCompleto, 'El nombre del pasajero');
      e['p' + i + 'documento'] = V.documento(p.tipoDocumento, p.numeroDocumento)
        ?? (docs.has(doc) ? 'Este documento ya está en otro pasajero de la reserva.' : null);
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
  get paquete() { return this.store.getPaquete(this.salida?.paqueteId); }
  get total() { return (this.paquete?.precioBase ?? 0) * this.pasajeros.length; }
  get excede() { return !!this.salida && this.pasajeros.length > this.salida.cuposDisponibles; }
  get puedeGuardar() { return !this.store.ocupado() && !!this.clienteId && !!this.salida && !this.excede && this.pasajeros.length > 0; }

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

  agregarCliente() {
    const c = this.store.getCliente(this.clienteId);
    if (!c) return;
    const dato: PasajeroDraft = {
      nombreCompleto: c.nombreCompleto, tipoDocumento: c.tipoDocumento, numeroDocumento: c.numeroDocumento,
      nacionalidad: 'Peruana', fechaNacimiento: ''
    };
    const primero = this.pasajeros[0];
    if (this.pasajeros.length === 1 && !primero.nombreCompleto && !primero.numeroDocumento) this.pasajeros[0] = dato;
    else this.pasajeros.push(dato);
  }

  guardar() {
    if (this.store.ocupado()) return;
    if (!this.v.valido() || this.excede) { this.alerts.toast('Revisa los campos marcados en rojo', 'error'); return; }
    const lista: PasajeroDraft[] = this.pasajeros.map(p => ({
      nombreCompleto: p.nombreCompleto.trim(), tipoDocumento: p.tipoDocumento, numeroDocumento: p.numeroDocumento.trim(),
      nacionalidad: p.nacionalidad.trim(), fechaNacimiento: p.fechaNacimiento
    }));
    this.store.ocupado.set(true);
    this.store.crearReserva(this.clienteId!, this.salidaId!, this.observaciones.trim(), lista).subscribe({
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
