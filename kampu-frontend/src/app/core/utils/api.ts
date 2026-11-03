import { HttpErrorResponse } from '@angular/common/http';

export const API = '/api/v1';

export const IMAGENES = '/lp2_gp2/images';

export function mensajeError(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) return 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.';
    const cuerpo = e.error;
    if (cuerpo && typeof cuerpo === 'object' && typeof cuerpo.error === 'string') return cuerpo.error;
    return `El servidor respondió con un error (${e.status}).`;
  }
  return e instanceof Error ? e.message : 'Ocurrió un error inesperado.';
}
