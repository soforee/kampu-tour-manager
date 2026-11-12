import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconComponent } from './icon.component';

export interface Miga { texto: string; ruta?: string; }

// Migas de pan: dónde está el usuario (Reservas › R-0009). El último elemento es la página actual.
@Component({
  selector: 'app-migas',
  standalone: true,
  imports: [RouterLink, IconComponent],
  template: `
    <nav aria-label="Ruta de navegación" class="no-print mb-4">
      <ol class="flex flex-wrap items-center gap-1 text-sm">
        @for (m of items(); track $index; let ultimo = $last) {
          <li class="flex items-center gap-1 min-w-0">
            @if (m.ruta && !ultimo) {
              <a [routerLink]="m.ruta" class="rounded-md px-1.5 py-0.5 font-medium text-zinc-500 transition-colors hover:bg-white hover:text-zinc-900 hover:shadow-soft">{{ m.texto }}</a>
            } @else {
              <span class="truncate px-1.5 py-0.5 font-semibold text-zinc-800" [attr.aria-current]="ultimo ? 'page' : null">{{ m.texto }}</span>
            }
            @if (!ultimo) { <app-icon name="chevron-right" size="w-3.5 h-3.5 text-zinc-300" /> }
          </li>
        }
      </ol>
    </nav>`
})
export class MigasComponent {
  items = input.required<Miga[]>();
}
