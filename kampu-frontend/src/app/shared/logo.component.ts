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

  get marca() { return this.colorCarpa ?? (this.oscuro ? '#FDF8E6' : '#4B5B1D'); }
  get texto() { return this.oscuro ? '#FDF8E6' : '#4B5B1D'; }
}
