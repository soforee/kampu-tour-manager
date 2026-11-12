import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TourStore } from '../../core/services/tour-store.service';
import { AlertService } from '../../core/services/alert.service';
import { ImagenService } from '../../core/services/imagen.service';
import { PaqueteForm } from '../../core/models';
import { hoy } from '../../core/utils/date.util';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';
import { filtrarEscritura, filtrarPegado, sinRueda } from '../../core/utils/filtro';
import { FormValidacion, V } from '../../core/utils/validacion';
import { LogoComponent } from '../../shared/logo.component';
import { ModalComponent } from '../../shared/modal.component';

@Component({
  selector: 'app-paquetes',
  standalone: true,
  imports: [FormsModule, RouterLink, CurrencyPipe, BadgeComponent, IconComponent, LogoComponent, ModalComponent],
  templateUrl: './paquetes.component.html'
})
export class PaquetesComponent {
  store = inject(TourStore);
  private alerts = inject(AlertService);
  imagenes = inject(ImagenService);

  private gradientes = [
    'from-zinc-800 to-zinc-900', 'from-brand-500 to-brand-700', 'from-earth-500 to-earth-700',
    'from-emerald-600 to-zinc-900', 'from-brand-600 to-zinc-900', 'from-earth-400 to-zinc-900'
  ];

  texto = signal('');
  abierto = signal(false);
  fotoSeleccionada: File | null = null;
  vistaNueva = signal<string | null>(null);
  quitar = signal(false);
  editId: number | null = null;
  form: PaqueteForm = this.vacio();
  v = new FormValidacion(() => ({
    nombre: V.texto(this.form.nombre, 'El nombre', 100, true),
    descripcion: (this.form.descripcion ?? '').length > 1000 ? 'La descripción admite máximo 1000 caracteres.' : null,
    dias: V.entero(this.form.dias, 'Los días', 1, 99),
    noches: V.entero(this.form.noches, 'Las noches', 0, 99)
      ?? (this.form.noches > this.form.dias ? 'Las noches no pueden ser más que los días.' : null),
    precio: V.importe(this.form.precioBase, 'El precio base')
  }));

  private vacio(): PaqueteForm {
    return { nombre: '', descripcion: '', precioBase: 0, dias: 1, noches: 0, imagen: '', estado: 'ACTIVO' };
  }

  filas = computed(() => {
    const t = this.texto().toLowerCase().trim();
    const hoyStr = hoy();
    return this.store.paquetes()
      .filter(p => !t || p.nombre.toLowerCase().includes(t) || p.descripcion.toLowerCase().includes(t))
      .map(p => ({
        paquete: p,
        gradiente: this.gradientes[p.paqueteId % this.gradientes.length],
        proximas: this.store.salidasDe(p.paqueteId)
          .filter(s => (s.estado === 'PROGRAMADA' || s.estado === 'CONFIRMADA') && s.fechaInicio >= hoyStr && s.cuposDisponibles > 0).length
      }));
  });

  get vistaPrevia(): string | null {
    if (this.quitar()) return null;
    if (this.vistaNueva()) return this.vistaNueva();
    return this.imagenes.url(this.form.imagen);
  }

  elegir(event: any) {
    const file = event.target.files[0];
    event.target.value = '';
    if (file) this.procesar(file);
  }

  soltar(event: DragEvent) {
    event.preventDefault();
    if (event.dataTransfer && event.dataTransfer.files.length > 0) this.procesar(event.dataTransfer.files[0]);
  }

  private procesar(file: File) {
    const error = this.imagenes.validar(file);
    if (error) {
      this.alerts.error(error);
      return;
    }
    this.fotoSeleccionada = file;
    this.vistaNueva.set(URL.createObjectURL(file));
    this.quitar.set(false);
  }

  quitarImagen() {
    this.fotoSeleccionada = null;
    this.vistaNueva.set(null);
    this.quitar.set(true);
  }

  nuevo() {
    this.editId = null;
    this.form = this.vacio();
    this.reiniciarImagen();
    this.v.reiniciar();
    this.abierto.set(true);
  }

  private reiniciarImagen() {
    this.fotoSeleccionada = null;
    this.vistaNueva.set(null);
    this.quitar.set(false);
  }

  editar(id: number) {
    const p = this.store.getPaquete(id)!;
    this.editId = p.paqueteId;
    this.form = {
      nombre: p.nombre, descripcion: p.descripcion, precioBase: p.precioBase, dias: p.dias, noches: p.noches,
      imagen: this.imagenes.url(p.imagen) ? p.imagen : '', estado: p.estado
    };
    this.reiniciarImagen();
    this.v.reiniciar();
    this.abierto.set(true);
  }

  sugerirNoches() {
    if (!this.editId && this.form.dias >= 1) this.form.noches = this.form.dias - 1;
  }

  guardar() {
    if (this.store.ocupado()) return;
    if (!this.v.valido()) { this.alerts.toast('Revisa los campos marcados en rojo', 'error'); return; }
    this.store.ocupado.set(true);
    if (this.fotoSeleccionada) {
      this.imagenes.subir(this.fotoSeleccionada).subscribe({
        next: (r) => this.enviarPaquete(r.imagen),
        error: (err) => this.fallo(err)
      });
    } else {
      this.enviarPaquete(this.quitar() ? '' : this.form.imagen);
    }
  }

  private enviarPaquete(imagen: string) {
    const f = this.form;
    const datos: PaqueteForm = {
      nombre: f.nombre.trim(), descripcion: (f.descripcion ? f.descripcion : '').trim(), precioBase: f.precioBase,
      dias: f.dias, noches: f.noches, imagen: imagen, estado: f.estado
    };
    const editando = this.editId !== null;
    this.store.guardarPaquete(datos, this.editId).subscribe({
      next: () => {
        this.store.ocupado.set(false);
        this.store.cargarPaquetes();
        this.abierto.set(false);
        this.alerts.toast(editando ? 'Paquete actualizado' : 'Paquete creado');
      },
      error: (err) => this.fallo(err)
    });
  }

  private fallo(err: unknown) {
    this.store.ocupado.set(false);
    this.alerts.error(this.store.error(err));
  }

  async eliminar(id: number) {
    if (!(await this.alerts.confirm('Esta acción no se puede deshacer.', 'Eliminar paquete'))) return;
    this.store.eliminarPaquete(id).subscribe({
      next: () => {
        this.store.cargarPaquetes();
        this.alerts.toast('Paquete eliminado');
      },
      error: (err) => this.alerts.error(this.store.error(err))
    });
  }

  filtrar(evento: Event, tipo: string) { filtrarEscritura(evento, tipo); }
  pegar(evento: Event, tipo: string) { filtrarPegado(evento, tipo); }
  rueda(evento: Event) { sinRueda(evento); }
}
