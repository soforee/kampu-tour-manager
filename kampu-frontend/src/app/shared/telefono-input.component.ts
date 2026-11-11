import { Component, ElementRef, EventEmitter, HostListener, Input, OnChanges, Output, ViewChild, computed, inject, signal } from '@angular/core';
import { PAISES, PaisTelefono, banderaUrl, normalizar, separarTelefono, unirTelefono } from '../core/utils/paises';

// Teléfono con código de país. Uso: <app-telefono-input [(valor)]="form.telefono" />
// El valor es "+51 987654321"; el campo no deja escribir más dígitos de los que admite el país.
// El país se elige en una lista propia con banderas y buscador (la lista nativa no admite imágenes).
@Component({
  selector: 'app-telefono-input',
  standalone: true,
  templateUrl: './telefono-input.component.html',
  styleUrl: './telefono-input.component.css'
})
export class TelefonoInputComponent implements OnChanges {
  @Input() valor = '';
  @Input() invalido = false;
  @Input() idCampo = '';
  @Output() valorChange = new EventEmitter<string>();
  @Output() tocado = new EventEmitter<void>();
  @ViewChild('buscador') buscador?: ElementRef<HTMLInputElement>;
  @ViewChild('numeroCampo') numeroCampo?: ElementRef<HTMLInputElement>;

  private host = inject(ElementRef<HTMLElement>);

  pais: PaisTelefono = PAISES[0];
  numero = '';
  abierto = signal(false);
  texto = signal('');
  activo = signal(0);
  private ultimoEmitido: string | null = null;

  // Países que coinciden con lo escrito: nombre (sin tildes) o código ("+56", "56")
  filtrados = computed(() => {
    const q = normalizar(this.texto()).replace(/^\+/, '');
    if (!q) return PAISES;
    return PAISES.filter(p => normalizar(p.nombre).includes(q) || p.codigo.startsWith(q));
  });

  ngOnChanges() {
    // Solo se vuelve a separar si el valor cambió desde fuera (al abrir el formulario de edición)
    if (this.valor === this.ultimoEmitido) return;
    const t = separarTelefono(this.valor);
    this.pais = t.pais;
    this.numero = t.numero.slice(0, t.pais.max);
  }

  bandera(p: PaisTelefono) { return banderaUrl(p.iso); }

  get digitos() { return this.numero.length; }

  get ayuda(): string {
    const p = this.pais;
    return p.min === p.max ? `${p.max} dígitos` : `${p.min} a ${p.max} dígitos`;
  }

  alternar() {
    if (this.abierto()) { this.cerrar(); return; }
    this.texto.set('');
    this.activo.set(Math.max(0, PAISES.indexOf(this.pais)));
    this.abierto.set(true);
    setTimeout(() => {
      this.buscador?.nativeElement.focus();
      this.verActivo();
    });
  }

  cerrar(devolverFoco = false) {
    if (!this.abierto()) return;
    this.abierto.set(false);
    this.tocado.emit();
    if (devolverFoco) this.numeroCampo?.nativeElement.focus();
  }

  buscar(valor: string) {
    this.texto.set(valor);
    this.activo.set(0);
  }

  teclear(evento: KeyboardEvent) {
    const n = this.filtrados().length;
    if (evento.key === 'ArrowDown') {
      evento.preventDefault();
      if (n) this.activo.set((this.activo() + 1) % n);
      this.verActivo();
    } else if (evento.key === 'ArrowUp') {
      evento.preventDefault();
      if (n) this.activo.set((this.activo() - 1 + n) % n);
      this.verActivo();
    } else if (evento.key === 'Enter') {
      evento.preventDefault();
      const p = this.filtrados()[this.activo()];
      if (p) this.elegir(p);
    } else if (evento.key === 'Escape' || evento.key === 'Tab') {
      this.cerrar(evento.key === 'Escape');
    }
  }

  elegir(p: PaisTelefono) {
    this.pais = p;
    this.numero = this.numero.slice(0, p.max);
    this.emitir();
    this.cerrar(true);
  }

  escribir(campo: HTMLInputElement) {
    // Solo dígitos y como máximo los que admite el país
    const limpio = campo.value.replace(/\D/g, '').slice(0, this.pais.max);
    if (campo.value !== limpio) campo.value = limpio;
    this.numero = limpio;
    this.emitir();
  }

  // Cierra la lista al hacer clic fuera del componente
  @HostListener('document:mousedown', ['$event'])
  alHacerClic(evento: MouseEvent) {
    if (this.abierto() && !this.host.nativeElement.contains(evento.target as Node)) this.cerrar();
  }

  private verActivo() {
    setTimeout(() => this.host.nativeElement.querySelector('.opcion-activa')?.scrollIntoView({ block: 'nearest' }));
  }

  private emitir() {
    this.ultimoEmitido = unirTelefono(this.pais, this.numero);
    this.valorChange.emit(this.ultimoEmitido);
  }
}
