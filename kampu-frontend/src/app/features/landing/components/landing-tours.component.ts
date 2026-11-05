import { CurrencyPipe } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { ImagenService } from '../../../core/services/imagen.service';
import { IconComponent } from '../../../shared/icon.component';
import { RevealDirective } from '../../../shared/reveal.directive';
import { Contacto, TourCard } from '../landing.models';

@Component({
  selector: 'app-landing-tours',
  standalone: true,
  imports: [CurrencyPipe, IconComponent, RevealDirective],
  templateUrl: './landing-tours.component.html',
  styleUrl: './landing-tours.component.css'
})
export class LandingToursComponent {
  @Input() tours: TourCard[] = [];
  @Input() cargando = false;
  @Input() hayFiltro = false;
  @Input({ required: true }) contacto!: Contacto;
  @Output() limpiar = new EventEmitter<void>();

  imagenes = inject(ImagenService);

  private meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

  // '2026-12-12' → '12 dic 2026'
  fecha(iso: string): string {
    const [y, m, d] = iso.split('-').map(Number);
    return `${d} ${this.meses[m - 1]} ${y}`;
  }

  cotizar(t: TourCard): string {
    const asunto = encodeURIComponent('Cotización: ' + t.paquete.nombre);
    return `mailto:${this.contacto.email}?subject=${asunto}`;
  }
}
