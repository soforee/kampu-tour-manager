import { DatePipe } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { Operacion } from '../../core/models';
import { addDays, hoy } from '../../core/utils/date.util';
import { etiqueta } from '../../core/utils/etiquetas';
import { colorEstado, mismoDia } from '../../core/utils/trafico';
import { IconComponent } from '../../shared/icon.component';

/** Calendario semanal de operaciones: cada día con sus servicios en orden de salida */
@Component({
  selector: 'app-calendario-operaciones',
  standalone: true,
  imports: [DatePipe, IconComponent],
  template: `
  <div class="card p-4 sm:p-5">
    <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
      <div class="card-title !mb-0"><span class="ico"><app-icon name="calendar" size="w-4 h-4" /></span>
        Semana del {{ dias()[0].fecha | date:'d MMM' }} al {{ dias()[6].fecha | date:'d MMM yyyy' }}</div>
      <div class="flex items-center gap-2">
        <button type="button" class="btn-light !h-9" (click)="mover(-7)"><app-icon name="arrow-left" size="w-4 h-4" /></button>
        <button type="button" class="btn-light !h-9" (click)="inicio.set(lunes(hoy()))">Hoy</button>
        <button type="button" class="btn-light !h-9" (click)="mover(7)"><app-icon name="arrow-right" size="w-4 h-4" /></button>
      </div>
    </div>
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-7 gap-3">
      @for (d of dias(); track d.fecha) {
        <div class="min-w-0 rounded-xl border p-2.5" [class]="d.fecha === hoy() ? 'border-brand-300 bg-brand-50/40' : 'border-zinc-100 bg-sand-50/60'">
          <div class="flex items-baseline justify-between mb-2">
            <span class="text-xs font-bold uppercase tracking-wide" [class]="d.fecha === hoy() ? 'text-brand-700' : 'text-zinc-500'">{{ d.fecha + 'T00:00' | date:'EEE d' }}</span>
            <span class="text-[11px] text-zinc-400">{{ d.ops.length }} serv.</span>
          </div>
          <div class="space-y-1.5">
            @for (o of d.ops; track o.operacionId) {
              <button type="button" class="w-full text-left rounded-lg bg-white px-2 py-1.5 shadow-soft ring-1 ring-zinc-100 hover:ring-brand-300 transition"
                      [class.opacity-50]="o.estadoActual === 'CANCELADO'" (click)="elegir.emit(o)">
                <div class="flex items-center gap-1.5 text-[11px]">
                  <span class="h-2 w-2 shrink-0 rounded-full" [class]="color(o.estadoActual).bg"></span>
                  <span class="font-mono font-bold text-zinc-800">{{ o.horaProgramadaSalida | date:'HH:mm' }}</span>
                  <span class="font-mono text-zinc-400">{{ o.codigoOperacion }}</span>
                </div>
                <div class="text-xs text-zinc-700 truncate mt-0.5" [title]="o.origen + ' → ' + o.destino">{{ o.ciudadOrigen }} → {{ o.destino }}</div>
                <div class="text-[11px] truncate" [class]="o.placa ? 'text-zinc-500' : 'text-amber-600'">{{ o.placa ? o.placa + ' · ' + o.proveedor : 'Sin asignar' }}</div>
                @if (o.conflicto) { <div class="text-[11px] font-semibold text-rose-600 truncate">⚠ {{ o.conflicto }}</div> }
              </button>
            } @empty {
              <div class="py-4 text-center text-[11px] text-zinc-400">Sin servicios</div>
            }
          </div>
        </div>
      }
    </div>
    <div class="mt-4 flex flex-wrap gap-3 text-[11px] text-zinc-500">
      @for (e of leyenda; track e) { <span class="inline-flex items-center gap-1.5"><span class="h-2 w-2 rounded-full" [class]="color(e).bg"></span>{{ etiqueta(e) }}</span> }
    </div>
  </div>`
})
export class CalendarioOperacionesComponent {
  operaciones = input.required<Operacion[]>();
  elegir = output<Operacion>();

  hoy = hoy;
  etiqueta = etiqueta;
  color = colorEstado;
  leyenda = ['PROGRAMADO', 'ASIGNADO', 'EMBARQUE', 'EN_ESPERA_SALIDA', 'EN_RUTA', 'LLEGADA_PROXIMA', 'FINALIZADO', 'RETRASADO', 'CANCELADO'];
  inicio = signal(this.lunes(hoy()));

  dias = computed(() => {
    const ops = this.operaciones();
    return Array.from({ length: 7 }, (_, i) => {
      const fecha = addDays(this.inicio(), i);
      return { fecha, ops: ops.filter(o => mismoDia(o.horaProgramadaSalida, fecha)) };
    });
  });

  lunes(fecha: string): string {
    const [y, m, d] = fecha.split('-').map(Number);
    const dia = new Date(y, m - 1, d).getDay();
    return addDays(fecha, dia === 0 ? -6 : 1 - dia);
  }

  mover(dias: number) { this.inicio.set(addDays(this.inicio(), dias)); }
}
