import { AuthService } from '../../core/services/auth.service';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TourStore } from '../../core/services/tour-store.service';
import { AlertService } from '../../core/services/alert.service';
import { RouterLink } from '@angular/router';
import { EstadoSalida, SalidaForm } from '../../core/models';
import { addDays, hoy } from '../../core/utils/date.util';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';
import { filtrarEscritura, filtrarPegado, sinRueda } from '../../core/utils/filtro';
import { FormValidacion, V } from '../../core/utils/validacion';
import { ModalComponent } from '../../shared/modal.component';

@Component({
  selector: 'app-salidas',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink, BadgeComponent, IconComponent, ModalComponent],
  templateUrl: './salidas.component.html',
  styleUrl: './salidas.component.css'
})
export class SalidasComponent {
  store = inject(TourStore);
  private alerts = inject(AlertService);
  // GESTIONAR_CATALOGO: solo Admin y Ventas crean/editan salidas (el Operador solo las consulta)
  editable = inject(AuthService).puede('GESTIONAR_CATALOGO');

  estados: EstadoSalida[] = ['PROGRAMADA', 'CONFIRMADA', 'COMPLETADA', 'CANCELADA'];
  filtros = [{ valor: '', texto: 'Todas' }, { valor: 'PROGRAMADA', texto: 'Programadas' }, { valor: 'CONFIRMADA', texto: 'Confirmadas' },
             { valor: 'COMPLETADA', texto: 'Completadas' }, { valor: 'CANCELADA', texto: 'Canceladas' }];
  filtro = signal('');
  abierto = signal(false);
  editId: number | null = null;
  form: SalidaForm = this.vacio();
  v = new FormValidacion(() => {
    const f = this.form;
    return {
      paquete: f.paqueteId ? null : 'Selecciona un paquete.',
      inicio: V.fecha(f.fechaInicio, 'La fecha de inicio'),
      cupos: V.entero(f.cuposMaximos, 'Los cupos máximos', 1, 999)
    };
  });

  private vacio(): SalidaForm {
    return { paqueteId: 0, fechaInicio: hoy(), cuposMaximos: 10, guiaId: null, estado: 'PROGRAMADA' };
  }

  // Igual que trg_salida_biu: fecha fin = inicio + días del paquete - 1 (solo para mostrarla)
  get fechaFin(): string {
    const p = this.store.getPaquete(this.form.paqueteId);
    return p && this.form.fechaInicio ? addDays(this.form.fechaInicio, p.dias - 1) : '';
  }

  // Al confirmar, trg_salida_confirmar exige transporte suficiente (PKG_ASIGNACION.faltante_transporte)
  get faltanteActual(): string | null {
    return this.editId ? this.store.getSalida(this.editId)?.faltanteTransporte ?? null : null;
  }

  filas = computed(() => {
    const f = this.filtro();
    return this.store.salidas()
      .filter(s => !f || s.estado === f)
      .sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio))
      .map(s => ({
        salida: s,
        paquete: this.store.getPaquete(s.paqueteId),
        guia: this.store.getGuia(s.guiaId),
        vendidos: s.cuposMaximos - s.cuposDisponibles,
        pct: Math.round((s.cuposMaximos - s.cuposDisponibles) * 100 / s.cuposMaximos)
      }));
  });

  nuevo() { this.editId = null; this.form = this.vacio(); this.v.reiniciar(); this.abierto.set(true); }

  editar(id: number) {
    const sal = this.store.getSalida(id)!;
    this.editId = sal.salidaId;
    this.form = {
      paqueteId: sal.paqueteId, fechaInicio: sal.fechaInicio, cuposMaximos: sal.cuposMaximos,
      guiaId: sal.guiaId ?? null, estado: sal.estado
    };
    this.v.reiniciar();
    this.abierto.set(true);
  }

  guardar() {
    if (this.store.ocupado()) return;
    if (!this.v.valido()) { this.alerts.toast('Revisa los campos marcados en rojo', 'error'); return; }
    const editando = this.editId !== null;
    this.store.ocupado.set(true);
    this.store.guardarSalida(this.form, this.editId).subscribe({
      next: () => {
        this.store.ocupado.set(false);
        this.store.cargarSalidas();
        this.abierto.set(false);
        this.alerts.toast(editando ? 'Salida actualizada' : 'Salida creada');
      },
      error: (err) => {
        this.store.ocupado.set(false);
        this.alerts.error(this.store.error(err));
      }
    });
  }

  async eliminar(id: number) {
    if (!(await this.alerts.confirm('Esta acción no se puede deshacer.', 'Eliminar salida'))) return;
    this.store.eliminarSalida(id).subscribe({
      next: () => {
        this.store.cargarSalidas();
        this.alerts.toast('Salida eliminada');
      },
      error: (err) => this.alerts.error(this.store.error(err))
    });
  }

  filtrar(evento: Event, tipo: string) { filtrarEscritura(evento, tipo); }
  pegar(evento: Event, tipo: string) { filtrarPegado(evento, tipo); }
  rueda(evento: Event) { sinRueda(evento); }
}
