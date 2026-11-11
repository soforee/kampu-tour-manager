import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AfiliacionForm, Permiso, RegistroForm, Sesion } from '../models';
import { API } from '../utils/api';

const KEY = 'tm4_session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private sesion = signal<Sesion | null>(this.cargar());
  usuario = computed(() => this.sesion());
  autenticado = computed(() => this.sesion() !== null);
  esCliente = computed(() => this.sesion()?.rol === 'CLIENTE');
  // Empresa de transporte afiliada: solo ve su flota y los servicios que se le asignan
  esProveedor = computed(() => this.sesion()?.rol === 'PROVEEDOR');
  rol = computed(() => this.sesion()?.rol ?? null);
  // Permisos de la sesión (PKG_SEGURIDAD_ROLES.permisos): menú, rutas y botones dependen de esto
  permisos = computed(() => new Set<string>(this.sesion()?.permisos ?? []));

  puede(permiso: Permiso): boolean {
    return this.permisos().has(permiso);
  }

  private cargar(): Sesion | null {
    const texto = localStorage.getItem(KEY);
    if (!texto) return null;
    try {
      const s = JSON.parse(texto);
      return s && s.token && s.rol ? { ...s, permisos: Array.isArray(s.permisos) ? s.permisos : [] } : null;
    } catch {
      return null;
    }
  }

  // Personal interno: usuario · Cliente: correo o número de documento
  login(username: string, password: string): Observable<Sesion> {
    return this.http.post<Sesion>(`${API}/auth/login`, { username: username.trim(), password: password });
  }

  // "Registrarse": crea la cuenta del cliente y devuelve la sesión iniciada
  registrar(d: RegistroForm): Observable<Sesion> {
    return this.http.post<Sesion>(`${API}/auth/registro`, d);
  }

  // Afiliación de una empresa de transporte (queda PENDIENTE hasta que Kampu la apruebe)
  afiliar(d: AfiliacionForm): Observable<Sesion> {
    return this.http.post<Sesion>(`${API}/auth/afiliacion`, d);
  }

  guardarSesion(s: Sesion) {
    this.sesion.set(s);
    localStorage.setItem(KEY, JSON.stringify(s));
  }

  // Vuelve a pedir la sesión al backend (permisos al día; sesiones guardadas por versiones anteriores)
  refrescar(): Observable<Sesion> {
    return this.http.get<Sesion>(`${API}/auth/sesion`).pipe(tap(s => this.guardarSesion(s)));
  }

  // Ruta de inicio según el rol
  inicio(): string {
    if (this.esCliente()) return '/mi-cuenta';
    return this.esProveedor() ? '/operaciones' : '/dashboard';
  }

  logout() {
    if (this.sesion()) this.http.post(`${API}/auth/logout`, {}).subscribe({ error: () => undefined });
    this.olvidar();
  }

  // Sin llamar al backend (token vencido o rechazado)
  olvidar() {
    this.sesion.set(null);
    localStorage.removeItem(KEY);
  }
}
