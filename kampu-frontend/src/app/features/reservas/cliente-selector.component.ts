import { Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild, inject, signal } from '@angular/core';
import { Subject, Subscription, catchError, debounceTime, distinctUntilChanged, filter, map, of, switchMap, tap } from 'rxjs';
import { Cliente } from '../../core/models';
import { TourStore } from '../../core/services/tour-store.service';
import { IconComponent } from '../../shared/icon.component';

interface Trozo { texto: string; marca: boolean; }

// Buscador de clientes con autocompletado: busca en el servidor mientras se escribe
// (espera 300 ms sin teclear) por nombre, DNI/pasaporte o correo.
@Component({
  selector: 'app-cliente-selector',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './cliente-selector.component.html',
  styleUrl: './cliente-selector.component.css'
})
export class ClienteSelectorComponent implements OnInit, OnDestroy {
  @Input() seleccionado: Cliente | null = null;
  @Input() invalido = false;
  @Output() seleccionar = new EventEmitter<Cliente | null>();
  @Output() sinRegistro = new EventEmitter<string>();
  @Output() tocado = new EventEmitter<void>();
  @ViewChild('entrada') entrada?: ElementRef<HTMLInputElement>;

  private store = inject(TourStore);
  private textos = new Subject<string>();
  private sub?: Subscription;

  texto = signal('');
  resultados = signal<Cliente[]>([]);
  abierto = signal(false);
  cargando = signal(false);
  buscado = signal(false);
  activo = signal(-1);
  error = signal('');

  ngOnInit() {
    this.sub = this.textos.pipe(
      map(t => t.trim()),
      debounceTime(300),
      distinctUntilChanged(),
      tap(t => {
        this.error.set('');
        if (t.length < 2) {
          this.resultados.set([]);
          this.buscado.set(false);
          this.cargando.set(false);
        }
      }),
      filter(t => t.length >= 2),
      tap(() => this.cargando.set(true)),
      // switchMap cancela la búsqueda anterior si el usuario sigue escribiendo
      switchMap(t => this.store.buscarClientes(t).pipe(
        catchError(e => {
          this.error.set(this.store.error(e));
          return of([] as Cliente[]);
        })
      ))
    ).subscribe(lista => {
      this.cargando.set(false);
      this.buscado.set(true);
      this.resultados.set(lista);
      this.activo.set(lista.length > 0 ? 0 : -1);
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  escribir(valor: string) {
    this.texto.set(valor);
    this.abierto.set(true);
    if (valor.trim().length >= 2) this.cargando.set(true);
    this.textos.next(valor);
  }

  teclear(evento: KeyboardEvent) {
    const n = this.resultados().length;
    if (evento.key === 'ArrowDown') {
      evento.preventDefault();
      this.abierto.set(true);
      if (n > 0) this.activo.set((this.activo() + 1) % n);
    } else if (evento.key === 'ArrowUp') {
      evento.preventDefault();
      if (n > 0) this.activo.set((this.activo() - 1 + n) % n);
    } else if (evento.key === 'Enter') {
      if (this.abierto() && this.activo() >= 0 && this.activo() < n) {
        evento.preventDefault();
        this.elegir(this.resultados()[this.activo()]);
      }
    } else if (evento.key === 'Escape') {
      this.abierto.set(false);
    }
  }

  salir() {
    this.abierto.set(false);
    this.tocado.emit();
  }

  elegir(c: Cliente) {
    this.seleccionar.emit(c);
    this.abierto.set(false);
    this.texto.set('');
    this.resultados.set([]);
    this.buscado.set(false);
    this.textos.next('');
  }

  cambiar() {
    this.seleccionar.emit(null);
    setTimeout(() => this.entrada?.nativeElement.focus());
  }

  invitado() {
    this.abierto.set(false);
    this.sinRegistro.emit(this.texto().trim());
  }

  iniciales(nombre: string): string {
    const partes = nombre.trim().split(/\s+/);
    return ((partes[0]?.[0] ?? '') + (partes[1]?.[0] ?? '')).toUpperCase();
  }

  // Divide el texto para resaltar la parte que coincide con la búsqueda
  resaltar(valor: string): Trozo[] {
    const q = this.texto().trim();
    if (!valor || q.length < 2) return [{ texto: valor ?? '', marca: false }];
    const i = valor.toLowerCase().indexOf(q.toLowerCase());
    if (i < 0) return [{ texto: valor, marca: false }];
    return [
      { texto: valor.slice(0, i), marca: false },
      { texto: valor.slice(i, i + q.length), marca: true },
      { texto: valor.slice(i + q.length), marca: false }
    ];
  }
}
