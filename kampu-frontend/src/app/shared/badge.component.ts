import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-badge',
  standalone: true,
  templateUrl: './badge.component.html',
  styleUrl: './badge.component.css'
})
export class BadgeComponent {
  @Input({ required: true }) estado = '';

  private mapa: { [estado: string]: string } = {
    PENDIENTE: 'bg-amber-50 text-amber-700 ring-amber-200/70',
    PARCIAL: 'bg-sky-50 text-sky-700 ring-sky-200/70',
    PROGRAMADA: 'bg-sky-50 text-sky-700 ring-sky-200/70',
    CONFIRMADA: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    APROBADO: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    PAGADO: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    COMPLETADA: 'bg-zinc-100 text-zinc-700 ring-zinc-200',
    ANULADA: 'bg-rose-50 text-rose-700 ring-rose-200/70',
    CANCELADA: 'bg-rose-50 text-rose-700 ring-rose-200/70',
    RECHAZADO: 'bg-rose-50 text-rose-700 ring-rose-200/70',
    REEMBOLSADO: 'bg-violet-50 text-violet-700 ring-violet-200/70',
    ACTIVO: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    INACTIVO: 'bg-zinc-100 text-zinc-500 ring-zinc-200',
    // Proveedores de transporte
    ACTIVA: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    INACTIVA: 'bg-zinc-100 text-zinc-500 ring-zinc-200',
    SUSPENDIDA: 'bg-rose-50 text-rose-700 ring-rose-200/70',
    RETIRADA: 'bg-zinc-100 text-zinc-500 ring-zinc-200',
    DISPONIBLE: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    LIMITADA: 'bg-amber-50 text-amber-700 ring-amber-200/70',
    NO_DISPONIBLE: 'bg-rose-50 text-rose-700 ring-rose-200/70',
    OPERATIVO: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    EN_MANTENIMIENTO: 'bg-amber-50 text-amber-700 ring-amber-200/70',
    FUERA_DE_SERVICIO: 'bg-rose-50 text-rose-700 ring-rose-200/70',
    DESCANSO: 'bg-amber-50 text-amber-700 ring-amber-200/70',
    VACACIONES: 'bg-sky-50 text-sky-700 ring-sky-200/70',
    // Operaciones de transporte (DOMINIO_ESTADOS)
    PROGRAMADO: 'bg-zinc-100 text-zinc-700 ring-zinc-200',
    ASIGNADO: 'bg-sky-50 text-sky-700 ring-sky-200/70',
    EMBARQUE: 'bg-violet-50 text-violet-700 ring-violet-200/70',
    EN_ESPERA_SALIDA: 'bg-violet-50 text-violet-700 ring-violet-200/70',
    EN_RUTA: 'bg-brand-50 text-brand-700 ring-brand-200/70',
    LLEGADA_PROXIMA: 'bg-leaf-50 text-leaf-700 ring-leaf-200/70',
    FINALIZADO: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    RETRASADO: 'bg-amber-50 text-amber-700 ring-amber-200/70',
    CANCELADO: 'bg-rose-50 text-rose-700 ring-rose-200/70',
    // Portal del cliente (v4): estado de ejecución y estado de pago
    POR_EMPEZAR: 'bg-sky-50 text-sky-700 ring-sky-200/70',
    EN_CURSO: 'bg-brand-50 text-brand-700 ring-brand-200/70',
    TERMINADO: 'bg-zinc-100 text-zinc-700 ring-zinc-200',
    PAGADO_COMPLETO: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    FALTA_PAGAR: 'bg-amber-50 text-amber-700 ring-amber-200/70',
    PENDIENTE_PAGO: 'bg-rose-50 text-rose-700 ring-rose-200/70',
    // Auditoría: acciones
    CREAR: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
    ACTUALIZAR: 'bg-sky-50 text-sky-700 ring-sky-200/70',
    ELIMINAR: 'bg-rose-50 text-rose-700 ring-rose-200/70',
    REGISTRO: 'bg-violet-50 text-violet-700 ring-violet-200/70',
    LOGIN: 'bg-zinc-100 text-zinc-700 ring-zinc-200',
    LOGOUT: 'bg-zinc-100 text-zinc-500 ring-zinc-200',
    LOGIN_FALLIDO: 'bg-amber-50 text-amber-700 ring-amber-200/70'
  };

  // Textos legibles de los estados del portal
  private textos: { [estado: string]: string } = {
    POR_EMPEZAR: 'Por empezar', EN_CURSO: 'En curso', TERMINADO: 'Terminado',
    PAGADO_COMPLETO: 'Pagado completo', FALTA_PAGAR: 'Falta pagar', PENDIENTE_PAGO: 'Pendiente de pago',
    CREAR: 'Alta', ACTUALIZAR: 'Modificación', ELIMINAR: 'Baja', REGISTRO: 'Registro web',
    LOGIN: 'Inicio de sesión', LOGOUT: 'Cierre de sesión', LOGIN_FALLIDO: 'Ingreso fallido'
  };

  get clase() { return this.mapa[this.estado] ?? 'bg-zinc-100 text-zinc-700 ring-zinc-200'; }
  get texto() { return this.textos[this.estado] ?? this.estado.replace(/_/g, ' '); }
}
