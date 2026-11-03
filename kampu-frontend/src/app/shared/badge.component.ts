import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-badge',
  standalone: true,
  templateUrl: './badge.component.html',
  styleUrl: './badge.component.css'
})
export class BadgeComponent {
  @Input({ required: true }) estado = '';

  private mapa: { [estado: string]: string } = {
    PENDIENTE: 'bg-amber-50 text-amber-700 ring-amber-200/70',
    PARCIAL: 'bg-sky-50 text-sky-700 ring-sky-200/70',
    PROGRAMADA: 'bg-sky-50 text-sky-700 ring-sky-200/70',
    CONFIRMADA: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    APROBADO: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    PAGADO: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    COMPLETADA: 'bg-zinc-100 text-zinc-700 ring-zinc-200',
    ANULADA: 'bg-rose-50 text-rose-700 ring-rose-200/70',
    CANCELADA: 'bg-rose-50 text-rose-700 ring-rose-200/70',
    RECHAZADO: 'bg-rose-50 text-rose-700 ring-rose-200/70',
    REEMBOLSADO: 'bg-violet-50 text-violet-700 ring-violet-200/70',
    ACTIVO: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    INACTIVO: 'bg-zinc-100 text-zinc-500 ring-zinc-200'
  };

  get clase() { return this.mapa[this.estado] ?? 'bg-zinc-100 text-zinc-700 ring-zinc-200'; }
  get texto() { return this.estado.replace('_', ' '); }
}
