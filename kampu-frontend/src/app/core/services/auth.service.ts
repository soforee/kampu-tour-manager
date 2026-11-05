import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { Sesion } from '../models';
import { API } from '../utils/api';

const KEY = 'tm3_session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private sesion = signal<Sesion | null>(this.cargar());
  usuario = computed(() => this.sesion());
  autenticado = computed(() => this.sesion() !== null);

  private cargar(): Sesion | null {
    const texto = localStorage.getItem(KEY);
    if (!texto) return null;
    try {
      const s = JSON.parse(texto);
      return s && s.usuarioId && s.username ? s : null;
    } catch {
      return null;
    }
  }

  login(username: string, password: string): Observable<Sesion> {
    return this.http.post<Sesion>(`${API}/auth/login`, { username: username.trim(), password: password });
  }

  guardarSesion(s: Sesion) {
    this.sesion.set(s);
    localStorage.setItem(KEY, JSON.stringify(s));
  }

  logout() {
    this.sesion.set(null);
    localStorage.removeItem(KEY);
  }
}
