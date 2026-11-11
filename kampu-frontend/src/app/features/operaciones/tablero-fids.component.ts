import { DatePipe } from '@angular/common';
import { Component, computed, ElementRef, inject, input, OnDestroy, signal } from '@angular/core';
import { Operacion } from '../../core/models';
import { etiqueta } from '../../core/utils/etiquetas';
import { colorEstado, EN_TRANSITO, POR_SALIR, salidaEfectiva } from '../../core/utils/trafico';
import { IconComponent } from '../../shared/icon.component';

interface Contadores { transito: number; retrasados: number; disponibles: number; ocupados: number; empresas: number; }

/**
 * Tablero de salidas y llegadas al estilo de un aeropuerto (FIDS): fondo oscuro, reloj en vivo,
 * próximas salidas y próximas llegadas. El componente padre recarga las operaciones cada 30 s.
 */
@Component({
  selector: 'app-tablero-fids',
  standalone: true,
  imports: [DatePipe, IconComponent],
  template: `
  <div class="rounded-2xl bg-zinc-950 text-zinc-100 p-4 sm:p-6 shadow-lifted ring-1 ring-white/5 [&:fullscreen]:rounded-none [&:fullscreen]:overflow-auto">
    <div class="flex flex-wrap items-center justify-between gap-3 mb-5">
      <div class="flex items-center gap-3">
        <span class="grid h-10 w-10 place-items-center rounded-xl bg-amber-400/10 text-amber-300"><app-icon name="truck" size="w-5 h-5" /></span>
        <div>
          <div class="text-[11px] font-extrabold uppercase tracking-[0.2em] text-amber-300">Kampu · Centro de operaciones</div>
          <div class="text-lg font-bold">Salidas y llegadas</div>
        </div>
      </div>
      <div class="flex items-center gap-4">
        <div class="text-right">
          <div class="font-mono text-3xl font-bold tabular-nums text-amber-300">{{ ahora() | date:'HH:mm:ss' }}</div>
          <div class="text-xs text-zinc-400 capitalize">{{ ahora() | date:'EEEE d MMMM' }}</div>
        </div>
        <button type="button" class="grid h-9 w-9 place-items-center rounded-lg border border-white/10 text-zinc-300 hover:bg-white/10" title="Pantalla completa" (click)="pantallaCompleta()">
          <app-icon name="eye" size="w-4 h-4" />
        </button>
      </div>
    </div>

    <div class="grid grid-cols-2 sm:grid-cols-5 gap-2 mb-5">
      @for (k of indicadores(); track k.texto) {
        <div class="rounded-xl bg-white/[0.04] px-3 py-2 ring-1 ring-white/5">
          <div class="text-[10px] uppercase tracking-wider text-zinc-400">{{ k.texto }}</div>
          <div class="font-mono text-2xl font-bold" [class]="k.clase">{{ k.valor }}</div>
        </div>
      }
    </div>

    <div class="grid grid-cols-1 xl:grid-cols-2 gap-5">
      @for (panel of paneles(); track panel.titulo) {
        <section class="min-w-0">
          <div class="flex items-center gap-2 mb-2 text-sm font-bold uppercase tracking-[0.18em]" [class]="panel.clase">
            <app-icon [name]="panel.icono" size="w-4 h-4" /> {{ panel.titulo }}
          </div>
          <div class="overflow-x-auto rounded-xl ring-1 ring-white/10">
            <table class="w-full text-sm">
              <thead class="bg-white/[0.06] text-[10px] uppercase tracking-wider text-zinc-400">
                <tr><th class="px-3 py-2 text-left">Hora</th><th class="px-3 py-2 text-left">Código</th><th class="px-3 py-2 text-left">{{ panel.columna }}</th>
                  <th class="px-3 py-2 text-left">Empresa · Unidad</th><th class="px-3 py-2 text-left">Estado</th></tr>
              </thead>
              <tbody>
                @for (o of panel.filas; track o.operacionId) {
                  <tr class="border-t border-white/5 font-mono">
                    <td class="px-3 py-2 whitespace-nowrap">
                      @if (panel.salidas) {
                        <span [class.line-through]="o.horaSalidaEstimada" [class.text-zinc-500]="o.horaSalidaEstimada">{{ o.horaProgramadaSalida | date:'HH:mm' }}</span>
                        @if (o.horaSalidaEstimada) { <span class="ml-1 text-orange-300">{{ o.horaSalidaEstimada | date:'HH:mm' }}</span> }
                      } @else { {{ o.horaEstimadaLlegada | date:'HH:mm' }} }
                      @if (!esHoy(panel.salidas ? o.horaProgramadaSalida : o.horaEstimadaLlegada)) {
                        <div class="text-[10px] text-zinc-500">{{ (panel.salidas ? o.horaProgramadaSalida : o.horaEstimadaLlegada) | date:'dd/MM' }}</div>
                      }
                    </td>
                    <td class="px-3 py-2 font-bold text-amber-200 whitespace-nowrap">{{ o.codigoOperacion }}</td>
                    <td class="px-3 py-2 font-sans">
                      <div class="font-semibold text-zinc-100 truncate max-w-[170px]">{{ panel.salidas ? o.destino : o.origen }}</div>
                      <div class="text-[11px] text-zinc-500 truncate max-w-[170px]">{{ panel.salidas ? 'desde ' + o.origen : 'hacia ' + o.destino }}</div>
                    </td>
                    <td class="px-3 py-2 font-sans">
                      <div class="text-zinc-200 truncate max-w-[130px]">{{ o.proveedor ?? '—' }}</div>
                      <div class="text-[11px] text-zinc-500 font-mono whitespace-nowrap">{{ o.placa ?? 'sin unidad' }}</div>
                    </td>
                    <td class="px-3 py-2 whitespace-nowrap">
                      <span class="inline-flex items-center gap-1.5 text-xs font-bold uppercase" [class]="color(o.estadoActual).fids">
                        <span class="h-2 w-2 rounded-full" [class]="color(o.estadoActual).bg" [class.animate-pulse]="parpadea(o)"></span>
                        {{ etiqueta(o.estadoActual) }}
                      </span>
                      @if (o.minRetrasoEstimado) { <div class="text-[11px] text-orange-300">+{{ o.minRetrasoEstimado }} min</div> }
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="5" class="px-3 py-8 text-center text-zinc-500 font-sans">{{ panel.vacio }}</td></tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }
    </div>
    <p class="mt-4 text-[11px] text-zinc-500">Se actualiza automáticamente cada 30 segundos.</p>
  </div>`
})
export class TableroFidsComponent implements OnDestroy {
  operaciones = input.required<Operacion[]>();
  contadores = input.required<Contadores>();

  private el = inject(ElementRef);
  ahora = signal(new Date());
  private reloj = setInterval(() => this.ahora.set(new Date()), 1000);
  etiqueta = etiqueta;
  color = colorEstado;

  // Próximas 12 salidas (aún no parten) y próximas 12 llegadas (en tránsito o recién llegadas)
  paneles = computed(() => {
    const ops = this.operaciones();
    const ahora = this.ahora().getTime();
    const hace = ahora - 45 * 60000;
    const salidas = ops
      .filter(o => POR_SALIR.includes(o.estadoActual) || (o.estadoActual === 'CANCELADO' && new Date(o.horaProgramadaSalida).getTime() > hace))
      .filter(o => salidaEfectiva(o).getTime() > hace - 6 * 3600000)
      .sort((a, b) => salidaEfectiva(a).getTime() - salidaEfectiva(b).getTime())
      .slice(0, 12);
    const llegadas = ops
      .filter(o => EN_TRANSITO.includes(o.estadoActual)
        || (o.estadoActual === 'FINALIZADO' && o.horaRealLlegada !== null && new Date(o.horaRealLlegada).getTime() > hace))
      .sort((a, b) => new Date(a.horaEstimadaLlegada).getTime() - new Date(b.horaEstimadaLlegada).getTime())
      .slice(0, 12);
    return [
      { titulo: 'Salidas', icono: 'arrow-right', clase: 'text-amber-300', columna: 'Destino', salidas: true, filas: salidas, vacio: 'No hay salidas próximas.' },
      { titulo: 'Llegadas', icono: 'flag', clase: 'text-teal-300', columna: 'Procedencia', salidas: false, filas: llegadas, vacio: 'No hay unidades en tránsito.' }
    ];
  });

  indicadores = computed(() => {
    const c = this.contadores();
    return [
      { texto: 'En tránsito', valor: c.transito, clase: 'text-emerald-300' },
      { texto: 'Retrasados', valor: c.retrasados, clase: c.retrasados ? 'text-orange-300' : 'text-zinc-200' },
      { texto: 'Unidades libres', valor: c.disponibles, clase: 'text-sky-300' },
      { texto: 'Unidades ocupadas', valor: c.ocupados, clase: 'text-violet-300' },
      { texto: 'Empresas activas', valor: c.empresas, clase: 'text-amber-200' }
    ];
  });

  esHoy(iso: string) { return new Date(iso).toDateString() === this.ahora().toDateString(); }
  parpadea(o: Operacion) { return ['EMBARQUE', 'EN_ESPERA_SALIDA', 'LLEGADA_PROXIMA', 'RETRASADO'].includes(o.estadoActual); }

  pantallaCompleta() {
    const div = this.el.nativeElement.firstElementChild as HTMLElement;
    if (document.fullscreenElement) document.exitFullscreen();
    else div?.requestFullscreen?.();
  }

  ngOnDestroy() { clearInterval(this.reloj); }
}

