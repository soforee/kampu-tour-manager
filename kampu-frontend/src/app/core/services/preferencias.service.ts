import { Injectable, signal } from '@angular/core';

// Preferencias visuales del usuario en este navegador: tema claro/oscuro y menú colapsado.
@Injectable({ providedIn: 'root' })
export class PreferenciasService {
  oscuro = signal(document.documentElement.classList.contains('dark'));
  menuColapsado = signal(this.leer('tm_menu') === 'colapsado');

  alternarTema() {
    const valor = !this.oscuro();
    this.oscuro.set(valor);
    document.documentElement.classList.toggle('dark', valor);
    this.guardar('tm_tema', valor ? 'oscuro' : 'claro');
  }

  alternarMenu() {
    const valor = !this.menuColapsado();
    this.menuColapsado.set(valor);
    this.guardar('tm_menu', valor ? 'colapsado' : 'abierto');
  }

  private leer(clave: string): string | null {
    try { return localStorage.getItem(clave); } catch { return null; }
  }

  private guardar(clave: string, valor: string) {
    try { localStorage.setItem(clave, valor); } catch { /* modo privado: solo dura la sesión */ }
  }
}
