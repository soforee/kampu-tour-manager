import { AuthService } from '../../core/services/auth.service';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TourStore } from '../../core/services/tour-store.service';
import { AlertService } from '../../core/services/alert.service';
import { RouterLink } from '@angular/router';
import { EstadoSalida, SalidaForm } from '../../core/models';
import { addDays, cuando, diasHasta, hoy } from '../../core/utils/date.util';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';
import { EsqueletoComponent } from '../../shared/esqueleto.component';
import { filtrarEscritura, filtrarPegado, sinRueda } from '../../core/utils/filtro';
import { FormValidacion, V } from '../../core/utils/validacion';
import { ModalComponent } from '../../shared/modal.component';

@Component({
  selector: 'app-salidas',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink, BadgeComponent, IconComponent, ModalComponent, EsqueletoComponent],
  templateUrl: './salidas.component.html'
})
export class SalidasComponent {
  store = inject(TourStore);
  private alerts = inject(AlertService);
  // GESTIONAR_CATALOGO: solo Admin y Ventas crean/editan salidas (el Operador solo las consulta)
  editable = inject(AuthService).puede('GESTIONAR_CATALOGO');

  // Mismas transiciones que valida el backend (SalidaProgramadaServiceImpl)
  private transiciones: { [estado: string]: EstadoSalida[] } = {
    PROGRAMADA: ['PROGRAMADA', 'CONFIRMADA', 'CANCELADA'],
    CONFIRMADA: ['CONFIRMADA', 'PROGRAMADA', 'COMPLETADA', 'CANCELADA']
  };
  estadoOriginal: EstadoSalida = 'PROGRAMADA';
  get estados(): EstadoSalida[] { return this.transiciones[this.estadoOriginal] ?? [this.estadoOriginal]; }
  hoy = hoy();
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
      inicio: V.fecha(f.fechaInicio, 'La fecha de inicio')
        ?? (f.fechaInicio < this.hoy && f.fechaInicio !== this.fechaOriginal ? 'La fecha de inicio no puede ser anterior a hoy.' : null),
      cupos: V.entero(f.cuposMaximos, 'Los cupos máximos', 1, 999)
    };
  });

  private vacio(): SalidaForm {
    return { paqueteId: 0, fechaInicio: hoy(), cuposMaximos: 10, guiaId: null, estado: 'PROGRAMADA' };
  }

  private fechaOriginal = '';

  // Fecha fin = inicio + días del paquete - 1 (la guarda el backend; aquí solo se muestra)
  get fechaFin(): string {
    const p = this.store.getPaquete(this.form.paqueteId);
    return p && this.form.fechaInicio ? addDays(this.form.fechaInicio, p.dias - 1) : '';
  }

  // Para confirmar, el transporte asignado debe alcanzar en todos los tramos
  get faltanteActual(): string | null {
    return this.editId ? this.store.getSalida(this.editId)?.faltanteTransporte ?? null : null;
  }

  filas = computed(() => {
    const f = this.filtro();
    const abierta = (e: string) => e === 'PROGRAMADA' || e === 'CONFIRMADA';
    // Primero lo que viene (la más cercana arriba); después lo cerrado, de lo más reciente a lo más antiguo
    return this.store.salidas()
      .filter(s => !f || s.estado === f)
      .sort((a, b) => abierta(a.estado) !== abierta(b.estado) ? (abierta(a.estado) ? -1 : 1)
        : abierta(a.estado) ? a.fechaInicio.localeCompare(b.fechaInicio) : b.fechaInicio.localeCompare(a.fechaInicio))
      .map(s => ({
        salida: s,
        abierta: abierta(s.estado),
        cuando: this.textoCuando(s.fechaInicio, s.fechaFin),
        urgente: abierta(s.estado) && diasHasta(s.fechaInicio) <= 3 && (!s.guiaId || !!s.faltanteTransporte),
        paquete: this.store.getPaquete(s.paqueteId),
        guia: this.store.getGuia(s.guiaId),
        vendidos: s.cuposMaximos - s.cuposDisponibles,
        pct: Math.round((s.cuposMaximos - s.cuposDisponibles) * 100 / s.cuposMaximos)
      }));
  });

  private textoCuando(inicio: string, fin: string): string {
    const i = diasHasta(inicio);
    const f = diasHasta(fin);
    if (i > 0) return 'Sale ' + cuando(i);
    if (f >= 0) return 'En curso';
    return 'Terminó ' + cuando(f);
  }

  nuevo() {
    this.editId = null;
    this.form = this.vacio();
    this.estadoOriginal = 'PROGRAMADA';
    this.fechaOriginal = '';
    this.v.reiniciar();
    this.abierto.set(true);
  }

  editar(id: number) {
    const sal = this.store.getSalida(id)!;
    this.editId = sal.salidaId;
    this.estadoOriginal = sal.estado;
    this.fechaOriginal = sal.fechaInicio;
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
      next: (s) => {
        this.store.resaltar('salida-' + s.salidaId);
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
