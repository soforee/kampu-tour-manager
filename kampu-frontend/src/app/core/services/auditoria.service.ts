import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { FiltroAuditoria, Pagina, RegistroAuditoria } from '../models';
import { API } from '../utils/api';

// Bitácora AUDITORIA (solo ADMIN: el backend exige VER_AUDITORIA)
@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private http = inject(HttpClient);

  buscar(f: FiltroAuditoria, pagina: number, tamanio: number): Observable<Pagina<RegistroAuditoria>> {
    return this.http.get<Pagina<RegistroAuditoria>>(`${API}/auditoria`,
      { params: this.params(f).set('pagina', pagina).set('tamanio', tamanio) });
  }

  filtros(): Observable<{ entidades: string[]; usuarios: string[] }> {
    return this.http.get<{ entidades: string[]; usuarios: string[] }>(`${API}/auditoria/filtros`);
  }

  // CSV con los mismos filtros (se descarga con el token de la sesión)
  exportar(f: FiltroAuditoria): Observable<Blob> {
    return this.http.get(`${API}/auditoria/exportar`, { params: this.params(f), responseType: 'blob' });
  }

  private params(f: FiltroAuditoria): HttpParams {
    let p = new HttpParams();
    const campos: [string, unknown][] = [['desde', f.desde], ['hasta', f.hasta], ['rol', f.rol], ['accion', f.accion],
      ['entidad', f.entidad], ['usuario', f.usuario], ['q', f.q.trim()], ['entidadId', f.entidadId]];
    for (const [k, v] of campos) if (v !== null && v !== undefined && v !== '') p = p.set(k, String(v));
    return p;
  }
}
