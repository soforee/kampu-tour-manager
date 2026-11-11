import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import {
  CategoriaLicencia, EstadoPersonal, EstadoProveedor, EstadoVehiculo, Lugar, LugarForm, PersonalForm, ProveedorForm,
  RolPersonal, TipoLugar, TipoVehiculo, VehiculoForm
} from '../../core/models';
import { TourStore } from '../../core/services/tour-store.service';
import { AlertService } from '../../core/services/alert.service';
import { AuthService } from '../../core/services/auth.service';
import { IconComponent } from '../../shared/icon.component';
import { BadgeComponent } from '../../shared/badge.component';
import { filtrarEscritura, filtrarPegado, sinRueda } from '../../core/utils/filtro';
import { FormValidacion, telefonoRepetido, V } from '../../core/utils/validacion';
import { etiqueta } from '../../core/utils/etiquetas';
import { addDays, hoy } from '../../core/utils/date.util';
import { ModalComponent } from '../../shared/modal.component';

type Tab = 'proveedores' | 'personal' | 'vehiculos' | 'lugares';

/**
 * Logística v4: PROVEEDORES (Kampu Tours propio + terceros), PERSONAL (guías y conductores),
 * VEHICULOS y LUGARES. La flota y el personal propios cuelgan de Kampu Tours.
 */
@Component({
  selector: 'app-recursos',
  standalone: true,
  imports: [FormsModule, DatePipe, IconComponent, BadgeComponent, ModalComponent],
  templateUrl: './recursos.component.html',
  styleUrl: './recursos.component.css'
})
export class RecursosComponent {
  store = inject(TourStore);
  private alerts = inject(AlertService);
  private auth = inject(AuthService);

  tabs: { id: Tab; texto: string; icono: string; singular: string }[] = [
    { id: 'proveedores', texto: 'Proveedores', icono: 'flag', singular: 'proveedor' },
    { id: 'personal', texto: 'Personal', icono: 'id', singular: 'guía o conductor' },
    { id: 'vehiculos', texto: 'Vehículos', icono: 'truck', singular: 'vehículo' },
    { id: 'lugares', texto: 'Lugares', icono: 'map-pin', singular: 'lugar' }
  ];
  categorias: CategoriaLicencia[] = ['A-I', 'A-IIa', 'A-IIb', 'A-IIIa', 'A-IIIb', 'A-IIIc'];
  estadosProveedor: EstadoProveedor[] = ['ACTIVO', 'INACTIVO', 'SUSPENDIDO'];
  roles: RolPersonal[] = ['GUIA', 'CONDUCTOR', 'AMBOS'];
  estadosPersonal: EstadoPersonal[] = ['DISPONIBLE', 'OCUPADO', 'INACTIVO'];
  tiposVehiculo: TipoVehiculo[] = ['AUTO', 'CAMIONETA_4X4', 'VAN', 'MINIBUS', 'BUS'];
  estadosVehiculo: EstadoVehiculo[] = ['OPERATIVO', 'MANTENIMIENTO', 'INACTIVO'];
  tiposLugar: TipoLugar[] = ['AEROPUERTO', 'TERMINAL', 'HOTEL', 'ATRACTIVO', 'CIUDAD', 'OTRO'];
  etiqueta = etiqueta;
  hoy = hoy();
  pronto = addDays(hoy(), 30);

  // ?tab=personal|vehiculos: llega desde los avisos de licencias y mantenimiento del panel
  tab = signal<Tab>(this.tabInicial());
  private tabInicial(): Tab {
    const t = inject(ActivatedRoute).snapshot.queryParamMap.get('tab');
    return t === 'personal' || t === 'vehiculos' || t === 'lugares' ? t : 'proveedores';
  }
  filtroProveedor = signal<number | null>(null);
  filtroRol = signal<'' | RolPersonal>('');
  abierto = signal(false);
  editId: number | null = null;

  // Debe declararse ANTES de los formularios: personaVacia() y vehiculoVacio() lo usan al inicializarse.
  // (Declarado después, el componente fallaba con "this.propio is not a function" y Logística quedaba en blanco.)
  propio = computed(() => this.store.proveedores().find(p => p.esPropio));

  proveedor: ProveedorForm = this.proveedorVacio();
  persona: PersonalForm = this.personaVacia();
  vehiculo: VehiculoForm = this.vehiculoVacio();
  lugar: LugarForm = this.lugarVacio();

  // GESTIONAR_LOGISTICA (PKG_SEGURIDAD_ROLES): Admin y Operador; Ventas solo consulta (VER_LOGISTICA)
  puedeEditar = computed(() => this.auth.puede('GESTIONAR_LOGISTICA'));

  v = new FormValidacion(() => {
    switch (this.tab()) {
      case 'proveedores': {
        const p = this.proveedor;
        return {
          pNombre: V.texto(p.nombreComercial, 'El nombre comercial', 150, true),
          pRuc: V.ruc(p.ruc),
          pTelefono: V.telefono(p.telefono)
            ?? (telefonoRepetido(p.telefono, this.store.proveedores(), x => x.telefono, x => x.proveedorId === this.editId)
              ? 'Ese teléfono ya está registrado para otro proveedor.' : null),
          pPropio: p.esPropio && this.store.proveedores().some(x => x.esPropio && x.proveedorId !== this.editId)
            ? 'Ya existe el proveedor propio (Kampu Tours).' : null
        };
      }
      case 'personal': {
        const p = this.persona;
        const conduce = p.rol !== 'GUIA';
        const lic = p.numLicencia.trim();
        return {
          rProveedor: p.proveedorId ? null : 'Selecciona el proveedor.',
          rNombre: V.nombrePersona(p.nombreCompleto),
          rTelefono: V.telefono(p.telefono)
            ?? (telefonoRepetido(p.telefono, this.store.personal(), x => x.telefono, x => x.personalId === this.editId)
              ? 'Ese teléfono ya está registrado para otra persona.' : null),
          rLicencia: !conduce && !lic ? null
            : (!lic ? 'Quien conduce necesita número de licencia.'
              : (/^[A-Za-z0-9]{6,20}$/.test(lic) ? null : 'La licencia solo admite letras y números (6 a 20 caracteres).')),
          rCategoria: conduce && !p.catLicencia ? 'Elige la categoría de la licencia.' : null,
          rVencimiento: conduce ? V.fecha(p.vencLicencia, 'La fecha de vencimiento') : null
        };
      }
      case 'vehiculos': return {
        vProveedor: this.vehiculo.proveedorId ? null : 'Selecciona el proveedor.',
        vPlaca: V.placa(this.vehiculo.placa),
        vMarca: V.texto(this.vehiculo.marcaModelo, 'La marca/modelo', 100),
        vCapacidad: V.entero(this.vehiculo.capacidadPax, 'La capacidad', 1, 999),
        vMantenimiento: V.fecha(this.vehiculo.proxMantenimiento, 'La fecha del próximo mantenimiento', false)
      };
      case 'lugares': {
        const l = this.lugar;
        const vacioLat = l.latitud === null || (l.latitud as unknown) === '';
        const vacioLon = l.longitud === null || (l.longitud as unknown) === '';
        return {
          lNombre: V.texto(l.nombre, 'El nombre', 150, true),
          lCiudad: V.texto(l.ciudad, 'La ciudad', 100, true),
          lLatitud: vacioLat ? (vacioLon ? null : 'Indica también la latitud.') : (l.latitud! < -90 || l.latitud! > 90 ? 'La latitud va de -90 a 90.' : null),
          lLongitud: vacioLon ? (vacioLat ? null : 'Indica también la longitud.') : (l.longitud! < -180 || l.longitud! > 180 ? 'La longitud va de -180 a 180.' : null)
        };
      }
    }
  });

  personalFiltrado = computed(() => {
    const p = this.filtroProveedor();
    const r = this.filtroRol();
    return this.store.personal().filter(x => (!p || x.proveedorId === p) && (!r || x.rol === r || (x.rol === 'AMBOS' && r !== 'AMBOS')));
  });
  vehiculosFiltrados = computed(() => {
    const p = this.filtroProveedor();
    return this.store.vehiculos().filter(v => !p || v.proveedorId === p);
  });
  lugaresPorCiudad = computed(() => {
    const grupos: { ciudad: string; lugares: Lugar[] }[] = [];
    for (const l of this.store.lugares()) {
      let g = grupos.find(x => x.ciudad === l.ciudad);
      if (!g) { g = { ciudad: l.ciudad, lugares: [] }; grupos.push(g); }
      g.lugares.push(l);
    }
    return grupos;
  });
  proveedoresActivos = computed(() => this.store.proveedores().filter(p => p.estado !== 'INACTIVO'));

  get actual() { return this.tabs.find(t => t.id === this.tab())!; }

  // ---------- Formularios ----------
  private proveedorVacio(): ProveedorForm { return { nombreComercial: '', ruc: '', telefono: '', esPropio: false, estado: 'ACTIVO' }; }
  private personaVacia(): PersonalForm {
    return { proveedorId: this.filtroProveedor() ?? this.propio()?.proveedorId ?? null, nombreCompleto: '', rol: 'GUIA', telefono: '',
      numLicencia: '', catLicencia: '', vencLicencia: '', estado: 'DISPONIBLE' };
  }
  private vehiculoVacio(): VehiculoForm {
    return { proveedorId: this.filtroProveedor() ?? this.propio()?.proveedorId ?? null, placa: '', tipoVehiculo: 'VAN', marcaModelo: '',
      capacidadPax: 12, proxMantenimiento: addDays(hoy(), 180), estado: 'OPERATIVO' };
  }
  private lugarVacio(): LugarForm { return { nombre: '', tipoLugar: 'HOTEL', ciudad: '', latitud: null, longitud: null }; }

  private abrir() { this.v.reiniciar(); this.abierto.set(true); }

  nuevo() {
    this.editId = null;
    this.proveedor = this.proveedorVacio();
    this.persona = this.personaVacia();
    this.vehiculo = this.vehiculoVacio();
    this.lugar = this.lugarVacio();
    this.abrir();
  }

  editarProveedor(id: number) {
    const p = this.store.getProveedor(id)!;
    this.editId = id;
    this.proveedor = { nombreComercial: p.nombreComercial, ruc: p.ruc, telefono: p.telefono, esPropio: p.esPropio, estado: p.estado };
    this.abrir();
  }
  editarPersona(id: number) {
    const p = this.store.getPersonal(id)!;
    this.editId = id;
    this.persona = { proveedorId: p.proveedorId, nombreCompleto: p.nombreCompleto, rol: p.rol, telefono: p.telefono,
      numLicencia: p.numLicencia ?? '', catLicencia: p.catLicencia ?? '', vencLicencia: p.vencLicencia ?? '', estado: p.estado };
    this.abrir();
  }
  editarVehiculo(id: number) {
    const x = this.store.getVehiculo(id)!;
    this.editId = id;
    this.vehiculo = { proveedorId: x.proveedorId, placa: x.placa, tipoVehiculo: x.tipoVehiculo, marcaModelo: x.marcaModelo,
      capacidadPax: x.capacidadPax, proxMantenimiento: x.proxMantenimiento ?? '', estado: x.estado };
    this.abrir();
  }
  editarLugar(id: number) {
    const l = this.store.getLugar(id)!;
    this.editId = id;
    this.lugar = { nombre: l.nombre, tipoLugar: l.tipoLugar, ciudad: l.ciudad, latitud: l.latitud, longitud: l.longitud };
    this.abrir();
  }

  guardar() {
    if (this.store.ocupado()) return;
    if (!this.v.valido()) { this.alerts.toast('Revisa los campos marcados en rojo', 'error'); return; }
    const t = (s: string) => (s ?? '').trim();
    const num = (x: number | null) => x === null || (x as unknown) === '' ? null : x;
    let peticion: Observable<unknown>;
    switch (this.tab()) {
      case 'proveedores': {
        const p = this.proveedor;
        peticion = this.store.guardarProveedor({ ...p, nombreComercial: t(p.nombreComercial), ruc: t(p.ruc), telefono: t(p.telefono) }, this.editId);
        break;
      }
      case 'personal': {
        const p = this.persona;
        const conduce = p.rol !== 'GUIA';
        const lic = t(p.numLicencia).toUpperCase();
        peticion = this.store.guardarPersonal({ ...p, nombreCompleto: t(p.nombreCompleto), telefono: t(p.telefono),
          numLicencia: conduce || lic ? lic : '', catLicencia: conduce || lic ? p.catLicencia : '',
          vencLicencia: conduce || lic ? p.vencLicencia : '' }, this.editId);
        break;
      }
      case 'vehiculos': {
        const x = this.vehiculo;
        peticion = this.store.guardarVehiculo({ ...x, placa: t(x.placa).toUpperCase(), marcaModelo: t(x.marcaModelo),
          proxMantenimiento: x.proxMantenimiento || '' }, this.editId);
        break;
      }
      default: {
        const l = this.lugar;
        peticion = this.store.guardarLugar({ ...l, nombre: t(l.nombre), ciudad: t(l.ciudad), latitud: num(l.latitud), longitud: num(l.longitud) }, this.editId);
      }
    }
    this.store.ocupado.set(true);
    peticion.subscribe({
      next: () => {
        this.store.ocupado.set(false);
        this.recargar();
        this.abierto.set(false);
        this.alerts.toast('Guardado');
      },
      error: (err) => {
        this.store.ocupado.set(false);
        this.alerts.error(this.store.error(err));
      }
    });
  }

  // La flota y el personal cambian los totales de cada proveedor
  private recargar() {
    if (this.tab() === 'lugares') { this.store.cargarLugares(); return; }
    if (this.tab() === 'personal') this.store.cargarPersonal();
    if (this.tab() === 'vehiculos') this.store.cargarVehiculos();
    this.store.cargarProveedores();
  }

  async eliminar(id: number) {
    const tab = this.tab();
    if (!(await this.alerts.confirm('Esta acción no se puede deshacer.', 'Eliminar ' + this.actual.singular))) return;
    const peticiones: { [t in Tab]: () => Observable<void> } = {
      proveedores: () => this.store.eliminarProveedor(id),
      personal: () => this.store.eliminarPersonal(id),
      vehiculos: () => this.store.eliminarVehiculo(id),
      lugares: () => this.store.eliminarLugar(id)
    };
    peticiones[tab]().subscribe({
      next: () => { this.recargar(); this.alerts.toast('Eliminado'); },
      error: (err) => this.alerts.error(this.store.error(err))
    });
  }

  filtrar(evento: Event, tipo: string) { filtrarEscritura(evento, tipo); }
  pegar(evento: Event, tipo: string) { filtrarPegado(evento, tipo); }
  rueda(evento: Event) { sinRueda(evento); }
}
