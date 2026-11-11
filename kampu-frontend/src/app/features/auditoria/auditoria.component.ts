import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AccionAuditoria, FiltroAuditoria, RegistroAuditoria } from '../../core/models';
import { AlertService } from '../../core/services/alert.service';
import { AuditoriaService } from '../../core/services/auditoria.service';
import { mensajeError } from '../../core/utils/api';
import { addDays, hoy } from '../../core/utils/date.util';
import { etiqueta } from '../../core/utils/etiquetas';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';

/**
 * Sección "Auditoría" (solo ADMIN, permiso VER_AUDITORIA).
 * Bitácora completa: altas, modificaciones campo por campo, bajas, registros web,
 * inicios/cierres de sesión e ingresos fallidos. Filtros combinables, paginación en el
 * servidor, historial de un registro y exportación a CSV.
 */
@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [FormsModule, RouterLink, DatePipe, BadgeComponent, IconComponent],
  templateUrl: './auditoria.component.html'
})
export class AuditoriaComponent {
  private api = inject(AuditoriaService);
  private alerts = inject(AlertService);

  readonly tamanio = 50;
  etiqueta = etiqueta;
  hoy = hoy();

  roles = ['ADMIN', 'VENDEDOR', 'OPERADOR', 'CLIENTE', 'SISTEMA'];
  acciones: AccionAuditoria[] = ['CREAR', 'ACTUALIZAR', 'ELIMINAR', 'REGISTRO', 'LOGIN', 'LOGOUT', 'LOGIN_FALLIDO'];
  entidades = signal<string[]>([]);
  usuarios = signal<string[]>([]);

  filtro: FiltroAuditoria = this.vacio();
  filas = signal<RegistroAuditoria[]>([]);
  total = signal(0);
  pagina = signal(0);
  cargando = signal(false);
  error = signal('');
  abierta = signal<number | null>(null);
  exportando = signal(false);

  paginas = computed(() => Math.max(1, Math.ceil(this.total() / this.tamanio)));
  desdeFila = computed(() => this.total() === 0 ? 0 : this.pagina() * this.tamanio + 1);
  hastaFila = computed(() => Math.min(this.total(), (this.pagina() + 1) * this.tamanio));
  hayFiltros = computed(() => this.version() >= 0 && Object.entries(this.filtro).some(([, v]) => v !== '' && v !== null && v !== undefined));
  private version = signal(0);

  constructor() {
    this.api.filtros().subscribe({
      next: f => { this.entidades.set(f.entidades); this.usuarios.set(f.usuarios); },
      error: () => undefined
    });
    this.buscar();
  }

  private vacio(): FiltroAuditoria {
    return { desde: '', hasta: '', rol: '', accion: '', entidad: '', usuario: '', q: '', entidadId: null };
  }

  buscar(pagina = 0) {
    if (this.filtro.desde && this.filtro.hasta && this.filtro.hasta < this.filtro.desde) {
      this.error.set('La fecha "hasta" no puede ser anterior a "desde".');
      return;
    }
    this.cargando.set(true);
    this.error.set('');
    this.version.update(v => v + 1);
    this.api.buscar(this.filtro, pagina, this.tamanio).subscribe({
      next: p => {
        this.filas.set(p.contenido);
        this.total.set(p.total);
        this.pagina.set(p.pagina);
        this.abierta.set(null);
        this.cargando.set(false);
      },
      error: e => { this.error.set(mensajeError(e)); this.cargando.set(false); }
    });
  }

  rango(dias: number) {
    this.filtro.desde = dias === 0 ? this.hoy : addDays(this.hoy, -dias);
    this.filtro.hasta = this.hoy;
    this.buscar();
  }

  limpiar() {
    this.filtro = this.vacio();
    this.buscar();
  }

  // Historial completo de un registro (p. ej. todo lo que le pasó a la reserva 7)
  historial(a: RegistroAuditoria) {
    if (a.entidadId === null) return;
    this.filtro = { ...this.vacio(), entidad: a.entidad, entidadId: a.entidadId };
    this.buscar();
  }

  quitarHistorial() {
    this.filtro.entidadId = null;
    this.buscar();
  }

  exportar() {
    this.exportando.set(true);
    this.api.exportar(this.filtro).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `auditoria-${this.hoy}.csv`;
        a.click();
        URL.revokeObjectURL(url);
        this.exportando.set(false);
      },
      error: e => { this.exportando.set(false); this.alerts.error(mensajeError(e)); }
    });
  }

  // Texto del cambio en una línea
  resumen(a: RegistroAuditoria): string {
    if (a.campo && (a.valorAnterior !== null || a.valorNuevo !== null)) {
      return `${this.campo(a.campo)}: ${a.valorAnterior ?? '(vacío)'} → ${a.valorNuevo ?? '(vacío)'}`;
    }
    return a.detalle ?? '';
  }

  campo(c: string | null): string {
    return c ? c.charAt(0) + c.slice(1).toLowerCase().replace(/_/g, ' ') : '';
  }

  enlace(a: RegistroAuditoria): string[] | null {
    if (a.entidadId === null) return null;
    switch (a.entidad) {
      case 'RESERVA': return ['/reservas', String(a.entidadId)];
      default: return null;
    }
  }
}
