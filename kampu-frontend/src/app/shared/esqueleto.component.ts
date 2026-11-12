import { Component, input } from '@angular/core';

// Esqueleto de carga: bloques grises animados con la forma de una lista mientras llegan los datos
@Component({
  selector: 'app-esqueleto',
  standalone: true,
  template: `
    <div class="space-y-3 px-5 py-5" role="status" aria-label="Cargando">
      @for (f of filas(); track $index) {
        <div class="flex items-center gap-4 animate-pulse">
          <div class="h-9 w-9 shrink-0 rounded-full bg-zinc-100"></div>
          <div class="flex-1 space-y-2">
            <div class="h-3 rounded bg-zinc-100" [style.width.%]="60 - ($index % 3) * 12"></div>
            <div class="h-2.5 w-1/3 rounded bg-zinc-100/80"></div>
          </div>
          <div class="hidden sm:block h-5 w-20 rounded-full bg-zinc-100"></div>
        </div>
      }
      <span class="sr-only">Cargando…</span>
    </div>`
})
export class EsqueletoComponent {
  cantidad = input(5);
  filas() { return Array.from({ length: this.cantidad() }); }
}
