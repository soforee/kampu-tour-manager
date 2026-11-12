import { AuthService } from '../../core/services/auth.service';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TourStore } from '../../core/services/tour-store.service';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';
import { RelativaPipe } from '../../shared/relativa.pipe';
import { AvatarComponent } from '../../shared/avatar.component';
import { EsqueletoComponent } from '../../shared/esqueleto.component';

@Component({
  selector: 'app-reservas',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, BadgeComponent, IconComponent, EsqueletoComponent, AvatarComponent, RelativaPipe],
  templateUrl: './reservas.component.html'
})
export class ReservasComponent {
  store = inject(TourStore);
  private route = inject(ActivatedRoute);
  gestiona = inject(AuthService).puede('GESTIONAR_RESERVAS');

  texto = signal('');
  estado = signal('');
  estados = [
    { valor: '', texto: 'Todas' }, { valor: 'PENDIENTE', texto: 'Pendientes' },
    { valor: 'CONFIRMADA', texto: 'Confirmadas' }, { valor: 'COMPLETADA', texto: 'Completadas' }, { valor: 'ANULADA', texto: 'Anuladas' }
  ];

  constructor() {
    const q = this.route.snapshot.queryParamMap.get('q');
    if (q) this.texto.set(q);
  }

  filas = computed(() => {
    const t = this.texto().toLowerCase().trim();
    const e = this.estado();
    return this.store.reservas()
      .map(r => {
        const salida = this.store.getSalida(r.salidaId);
        return {
          reserva: r, salida,
          codigo: this.store.codigoReserva(r.reservaId),
          cliente: this.store.nombreCliente(r),
          cuentaWeb: !!this.store.getCliente(r.clienteId)?.cuentaWeb,
          paquete: this.store.getPaquete(salida?.paqueteId),
          saldo: this.store.saldo(r)
        };
      })
      .filter(f => !e || f.reserva.estadoReserva === e)
      .filter(f => !t || f.codigo.toLowerCase().includes(t)
        || f.cliente.toLowerCase().includes(t)
        || (f.paquete?.nombre.toLowerCase().includes(t) ?? false))
      .sort((a, b) => b.reserva.reservaId - a.reserva.reservaId);
  });
}
