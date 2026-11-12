import { AuthService } from '../../core/services/auth.service';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { TourStore } from '../../core/services/tour-store.service';
import { AlertService } from '../../core/services/alert.service';
import { EstadoTransaccion, Moneda } from '../../core/models';
import { aPEN, TASAS } from '../../core/utils/moneda';
import { BadgeComponent } from '../../shared/badge.component';
import { IconComponent } from '../../shared/icon.component';
import { MigasComponent } from '../../shared/migas.component';
import { filtrarEscritura, filtrarPegado, sinRueda } from '../../core/utils/filtro';
import { FormValidacion, V } from '../../core/utils/validacion';

@Component({
  selector: 'app-reserva-detalle',
  standalone: true,
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, BadgeComponent, IconComponent, MigasComponent],
  templateUrl: './reserva-detalle.component.html'
})
export class ReservaDetalleComponent {
  store = inject(TourStore);
  private alerts = inject(AlertService);
  private auth = inject(AuthService);
  // GESTIONAR_RESERVAS (confirmar, completar, anular, boleta) y REGISTRAR_PAGO (registrar/validar pagos)
  gestiona = this.auth.puede('GESTIONAR_RESERVAS');
  cobra = this.auth.puede('REGISTRAR_PAGO');
  private router = inject(Router);
  private id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  monedas: Moneda[] = ['PEN', 'USD', 'EUR'];
  metodos = ['Efectivo', 'Tarjeta', 'Transferencia', 'Yape', 'Plin'];
  formasReembolso = ['Transferencia', 'Efectivo', 'Tarjeta', 'Yape', 'Plin', 'Nota de crédito'];

  monto: number | null = null;
  moneda: Moneda = 'PEN';
  metodo = 'Efectivo';
  codigo = '';
  estadoTx: EstadoTransaccion = 'APROBADO';
  v = new FormValidacion(() => ({
    monto: V.importe(this.monto, 'El monto')
      ?? (this.estadoTx === 'APROBADO' && this.equivalente() > this.saldo() + 0.005
        ? `El pago (S/ ${this.equivalente().toFixed(2)}) excede el saldo pendiente (S/ ${this.saldo().toFixed(2)}).` : null),
    codigo: V.codigo(this.codigo)
  }));

  reserva = computed(() => this.store.getReserva(this.id));
  codReserva = computed(() => this.store.codigoReserva(this.id));
  salida = computed(() => this.store.getSalida(this.reserva()?.salidaId));
  paquete = computed(() => this.store.getPaquete(this.salida()?.paqueteId));
  guia = computed(() => this.store.getGuia(this.salida()?.guiaId));
  // Transporte de la salida: sus operaciones (tramos) vigentes
  operaciones = computed(() => {
    const s = this.salida();
    return s ? this.store.operacionesDe(s.salidaId).filter(o => o.estadoActual !== 'CANCELADO') : [];
  });
  cliente = computed(() => this.store.getCliente(this.reserva()?.clienteId));
  nombreCliente = computed(() => this.store.nombreCliente(this.reserva()));
  emailCliente = computed(() => this.cliente()?.email ?? '');
  telefonoCliente = computed(() => this.cliente()?.telefono ?? '');
  enviandoBoleta = false;
  pasajeros = computed(() => this.store.pasajerosDe(this.id));
  pagos = computed(() => this.store.pagosDe(this.id));
  saldo = computed(() => { const r = this.reserva(); return r ? this.store.saldo(r) : 0; });
  avance = computed(() => { const r = this.reserva(); return r && r.montoTotal ? Math.min(100, r.montoPagado * 100 / r.montoTotal) : 0; });
  puedePagar = computed(() => {
    if (!this.cobra) return false;
    const r = this.reserva();
    return !!r && (r.estadoReserva === 'PENDIENTE' || r.estadoReserva === 'CONFIRMADA') && this.saldo() > 0;
  });
  usuarioAnula = computed(() => this.reserva()?.usuarioAnula ?? 'usuario #' + this.reserva()?.usuarioAnulaId);

  constructor() {
    this.store.cargar(false, error => {
      if (!error && !this.store.getReserva(this.id)) this.router.navigate(['/reservas']);
    });
  }

  tasa() { return TASAS[this.moneda]; }
  equivalente() { return aPEN(this.monto ?? 0, this.moneda); }

  usarSaldo() {
    this.monto = Math.round((this.saldo() / TASAS[this.moneda] + Number.EPSILON) * 100) / 100;
  }

  private ejecutar(peticion: Observable<unknown>, mensaje: string, alTerminar?: () => void) {
    this.store.ocupado.set(true);
    peticion.subscribe({
      next: () => {
        this.store.ocupado.set(false);
        this.store.cargarReservas();
        this.store.cargarPagos();
        this.store.cargarSalidas();
        this.alerts.toast(mensaje);
        if (alTerminar) alTerminar();
      },
      error: (err) => {
        this.store.ocupado.set(false);
        this.alerts.error(this.store.error(err));
      }
    });
  }

  async confirmar() {
    if (await this.alerts.confirm('La reserva pasará a CONFIRMADA.', 'Confirmar reserva'))
      this.ejecutar(this.store.confirmarReserva(this.id), 'Reserva confirmada');
  }

  async completar() {
    if (await this.alerts.confirm('La reserva pasará a COMPLETADA.', 'Completar reserva'))
      this.ejecutar(this.store.completarReserva(this.id), 'Reserva completada');
  }

  async anular() {
    const r = this.reserva();
    if (!r) return;
    let forma: string | null = null;
    if (r.montoPagado > 0) {
      const sel = await this.alerts.select('Anular reserva',
        `Se reembolsarán S/ ${r.montoPagado.toFixed(2)} ya pagados. ¿Cómo se devolverá el dinero?`, this.formasReembolso);
      if (!sel) return;
      forma = sel;
    } else if (!(await this.alerts.confirm('Se liberarán los cupos de la salida.', 'Anular reserva', 'Anular'))) {
      return;
    }
    this.ejecutar(this.store.anularReserva(this.id, forma), 'Reserva anulada');
  }

  // La boleta se pide con el token de la sesión y se abre en una pestaña nueva
  verBoleta() {
    const ventana = window.open('', '_blank');
    this.store.verBoleta(this.id).subscribe({
      next: html => {
        if (!ventana) return;
        ventana.document.open();
        ventana.document.write(html);
        ventana.document.close();
      },
      error: err => { ventana?.close(); this.alerts.error(this.store.error(err)); }
    });
  }

  async enviarBoleta() {
    if (this.enviandoBoleta) return;
    const email = this.emailCliente();
    if (!email) { this.alerts.error('El cliente no tiene correo registrado.'); return; }
    if (!(await this.alerts.confirm(`Se enviará la boleta a ${email}.`, 'Enviar boleta', 'Enviar'))) return;
    this.enviandoBoleta = true;
    this.store.enviarBoleta(this.id).subscribe({
      next: (res) => {
        this.enviandoBoleta = false;
        this.store.cargarReservas();
        this.alerts.toast('Boleta enviada a ' + res.destinatario);
      },
      error: (err) => {
        this.enviandoBoleta = false;
        this.alerts.error(this.store.error(err));
      }
    });
  }

  registrarPago() {
    if (this.store.ocupado()) return;
    if (!this.v.valido()) { this.alerts.toast('Revisa los campos marcados en rojo', 'error'); return; }
    const peticion = this.store.registrarPago(this.id, this.monto!, this.moneda, this.metodo, this.codigo.trim(), this.estadoTx);
    this.ejecutar(peticion, 'Pago registrado', () => {
      this.monto = null;
      this.codigo = '';
      this.estadoTx = 'APROBADO';
      this.v.reiniciar();
    });
  }

  resolver(pagoId: number, estado: 'APROBADO' | 'RECHAZADO') {
    if (this.store.ocupado()) return;
    this.ejecutar(this.store.resolverPago(pagoId, estado), estado === 'APROBADO' ? 'Pago aprobado' : 'Pago rechazado');
  }

  filtrar(evento: Event, tipo: string) { filtrarEscritura(evento, tipo); }
  pegar(evento: Event, tipo: string) { filtrarPegado(evento, tipo); }
  rueda(evento: Event) { sinRueda(evento); }
}
