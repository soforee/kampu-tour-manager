import { EstadoOperacion, Operacion } from '../models';

// Utilidades del centro de operaciones (tablero, calendario y flota)

export const EN_TRANSITO: EstadoOperacion[] = ['EN_RUTA', 'LLEGADA_PROXIMA'];
export const POR_SALIR: EstadoOperacion[] = ['PROGRAMADO', 'ASIGNADO', 'EMBARQUE', 'EN_ESPERA_SALIDA', 'RETRASADO'];
export const CERRADOS: EstadoOperacion[] = ['FINALIZADO', 'CANCELADO'];

// Color de cada estado: punto/barra (bg) y texto del tablero oscuro (fids)
export const COLOR_ESTADO: { [estado: string]: { bg: string; fids: string } } = {
  PROGRAMADO: { bg: 'bg-zinc-400', fids: 'text-zinc-300' },
  ASIGNADO: { bg: 'bg-sky-500', fids: 'text-sky-300' },
  EMBARQUE: { bg: 'bg-violet-500', fids: 'text-violet-300' },
  EN_ESPERA_SALIDA: { bg: 'bg-indigo-500', fids: 'text-indigo-300' },
  EN_RUTA: { bg: 'bg-brand-500', fids: 'text-emerald-300' },
  LLEGADA_PROXIMA: { bg: 'bg-teal-500', fids: 'text-teal-300' },
  FINALIZADO: { bg: 'bg-emerald-600', fids: 'text-emerald-400' },
  RETRASADO: { bg: 'bg-orange-500', fids: 'text-orange-300' },
  CANCELADO: { bg: 'bg-rose-500', fids: 'text-rose-400' }
};

export function colorEstado(estado: string): { bg: string; fids: string } {
  return COLOR_ESTADO[estado] ?? COLOR_ESTADO['PROGRAMADO'];
}

// Hora a la que realmente sale: la reprogramada si está retrasada
export function salidaEfectiva(o: Operacion): Date {
  return new Date(o.horaSalidaEstimada ?? o.horaProgramadaSalida);
}

// El vehículo está ocupado por la operación en este momento
export function ocupaAhora(o: Operacion, ahora: Date): boolean {
  if (!o.vehiculoId || CERRADOS.includes(o.estadoActual)) return false;
  if (EN_TRANSITO.includes(o.estadoActual) || o.estadoActual === 'EMBARQUE' || o.estadoActual === 'EN_ESPERA_SALIDA') return true;
  return new Date(o.horaProgramadaSalida) <= ahora && new Date(o.horaEstimadaLlegada) > ahora;
}

// Ya debió salir y sigue sin partir (o salió más de 15 min tarde)
export function estaRetrasada(o: Operacion, ahora: Date): boolean {
  if (o.estadoActual === 'RETRASADO') return true;
  if ((o.minRetrasoSalida ?? 0) > 15) return true;
  return ['PROGRAMADO', 'ASIGNADO', 'EMBARQUE', 'EN_ESPERA_SALIDA'].includes(o.estadoActual)
    && salidaEfectiva(o).getTime() + 15 * 60000 < ahora.getTime();
}

export function mismoDia(iso: string, fecha: string): boolean {
  return iso.slice(0, 10) === fecha;
}

// 'YYYY-MM-DDTHH:mm' local para inputs datetime-local
export function aInputFecha(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
}
