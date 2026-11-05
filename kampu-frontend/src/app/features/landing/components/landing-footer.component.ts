import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconComponent } from '../../../shared/icon.component';
import { LogoComponent } from '../../../shared/logo.component';
import { RevealDirective } from '../../../shared/reveal.directive';
import { Contacto } from '../landing.models';

@Component({
  selector: 'app-landing-footer',
  standalone: true,
  imports: [RouterLink, IconComponent, LogoComponent, RevealDirective],
  templateUrl: './landing-footer.component.html',
  styleUrl: './landing-footer.component.css'
})
export class LandingFooterComponent {
  @Input({ required: true }) contacto!: Contacto;

  anio = new Date().getFullYear();

  redes = [
    { nombre: 'Instagram', url: 'https://instagram.com' },
    { nombre: 'Facebook', url: 'https://facebook.com' },
    { nombre: 'WhatsApp', url: 'https://wa.me/' }
  ];

  get telefonoLink(): string {
    return 'tel:' + this.contacto.telefono.replace(/\s/g, '');
  }
}
