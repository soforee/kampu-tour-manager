import { Component, HostListener, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { IconComponent } from '../../../shared/icon.component';
import { LogoComponent } from '../../../shared/logo.component';

@Component({
  selector: 'app-landing-navbar',
  standalone: true,
  imports: [RouterLink, IconComponent, LogoComponent],
  templateUrl: './landing-navbar.component.html',
  styleUrl: './landing-navbar.component.css'
})
export class LandingNavbarComponent {
  auth = inject(AuthService);

  solido = signal(false);
  menu = signal(false);

  links = [
    { href: '#tours', texto: 'Tours' },
    { href: '#beneficios', texto: 'Por qué elegirnos' }
  ];

  // Transparente sobre la portada; sólida al bajar.
  @HostListener('window:scroll')
  alHacerScroll() {
    this.solido.set(window.scrollY > 24);
  }

  @HostListener('window:resize')
  alCambiarTamano() {
    if (window.innerWidth >= 768) this.menu.set(false);
  }

  cerrarMenu() {
    this.menu.set(false);
  }
}
