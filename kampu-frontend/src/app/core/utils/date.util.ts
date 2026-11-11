function pad(n: number): string {
  return n < 10 ? '0' + n : '' + n;
}

export function toIsoDate(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function hoy(): string {
  return toIsoDate(new Date());
}

export function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number);
  return toIsoDate(new Date(y, m - 1, d + days));
}

// Días desde hoy hasta la fecha (negativo si ya pasó). Acepta 'yyyy-MM-dd' o fecha con hora.
export function diasHasta(iso: string): number {
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number);
  const hoyDia = new Date();
  const inicio = new Date(hoyDia.getFullYear(), hoyDia.getMonth(), hoyDia.getDate()).getTime();
  return Math.round((new Date(y, m - 1, d).getTime() - inicio) / 86400000);
}

// "hoy", "mañana", "en 5 días", "hace 3 días"
export function cuando(dias: number): string {
  if (dias === 0) return 'hoy';
  if (dias === 1) return 'mañana';
  if (dias === -1) return 'ayer';
  return dias > 0 ? `en ${dias} días` : `hace ${-dias} días`;
}
