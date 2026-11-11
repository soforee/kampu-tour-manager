import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PortalDetalle } from '../../core/models';
import { ImagenService } from '../../core/services/imagen.service';
import { PortalService } from '../../core/services/portal.service';
import { mensajeError } from '../../core/utils/api';
import { FormValidacion, V } from '../../core/utils/validacion';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';
import { PortalHeaderComponent } from './portal-header.component';

// Detalle de un paquete del cliente: GET /portal/paquetes/{id}
// (PKG_GESTION_PAQUETES_CLIENTE.detalle_paquete; exigir_propietario impide ver reservas ajenas).
// El cliente no edita nada: solo puede "informar un pago", que queda PENDIENTE hasta que Ventas lo valide.
@Component({
  selector: 'app-paquete-cliente',
  standalone: true,
  imports: [FormsModule, RouterLink, DatePipe, DecimalPipe, BadgeComponent, IconComponent, PortalHeaderComponent],
  templateUrl: './paquete-cliente.component.html'
})
export class PaqueteClienteComponent {
  private portal = inject(PortalService);
  private imagenes = inject(ImagenService);
  private id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  detalle = signal<PortalDetalle | null>(null);
  cargando = signal(true);
  error = signal('');

  metodos = ['Transferencia', 'Yape', 'Plin', 'Depósito'];
  pago = { monto: null as number | null, metodoPago: 'Transferencia', codigoTransaccion: '' };
  informando = signal(false);
  enviando = signal(false);
  errorPago = signal('');
  okPago = signal('');

  paquete = computed(() => this.detalle()?.paquete ?? null);
  // Lo que todavía se puede informar: saldo menos lo que ya está por validar
  maximo = computed(() => {
    const p = this.paquete();
    return p ? Math.max(0, Math.round((p.saldo - p.montoPorValidar) * 100) / 100) : 0;
  });
  puedeInformar = computed(() => {
    const p = this.paquete();
    return !!p && p.estadoPago !== 'ANULADO' && p.estadoEjecucion !== 'CANCELADO' && p.estadoEjecucion !== 'TERMINADO' && this.maximo() > 0;
  });
  porcentaje = computed(() => {
    const p = this.paquete();
    return p && p.montoTotal > 0 ? Math.min(100, Math.round(p.montoPagado * 100 / p.montoTotal)) : 0;
  });

  v = new FormValidacion(() => ({
    monto: V.importe(this.pago.monto, 'El monto')
      ?? ((this.pago.monto ?? 0) > this.maximo() ? `El monto no puede superar S/ ${this.maximo().toFixed(2)}.` : null),
    codigo: this.pago.codigoTransaccion.trim() ? V.codigo(this.pago.codigoTransaccion) : 'Ingresa el número de operación.'
  }));

  constructor() {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.portal.detalle(this.id).subscribe({
      next: d => { this.detalle.set(d); this.cargando.set(false); },
      error: err => { this.error.set(mensajeError(err)); this.cargando.set(false); }
    });
  }

  imagen() { return this.imagenes.url(this.paquete()?.imagen); }

  abrirPago() {
    this.pago = { monto: this.maximo(), metodoPago: 'Transferencia', codigoTransaccion: '' };
    this.v.reiniciar();
    this.errorPago.set('');
    this.informando.set(true);
  }

  informar() {
    this.errorPago.set('');
    if (!this.v.valido()) return;
    this.enviando.set(true);
    this.portal.informarPago(this.id, this.pago.monto!, this.pago.metodoPago, this.pago.codigoTransaccion.trim()).subscribe({
      next: () => {
        this.enviando.set(false);
        this.informando.set(false);
        this.okPago.set('Recibimos tu pago. Quedará como "por validar" hasta que la agencia lo confirme.');
        this.cargar();
      },
      error: err => { this.errorPago.set(mensajeError(err)); this.enviando.set(false); }
    });
  }
}
