import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-logo',
  standalone: true,
  templateUrl: './logo.component.html'
})
export class LogoComponent {
  @Input() variante: 'isotipo' | 'horizontal' | 'vertical' = 'horizontal';
  @Input() oscuro = false;
  @Input() alto = 32;
  @Input() colorCarpa?: string;

  // Sin "oscuro" explícito el color sigue al tema (variables de styles.css): verde en claro, verde claro en oscuro
  get marca() { return this.colorCarpa ?? (this.oscuro ? '#FDF8E6' : 'rgb(var(--c-brand-600))'); }
  get texto() { return this.oscuro ? '#FDF8E6' : 'rgb(var(--c-brand-600))'; }
}
