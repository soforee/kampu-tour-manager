import { Pipe, PipeTransform } from '@angular/core';
import { cuando, diasHasta } from '../core/utils/date.util';

// Fecha legible: "hoy", "mañana", "en 5 días", "hace 3 días" (la fecha exacta va en el title)
@Pipe({ name: 'relativa', standalone: true })
export class RelativaPipe implements PipeTransform {
  transform(fecha?: string | null): string {
    return fecha ? cuando(diasHasta(fecha)) : '';
  }
}
