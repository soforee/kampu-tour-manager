import { AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Input, OnDestroy, Output, inject } from '@angular/core';
import { IconComponent } from './icon.component';

let siguienteId = 0;
const ENFOCABLES = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

// Ventana modal accesible: Esc cierra, el foco entra al abrir, Tab no sale de la ventana y al cerrar vuelve a donde estaba
@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './modal.component.html'
})
export class ModalComponent implements AfterViewInit, OnDestroy {
  @Input({ required: true }) titulo = '';
  @Output() cerrar = new EventEmitter<void>();

  idTitulo = 'modal-titulo-' + (++siguienteId);
  private el = inject(ElementRef<HTMLElement>);
  private previo = document.activeElement as HTMLElement | null;

  ngAfterViewInit() {
    // Primer campo del formulario (o el botón de cerrar si no hay campos)
    const panel = this.el.nativeElement.querySelector('[data-panel]') as HTMLElement;
    const campos = this.enfocables();
    const primero = campos.find(e => !e.matches('[data-cerrar]')) ?? campos[0];
    setTimeout(() => (primero ?? panel)?.focus({ preventScroll: true }));
  }

  ngOnDestroy() {
    this.previo?.focus?.({ preventScroll: true });
  }

  @HostListener('keydown', ['$event'])
  teclado(e: KeyboardEvent) {
    if (e.key === 'Escape') { e.stopPropagation(); this.cerrar.emit(); return; }
    if (e.key !== 'Tab') return;
    const campos = this.enfocables();
    if (!campos.length) return;
    const primero = campos[0], ultimo = campos[campos.length - 1];
    if (e.shiftKey && document.activeElement === primero) { e.preventDefault(); ultimo.focus(); }
    else if (!e.shiftKey && document.activeElement === ultimo) { e.preventDefault(); primero.focus(); }
  }

  private enfocables(): HTMLElement[] {
    return Array.from(this.el.nativeElement.querySelectorAll(ENFOCABLES) as NodeListOf<HTMLElement>)
      .filter(e => e.offsetParent !== null);
  }
}
