import { Component } from '@angular/core';
import { IconComponent } from '../../../shared/icon.component';
import { RevealDirective } from '../../../shared/reveal.directive';

@Component({
  selector: 'app-landing-beneficios',
  standalone: true,
  imports: [IconComponent, RevealDirective],
  templateUrl: './landing-beneficios.component.html',
  styleUrl: './landing-beneficios.component.css'
})
export class LandingBeneficiosComponent {
  beneficios = [
    { icono: 'calendar', titulo: 'Reserva en minutos', texto: 'Elige la salida, registra a tus pasajeros y asegura tu cupo sin trámites.' },
    { icono: 'lock', titulo: 'Pagos seguros', texto: 'Paga en soles, dólares o euros, en una o varias cuotas, con cada pago registrado.' },
    { icono: 'users', titulo: 'Guías locales', texto: 'Profesionales que conocen cada ruta y te acompañan durante todo el viaje.' },
    { icono: 'chart', titulo: 'Todo bajo control', texto: 'Cupos en tiempo real y confirmación inmediata del estado de tu reserva.' }
  ];
}
