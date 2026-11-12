import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EstadoEjecucion, PortalPaquete } from '../../core/models';
import { AuthService } from '../../core/services/auth.service';
import { ImagenService } from '../../core/services/imagen.service';
import { PortalService } from '../../core/services/portal.service';
import { mensajeError } from '../../core/utils/api';
import { cuentaRegresiva, porcentajePagado } from '../../core/utils/viaje';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';
import { PortalHeaderComponent } from './portal-header.component';

type Filtro = 'TODOS' | EstadoEjecucion;

// "Mis paquetes": GET /portal/paquetes -> VW_PAQUETES_CLIENTE (filtrada en Oracle por el cliente de la sesión).
// Solo lectura: estado de ejecución y estado de pago (FN_ESTADO_PAGO_PAQUETE).
@Component({
  selector: 'app-mis-paquetes',
  standalone: true,
  imports: [RouterLink, DatePipe, DecimalPipe, BadgeComponent, IconComponent, PortalHeaderComponent],
  templateUrl: './mis-paquetes.component.html'
})
export class MisPaquetesComponent {
  auth = inject(AuthService);
  private portal = inject(PortalService);
  private imagenes = inject(ImagenService);

  paquetes = signal<PortalPaquete[]>([]);
  cargando = signal(true);
  error = signal('');
  filtro = signal<Filtro>('TODOS');
  bienvenido = !!inject(ActivatedRoute).snapshot.queryParamMap.get('bienvenido');

  filtros: { valor: Filtro; texto: string }[] = [
    { valor: 'TODOS', texto: 'Todos' }, { valor: 'POR_EMPEZAR', texto: 'Por empezar' },
    { valor: 'EN_CURSO', texto: 'En curso' }, { valor: 'TERMINADO', texto: 'Terminados' }
  ];

  visibles = computed(() => {
    const f = this.filtro();
    return f === 'TODOS' ? this.paquetes() : this.paquetes().filter(p => p.estadoEjecucion === f);
  });
  contar = (f: Filtro) => f === 'TODOS' ? this.paquetes().length : this.paquetes().filter(p => p.estadoEjecucion === f).length;
  saldoTotal = computed(() => this.paquetes().filter(p => p.estadoEjecucion !== 'CANCELADO').reduce((s, p) => s + p.saldo, 0));

  constructor() {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.portal.misPaquetes().subscribe({
      next: lista => { this.paquetes.set(lista); this.cargando.set(false); },
      error: err => { this.error.set(mensajeError(err)); this.cargando.set(false); }
    });
  }

  imagen(p: PortalPaquete) { return this.imagenes.url(p.imagen); }
  faltan = cuentaRegresiva;
  pagado = porcentajePagado;
}
