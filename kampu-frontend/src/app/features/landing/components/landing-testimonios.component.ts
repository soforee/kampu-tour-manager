import { Component } from '@angular/core';
import { IconComponent } from '../../../shared/icon.component';
import { RevealDirective } from '../../../shared/reveal.directive';

// Testimonios DE EJEMPLO para la maqueta: reemplázalos por reseñas reales de clientes (con su permiso) antes de publicar.
@Component({
  selector: 'app-landing-testimonios',
  standalone: true,
  imports: [IconComponent, RevealDirective],
  template: `
  <section id="testimonios" class="scroll-mt-24" style="background: var(--kp-bg)">
    <div class="mx-auto max-w-6xl px-5 py-24 sm:px-8">
      <div class="mx-auto max-w-2xl text-center" appReveal>
        <p class="eyebrow">Testimonios</p>
        <h2 class="mt-3 text-3xl font-black tracking-tight sm:text-[40px] sm:leading-tight" style="color: var(--kp-text)">Lo que cuentan nuestros viajeros</h2>
      </div>
      <div class="mt-14 grid gap-5 md:grid-cols-3">
        @for (t of testimonios; track t.nombre; let i = $index) {
          <figure appReveal [revealDelay]="i * 100" class="flex h-full flex-col rounded-3xl p-7" style="background: var(--kp-bg-alt); border: 1px solid var(--kp-border)">
            <div class="flex gap-0.5" [attr.aria-label]="t.estrellas + ' de 5 estrellas'">
              @for (e of [1, 2, 3, 4, 5]; track e) { <app-icon name="star" size="w-4 h-4" [class]="e <= t.estrellas ? 'text-leaf-500 [&_polygon]:fill-current' : 'text-zinc-300'" /> }
            </div>
            <blockquote class="mt-4 flex-1 leading-relaxed" style="color: var(--kp-text)">“{{ t.texto }}”</blockquote>
            <figcaption class="mt-6 flex items-center gap-3">
              <span class="grid h-10 w-10 place-items-center rounded-full text-sm font-bold" style="background: var(--kp-primary-soft); color: var(--kp-primary)">{{ t.nombre[0] }}</span>
              <span><span class="block text-sm font-bold" style="color: var(--kp-text)">{{ t.nombre }}</span>
                <span class="block text-xs" style="color: var(--kp-muted)">{{ t.tour }} · {{ t.origen }}</span></span>
            </figcaption>
          </figure>
        }
      </div>
    </div>
  </section>`
})
export class LandingTestimoniosComponent {
  testimonios = [
    { nombre: 'Camila R.', origen: 'Lima', tour: 'Machu Picchu Clásico', estrellas: 5, texto: 'Todo estuvo coordinado: el traslado nos esperaba en el aeropuerto y el guía conocía cada rincón de la ciudadela.' },
    { nombre: 'Mark T.', origen: 'Toronto', tour: 'Arequipa y Cañón del Colca', estrellas: 5, texto: 'Vimos los cóndores en la Cruz del Cóndor. Recibimos el detalle de la reserva y los pagos por correo, muy claro.' },
    { nombre: 'Lucía P.', origen: 'Santiago', tour: 'Paracas e Ica Aventura', estrellas: 4, texto: 'Las Islas Ballestas y el sandboard en Huacachina fueron lo mejor del viaje. Volveremos por la Amazonía.' }
  ];
}
