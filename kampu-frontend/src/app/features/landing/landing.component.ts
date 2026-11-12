import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { PaqueteTour, SalidaProgramada } from '../../core/models';
import { API } from '../../core/utils/api';
import { hoy } from '../../core/utils/date.util';
import { LandingNavbarComponent } from './components/landing-navbar.component';
import { LandingHeroComponent } from './components/landing-hero.component';
import { LandingSearchComponent } from './components/landing-search.component';
import { LandingToursComponent } from './components/landing-tours.component';
import { LandingBeneficiosComponent } from './components/landing-beneficios.component';
import { LandingFooterComponent } from './components/landing-footer.component';
import { LandingTestimoniosComponent } from './components/landing-testimonios.component';
import { IconComponent } from '../../shared/icon.component';
import { Contacto, FiltroBusqueda, TourCard } from './landing.models';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [LandingNavbarComponent, LandingHeroComponent, LandingSearchComponent, LandingToursComponent,
    LandingBeneficiosComponent, LandingFooterComponent, LandingTestimoniosComponent, IconComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.css'
})
export class LandingComponent {
  private http = inject(HttpClient);

  private gradientes = [
    'from-zinc-800 to-zinc-900', 'from-brand-500 to-brand-700', 'from-earth-500 to-earth-700',
    'from-emerald-600 to-zinc-900', 'from-brand-600 to-zinc-900', 'from-earth-400 to-zinc-900'
  ];

  contacto: Contacto = { email: 'reservas@kampu.pe', telefono: '+51 984 000 000', ciudad: 'Cusco, Perú' };
  // wa.me necesita solo dígitos con el código de país
  whatsapp = this.contacto.telefono.replace(/\D/g, '');

  paquetes = signal<PaqueteTour[]>([]);
  salidas = signal<SalidaProgramada[]>([]);
  cargando = signal(true);
  filtro = signal<FiltroBusqueda>({ destino: '', fecha: '', tipo: '' });

  hayFiltro = computed(() => {
    const f = this.filtro();
    return f.destino.trim() !== '' || f.fecha !== '' || f.tipo !== '';
  });

  destinos = computed(() => this.paquetes().filter(p => p.estado === 'ACTIVO').map(p => p.nombre));

  tours = computed<TourCard[]>(() => {
    const f = this.filtro();
    const texto = f.destino.trim().toLowerCase();
    return this.paquetes()
      .filter(p => p.estado === 'ACTIVO')
      .filter(p => !texto || p.nombre.toLowerCase().includes(texto) || (p.descripcion ?? '').toLowerCase().includes(texto))
      .filter(p => this.cumpleTipo(p, f.tipo))
      .map(p => ({
        paquete: p,
        gradiente: this.gradientes[p.paqueteId % this.gradientes.length],
        duracion: this.duracion(p),
        proximaSalida: this.proximaSalida(p.paqueteId, f.fecha)
      }))
      .filter(t => !f.fecha || t.proximaSalida !== null);
  });

  constructor() {
    this.http.get<PaqueteTour[]>(`${API}/paquetes`).subscribe({
      next: lista => {
        this.paquetes.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
    this.http.get<SalidaProgramada[]>(`${API}/salidas`).subscribe({
      next: lista => this.salidas.set(lista),
      error: () => this.salidas.set([])
    });
  }

  buscar(f: FiltroBusqueda) {
    this.filtro.set(f);
    document.getElementById('tours')?.scrollIntoView({ behavior: 'smooth' });
  }

  limpiar() {
    this.filtro.set({ destino: '', fecha: '', tipo: '' });
  }

  private cumpleTipo(p: PaqueteTour, tipo: string): boolean {
    if (tipo === 'FULL_DAY') return p.dias <= 1;
    if (tipo === 'ESCAPADA') return p.dias >= 2 && p.dias <= 3;
    if (tipo === 'EXPEDICION') return p.dias >= 4;
    return true;
  }

  private duracion(p: PaqueteTour): string {
    const dias = p.dias === 1 ? '1 día' : `${p.dias} días`;
    if (!p.noches) return dias;
    return `${dias} · ${p.noches === 1 ? '1 noche' : p.noches + ' noches'}`;
  }

  // Primera salida con cupos desde la fecha elegida (o desde hoy).
  private proximaSalida(paqueteId: number, desde: string): string | null {
    const hoyStr = hoy();
    const inicio = desde && desde > hoyStr ? desde : hoyStr;
    const fechas = this.salidas()
      .filter(s => s.paqueteId === paqueteId && s.cuposDisponibles > 0)
      .filter(s => s.estado === 'PROGRAMADA' || s.estado === 'CONFIRMADA')
      .filter(s => s.fechaInicio >= inicio)
      .map(s => s.fechaInicio)
      .sort();
    return fechas.length > 0 ? fechas[0] : null;
  }
}
