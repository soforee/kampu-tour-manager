import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { GuiaForm, TransporteForm } from '../../core/models';
import { TourStore } from '../../core/services/tour-store.service';
import { AlertService } from '../../core/services/alert.service';
import { IconComponent } from '../../shared/icon.component';
import { filtrarEscritura, filtrarPegado, sinRueda } from '../../core/utils/filtro';
import { FormValidacion, V } from '../../core/utils/validacion';
import { ModalComponent } from '../../shared/modal.component';

@Component({
  selector: 'app-recursos',
  standalone: true,
  imports: [FormsModule, IconComponent, ModalComponent],
  templateUrl: './recursos.component.html',
  styleUrl: './recursos.component.css'
})
export class RecursosComponent {
  store = inject(TourStore);
  private alerts = inject(AlertService);

  tab = signal<'guias' | 'transporte'>('guias');
  abierto = signal(false);
  editId: number | null = null;
  guia = { nombreCompleto: '', especialidad: '', telefono: '' };
  transp = { placa: '', marcaModelo: '', capacidad: 10 as number | null };
  v = new FormValidacion(() => {
    if (this.tab() === 'guias') {
      return {
        gNombre: V.nombrePersona(this.guia.nombreCompleto),
        gEspecialidad: V.texto(this.guia.especialidad, 'La especialidad', 100),
        gTelefono: V.telefono(this.guia.telefono)
      };
    }
    return {
      tPlaca: V.placa(this.transp.placa),
      tMarca: V.texto(this.transp.marcaModelo, 'La marca/modelo', 100),
      tCapacidad: V.entero(this.transp.capacidad, 'La capacidad', 1, 999)
    };
  });

  nuevo() {
    this.editId = null;
    this.guia = { nombreCompleto: '', especialidad: '', telefono: '' };
    this.transp = { placa: '', marcaModelo: '', capacidad: 10 };
    this.v.reiniciar();
    this.abierto.set(true);
  }

  editarGuia(id: number) {
    const g = this.store.getGuia(id)!;
    this.editId = g.guiaId;
    this.guia = { nombreCompleto: g.nombreCompleto, especialidad: g.especialidad, telefono: g.telefono };
    this.v.reiniciar();
    this.abierto.set(true);
  }

  editarTransporte(id: number) {
    const t = this.store.getTransporte(id)!;
    this.editId = t.transporteId;
    this.transp = { placa: t.placa, marcaModelo: t.marcaModelo, capacidad: t.capacidad };
    this.v.reiniciar();
    this.abierto.set(true);
  }

  guardar() {
    if (this.store.ocupado()) return;
    if (!this.v.valido()) { this.alerts.toast('Revisa los campos marcados en rojo', 'error'); return; }
    if (this.tab() === 'guias') {
      const g = this.guia;
      const datos: GuiaForm = { nombreCompleto: g.nombreCompleto.trim(), especialidad: g.especialidad.trim(), telefono: g.telefono.trim() };
      this.store.ocupado.set(true);
      this.store.guardarGuia(datos, this.editId).subscribe({
        next: () => this.guardado(),
        error: (err) => this.fallo(err)
      });
    } else {
      const t = this.transp;
      const datos: TransporteForm = { placa: t.placa.trim(), marcaModelo: t.marcaModelo.trim(), capacidad: t.capacidad! };
      this.store.ocupado.set(true);
      this.store.guardarTransporte(datos, this.editId).subscribe({
        next: () => this.guardado(),
        error: (err) => this.fallo(err)
      });
    }
  }

  private guardado() {
    this.store.ocupado.set(false);
    if (this.tab() === 'guias') this.store.cargarGuias();
    else this.store.cargarTransportes();
    this.abierto.set(false);
    this.alerts.toast('Guardado');
  }

  private fallo(err: unknown) {
    this.store.ocupado.set(false);
    this.alerts.error(this.store.error(err));
  }

  async eliminarGuia(id: number) {
    if (!(await this.alerts.confirm('Esta acción no se puede deshacer.', 'Eliminar guía'))) return;
    this.store.eliminarGuia(id).subscribe({
      next: () => {
        this.store.cargarGuias();
        this.alerts.toast('Guía eliminado');
      },
      error: (err) => this.alerts.error(this.store.error(err))
    });
  }

  async eliminarTransporte(id: number) {
    if (!(await this.alerts.confirm('Esta acción no se puede deshacer.', 'Eliminar transporte'))) return;
    this.store.eliminarTransporte(id).subscribe({
      next: () => {
        this.store.cargarTransportes();
        this.alerts.toast('Transporte eliminado');
      },
      error: (err) => this.alerts.error(this.store.error(err))
    });
  }

  filtrar(evento: Event, tipo: string) { filtrarEscritura(evento, tipo); }
  pegar(evento: Event, tipo: string) { filtrarPegado(evento, tipo); }
  rueda(evento: Event) { sinRueda(evento); }
}
