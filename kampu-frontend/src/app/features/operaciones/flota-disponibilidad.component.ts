import { DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { Operacion, Proveedor, Vehiculo } from '../../core/models';
import { hoy } from '../../core/utils/date.util';
import { etiqueta } from '../../core/utils/etiquetas';
import { colorEstado, ocupaAhora } from '../../core/utils/trafico';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';

/**
 * Disponibilidad de la flota en un día: por empresa, cada vehículo con su línea de tiempo
 * (barras = servicios, coloreadas por estado) y su situación en este momento.
 */
@Component({
  selector: 'app-flota-disponibilidad',
  standalone: true,
  imports: [DatePipe, BadgeComponent, IconComponent],
  template: `
  <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3 mb-5">
    @for (p of proveedores(); track p.proveedorId) {
      <div class="card p-4" [class.opacity-60]="p.estado !== 'ACTIVO'">
        <div class="flex items-start justify-between gap-2">
          <div class="min-w-0">
            <div class="font-semibold text-zinc-900 truncate">{{ p.nombreComercial }}</div>
            <div class="text-xs text-zinc-500 truncate">{{ p.esPropio ? 'Flota propia' : 'RUC ' + p.ruc }} · {{ p.zonas ? p.zonas.split(',').join(', ') : 'Todo el país' }}</div>
          </div>
          <app-badge [estado]="p.estado" />
        </div>
        <div class="mt-3 grid grid-cols-4 gap-2 text-center">
          <div><div class="text-lg font-extrabold text-zinc-900">{{ p.vehiculos }}</div><div class="text-[10px] text-zinc-500">Unidades</div></div>
          <div><div class="text-lg font-extrabold text-emerald-600">{{ p.vehiculosDisponibles }}</div><div class="text-[10px] text-zinc-500">Libres ahora</div></div>
          <div><div class="text-lg font-extrabold text-zinc-900">{{ p.serviciosHoy }}</div><div class="text-[10px] text-zinc-500">Servicios hoy</div></div>
          <div><div class="text-lg font-extrabold" [class]="p.serviciosPorConfirmar ? 'text-amber-600' : 'text-zinc-900'">{{ p.serviciosPorConfirmar }}</div><div class="text-[10px] text-zinc-500">Por confirmar</div></div>
        </div>
        <div class="mt-2 flex flex-wrap gap-1">
          @for (s of p.servicios; track s) { <span class="rounded-md bg-sand-100 px-1.5 py-0.5 text-[10px] font-semibold text-zinc-600">{{ etiqueta(s) }}</span> }
          <span class="rounded-md bg-sand-100 px-1.5 py-0.5 text-[10px] font-semibold text-zinc-600">{{ p.capacidadOperativa }} asientos</span>
        </div>
      </div>
    }
  </div>

  <div class="card p-4 sm:p-5">
    <div class="card-title"><span class="ico"><app-icon name="truck" size="w-4 h-4" /></span> Ocupación de la flota · {{ fecha() + 'T00:00' | date:'EEEE d MMMM' }}</div>
    <div class="overflow-x-auto">
      <div class="min-w-[760px]">
        <div class="grid grid-cols-[200px_1fr] text-[10px] text-zinc-400 mb-1">
          <span></span>
          <div class="relative h-4">
            @for (h of horas; track h) { <span class="absolute -translate-x-1/2" [style.left.%]="h / 24 * 100">{{ h }}h</span> }
          </div>
        </div>
        @for (g of grupos(); track g.proveedor.proveedorId) {
          <div class="mt-3 mb-1 text-[11px] font-bold uppercase tracking-wide text-zinc-500">{{ g.proveedor.nombreComercial }}</div>
          @for (f of g.filas; track f.vehiculo.vehiculoId) {
            <div class="grid grid-cols-[200px_1fr] items-center border-t border-zinc-100 py-1.5">
              <div class="pr-3 min-w-0">
                <div class="flex items-center gap-1.5">
                  <span class="h-2 w-2 rounded-full" [class]="f.situacion.punto"></span>
                  <span class="font-mono text-xs font-bold text-zinc-800">{{ f.vehiculo.placa }}</span>
                  <span class="text-[11px] text-zinc-400">{{ etiqueta(f.vehiculo.tipoVehiculo) }} · {{ f.vehiculo.capacidadPax }}</span>
                </div>
                <div class="text-[11px]" [class]="f.situacion.texto">{{ f.situacion.nombre }}</div>
              </div>
              <div class="relative h-8 rounded-lg bg-sand-50 ring-1 ring-zinc-100 overflow-hidden"
                   [class.bg-stripes]="f.vehiculo.estado !== 'OPERATIVO'">
                @if (esHoy()) { <div class="absolute inset-y-0 w-px bg-rose-400 z-10" [style.left.%]="ahoraPct()"></div> }
                @for (b of f.barras; track b.op.operacionId) {
                  <button type="button" class="absolute top-1 bottom-1 rounded-md px-1.5 text-left text-[10px] font-semibold text-white truncate shadow-sm hover:brightness-110"
                          [class]="color(b.op.estadoActual).bg" [style.left.%]="b.izq" [style.width.%]="b.ancho"
                          [title]="b.op.codigoOperacion + ' · ' + b.op.origen + ' → ' + b.op.destino + ' · ' + etiqueta(b.op.estadoActual)"
                          (click)="elegir.emit(b.op)">{{ b.op.codigoOperacion }}</button>
                }
              </div>
            </div>
          }
        } @empty {
          <div class="empty"><span class="ico"><app-icon name="truck" size="w-6 h-6" /></span>No hay vehículos registrados.</div>
        }
      </div>
    </div>
  </div>`,
  styles: [`.bg-stripes { background-image: repeating-linear-gradient(45deg, rgb(244 244 245) 0 6px, rgb(250 250 250) 6px 12px); }`]
})
export class FlotaDisponibilidadComponent {
  fecha = input.required<string>();
  vehiculos = input.required<Vehiculo[]>();
  proveedores = input.required<Proveedor[]>();
  operaciones = input.required<Operacion[]>();
  elegir = output<Operacion>();

  etiqueta = etiqueta;
  color = colorEstado;
  horas = [0, 3, 6, 9, 12, 15, 18, 21, 24];

  esHoy = computed(() => this.fecha() === hoy());
  ahoraPct() { const d = new Date(); return (d.getHours() * 60 + d.getMinutes()) / 1440 * 100; }

  grupos = computed(() => {
    const inicio = new Date(this.fecha() + 'T00:00').getTime();
    const fin = inicio + 86400000;
    const ahora = new Date();
    const ops = this.operaciones().filter(o => o.vehiculoId && o.estadoActual !== 'CANCELADO');
    return this.proveedores().map(p => ({
      proveedor: p,
      filas: this.vehiculos().filter(v => v.proveedorId === p.proveedorId).map(v => {
        const suyas = ops.filter(o => o.vehiculoId === v.vehiculoId);
        const barras = suyas
          .map(o => ({ op: o, a: new Date(o.horaProgramadaSalida).getTime(), b: new Date(o.horaEstimadaLlegada).getTime() }))
          .filter(x => x.b > inicio && x.a < fin)
          .map(x => {
            const a = Math.max(x.a, inicio), b = Math.min(x.b, fin);
            return { op: x.op, izq: (a - inicio) / 864000, ancho: Math.max((b - a) / 864000, 1.2) };
          });
        return { vehiculo: v, barras, situacion: this.situacion(v, suyas.find(o => ocupaAhora(o, ahora)) ?? null) };
      })
    })).filter(g => g.filas.length > 0);
  });

  private situacion(v: Vehiculo, actual: Operacion | null) {
    if (v.estado === 'MANTENIMIENTO') return { nombre: 'En mantenimiento', punto: 'bg-amber-500', texto: 'text-amber-600' };
    if (v.estado === 'INACTIVO') return { nombre: 'Inactivo', punto: 'bg-zinc-300', texto: 'text-zinc-400' };
    if (actual) return { nombre: `En servicio · ${actual.codigoOperacion}`, punto: 'bg-violet-500', texto: 'text-violet-600' };
    return { nombre: 'Libre ahora', punto: 'bg-emerald-500', texto: 'text-emerald-600' };
  }
}
