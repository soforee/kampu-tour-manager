import { Component, computed, input } from '@angular/core';

const COLORES = [
  'bg-brand-100 text-brand-800', 'bg-sky-100 text-sky-800', 'bg-violet-100 text-violet-800',
  'bg-amber-100 text-amber-800', 'bg-emerald-100 text-emerald-800', 'bg-rose-100 text-rose-800'
];

// Avatar con iniciales; el color sale del nombre (siempre el mismo para la misma persona)
@Component({
  selector: 'app-avatar',
  standalone: true,
  template: `<span class="avatar ring-2 ring-white" [class]="clase()" [style.width.px]="tam()" [style.height.px]="tam()"
                   [style.font-size.px]="tam() * 0.38" aria-hidden="true">{{ iniciales() }}</span>`
})
export class AvatarComponent {
  nombre = input('');
  tam = input(36);
  iniciales = computed(() => {
    const partes = (this.nombre() || '?').trim().split(/\s+/).filter(p => p.length > 0);
    return ((partes[0]?.[0] ?? '?') + (partes.length > 1 ? partes[1][0] : '')).toUpperCase();
  });
  clase = computed(() => {
    let h = 0;
    for (const c of this.nombre() || '') h = (h * 31 + c.charCodeAt(0)) >>> 0;
    return COLORES[h % COLORES.length];
  });
}
