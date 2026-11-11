import { AuthService } from '../../core/services/auth.service';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TourStore } from '../../core/services/tour-store.service';
import { AlertService } from '../../core/services/alert.service';
import { aPEN } from '../../core/utils/moneda';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';

@Component({
  selector: 'app-pagos',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, BadgeComponent, IconComponent],
  templateUrl: './pagos.component.html'
})
export class PagosComponent {
  private store = inject(TourStore);
  private alerts = inject(AlertService);
  // REGISTRAR_PAGO: registrar y validar pagos (Admin y Ventas)
  gestiona = inject(AuthService).puede('REGISTRAR_PAGO');

  texto = signal('');
  // ?estado=PENDIENTE: llega desde el aviso "pagos por validar" del panel
  estado = signal(inject(ActivatedRoute).snapshot.queryParamMap.get('estado') ?? '');
  estados = [{ valor: '', texto: 'Todos' }, { valor: 'APROBADO', texto: 'Aprobados' }, { valor: 'PENDIENTE', texto: 'Pendientes' },
             { valor: 'RECHAZADO', texto: 'Rechazados' }, { valor: 'REEMBOLSADO', texto: 'Reembolsados' }];

  filas = computed(() => {
    const t = this.texto().toLowerCase().trim();
    const e = this.estado();
    return this.store.pagos()
      .map(p => ({
        pago: p,
        codigo: this.store.codigoReserva(p.reservaId),
        cliente: this.store.nombreCliente(this.store.getReserva(p.reservaId)) || '—',
        pen: p.montoPen ?? aPEN(p.monto, p.moneda)   // PAGOS.monto_pen (columna virtual)
      }))
      .filter(f => !e || f.pago.estadoTransaccion === e)
      .filter(f => !t || f.codigo.toLowerCase().includes(t) || f.cliente.toLowerCase().includes(t)
        || f.pago.codigoTransaccion.toLowerCase().includes(t))
      .sort((a, b) => b.pago.fechaPago.localeCompare(a.pago.fechaPago));
  });

  totalAprobado = computed(() =>
    this.filas().filter(f => f.pago.estadoTransaccion === 'APROBADO').reduce((a, f) => a + f.pen, 0));

  resolver(id: number, estado: 'APROBADO' | 'RECHAZADO') {
    if (this.store.ocupado()) return;
    this.store.ocupado.set(true);
    this.store.resolverPago(id, estado).subscribe({
      next: () => {
        this.store.ocupado.set(false);
        this.store.cargarPagos();
        this.store.cargarReservas();
        this.alerts.toast(estado === 'APROBADO' ? 'Pago aprobado' : 'Pago rechazado');
      },
      error: (err) => {
        this.store.ocupado.set(false);
        this.alerts.error(this.store.error(err));
      }
    });
  }
}
