import { Directive, ElementRef, inject, Input, OnDestroy, OnInit } from '@angular/core';

// Aparición suave hacia arriba (fade-in-up) cuando el elemento entra en pantalla.
@Directive({
  selector: '[appReveal]',
  standalone: true
})
export class RevealDirective implements OnInit, OnDestroy {
  @Input() revealDelay = 0;

  private el = inject(ElementRef<HTMLElement>);
  private observer?: IntersectionObserver;

  ngOnInit() {
    const elemento: HTMLElement = this.el.nativeElement;
    elemento.classList.add('reveal');
    elemento.style.transitionDelay = this.revealDelay + 'ms';

    if (!('IntersectionObserver' in window)) {
      elemento.classList.add('reveal-visible');
      return;
    }
    this.observer = new IntersectionObserver(entradas => {
      for (const entrada of entradas) {
        if (entrada.isIntersecting) {
          elemento.classList.add('reveal-visible');
          this.observer?.disconnect();
        }
      }
    }, { threshold: 0.15, rootMargin: '0px 0px -40px 0px' });
    this.observer.observe(elemento);
  }

  ngOnDestroy() {
    this.observer?.disconnect();
  }
}
