import { Component, Input, OnDestroy, OnInit, signal } from '@angular/core';
import { IconComponent } from '../../../shared/icon.component';

@Component({
  selector: 'app-landing-hero',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './landing-hero.component.html',
  styleUrl: './landing-hero.component.css'
})
export class LandingHeroComponent implements OnInit, OnDestroy {
  @Input() totalTours = 0;

  lugares = ['Cusco', 'los Andes', 'la Amazonía', 'el Titicaca'];
  indice = signal(0);
  private intervalo?: ReturnType<typeof setInterval>;

  ngOnInit() {
    this.intervalo = setInterval(() => this.indice.set((this.indice() + 1) % this.lugares.length), 2800);
  }

  ngOnDestroy() {
    clearInterval(this.intervalo);
  }

  get textoCta(): string {
    if (this.totalTours === 1) return 'Ver 1 tour disponible';
    if (this.totalTours > 1) return `Ver ${this.totalTours} tours disponibles`;
    return 'Explorar tours';
  }
}
