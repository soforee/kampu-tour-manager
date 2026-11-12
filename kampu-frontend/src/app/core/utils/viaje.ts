import { PortalPaquete } from '../models';
import { diasHasta } from './date.util';

export type EstadoPaso = 'hecho' | 'actual' | 'pendiente';

// Línea de tiempo del viaje para el cliente: Reservado → Pagado → Por empezar → En curso → Terminado
export function lineaViaje(p: PortalPaquete): { texto: string; estado: EstadoPaso }[] {
  const ejec = p.estadoEjecucion;
  const pagado = p.estadoPago === 'PAGADO_COMPLETO';
  const empezo = ejec === 'EN_CURSO' || ejec === 'TERMINADO';
  return [
    { texto: 'Reservado', estado: 'hecho' },
    { texto: 'Pagado', estado: pagado ? 'hecho' : 'actual' },
    { texto: 'Por empezar', estado: empezo ? 'hecho' : (pagado ? 'actual' : 'pendiente') },
    { texto: 'En curso', estado: ejec === 'TERMINADO' ? 'hecho' : (ejec === 'EN_CURSO' ? 'actual' : 'pendiente') },
    { texto: 'Terminado', estado: ejec === 'TERMINADO' ? 'hecho' : 'pendiente' }
  ];
}

// "Faltan 12 días" / "¡Sale mañana!" / null si ya empezó o se canceló
export function cuentaRegresiva(p: PortalPaquete): string | null {
  if (p.estadoEjecucion !== 'POR_EMPEZAR' || p.estadoPago === 'ANULADO') return null;
  const d = diasHasta(p.fechaInicio);
  if (d < 0) return null;
  return d === 0 ? '¡Sale hoy!' : d === 1 ? '¡Sale mañana!' : `Faltan ${d} días`;
}

export function porcentajePagado(p: PortalPaquete): number {
  return p.montoTotal > 0 ? Math.min(100, Math.round(p.montoPagado * 100 / p.montoTotal)) : 0;
}
