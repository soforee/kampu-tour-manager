import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { PAISES, PaisTelefono, bandera, separarTelefono, unirTelefono } from '../core/utils/paises';

// Teléfono con código de país. Uso: <app-telefono-input [(valor)]="form.telefono" />
// El valor es "+51 987654321"; el campo no deja escribir más dígitos de los que admite el país.
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

  paises = PAISES;
  pais: PaisTelefono = PAISES[0];
  numero = '';
  private ultimoEmitido: string | null = null;

  ngOnChanges() {
    // Solo se vuelve a separar si el valor cambió desde fuera (al abrir el formulario de edición)
    if (this.valor === this.ultimoEmitido) return;
    const t = separarTelefono(this.valor);
    this.pais = t.pais;
    this.numero = t.numero.slice(0, t.pais.max);
  }

  bandera(p: PaisTelefono) { return bandera(p.iso); }

  get digitos() { return this.numero.length; }

  get ayuda(): string {
    const p = this.pais;
    return p.min === p.max ? `${p.max} dígitos` : `${p.min} a ${p.max} dígitos`;
  }

  cambiarPais(iso: string) {
    this.pais = PAISES.find(p => p.iso === iso) ?? PAISES[0];
    this.numero = this.numero.slice(0, this.pais.max);
    this.emitir();
  }

  escribir(campo: HTMLInputElement) {
    // Solo dígitos y como máximo los que admite el país
    const limpio = campo.value.replace(/\D/g, '').slice(0, this.pais.max);
    if (campo.value !== limpio) campo.value = limpio;
    this.numero = limpio;
    this.emitir();
  }

  private emitir() {
    this.ultimoEmitido = unirTelefono(this.pais, this.numero);
    this.valorChange.emit(this.ultimoEmitido);
  }
}
