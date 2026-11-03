import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { SesionGuardada } from '../models';
import { API } from '../utils/api';

const KEY = 'tm3_session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private sesion = signal<SesionGuardada | null>(this.cargar());
  usuario = computed(() => {
    const s = this.sesion();
    return s ? s.usuario : null;
  });
  autenticado = computed(() => this.sesion() !== null);

  private cargar(): SesionGuardada | null {
    const texto = localStorage.getItem(KEY);
    if (!texto) return null;
    try {
      const s = JSON.parse(texto);
      return s && s.token && s.usuario ? s : null;
    } catch {
      return null;
    }
  }

  token(): string | null {
    const s = this.sesion();
    return s ? s.token : null;
  }

  login(username: string, password: string): Observable<SesionGuardada> {
    return this.http.post<SesionGuardada>(`${API}/auth/login`, { username: username.trim(), password: password });
  }

  guardarSesion(s: SesionGuardada) {
    this.sesion.set(s);
    localStorage.setItem(KEY, JSON.stringify(s));
  }

  logout() {
    const token = this.token();
    if (token) {
      this.http.post(`${API}/auth/logout`, {}, { headers: { Authorization: 'Bearer ' + token } }).subscribe({ error: () => {} });
    }
    this.cerrarSesionLocal();
  }

  cerrarSesionLocal() {
    this.sesion.set(null);
    localStorage.removeItem(KEY);
  }
}
