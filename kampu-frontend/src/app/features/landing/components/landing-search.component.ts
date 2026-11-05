import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { hoy } from '../../../core/utils/date.util';
import { IconComponent } from '../../../shared/icon.component';
import { FiltroBusqueda, TipoTour } from '../landing.models';

@Component({
  selector: 'app-landing-search',
  standalone: true,
  imports: [FormsModule, IconComponent],
  templateUrl: './landing-search.component.html',
  styleUrl: './landing-search.component.css'
})
export class LandingSearchComponent {
  @Input() destinos: string[] = [];
  @Output() buscar = new EventEmitter<FiltroBusqueda>();

  destino = '';
  fecha = '';
  tipo: TipoTour = '';
  hoy = hoy();

  tipos: { valor: TipoTour; texto: string }[] = [
    { valor: '', texto: 'Todos los tours' },
    { valor: 'FULL_DAY', texto: 'Full day (1 día)' },
    { valor: 'ESCAPADA', texto: 'Escapadas (2 a 3 días)' },
    { valor: 'EXPEDICION', texto: 'Expediciones (4 días o más)' }
  ];

  enviar() {
    this.buscar.emit({ destino: this.destino, fecha: this.fecha, tipo: this.tipo });
  }
}
