
export type EstadoSalida = 'PROGRAMADA' | 'CONFIRMADA' | 'COMPLETADA' | 'CANCELADA';
export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'ANULADA' | 'COMPLETADA';
export type EstadoPagoReserva = 'PENDIENTE' | 'PARCIAL' | 'PAGADO' | 'REEMBOLSADO';
export type EstadoTransaccion = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'REEMBOLSADO';
export type TipoDocumento = 'DNI' | 'CE' | 'PASAPORTE';
export type Moneda = 'PEN' | 'USD' | 'EUR';
export type Rol = 'ADMIN' | 'VENTAS';
export type EstadoRegistro = 'ACTIVO' | 'INACTIVO';

export interface UsuarioSistema {
  usuarioId: number;
  username: string;
  passwordHash: string;
  rol: Rol;
  estado: EstadoRegistro;
}

export interface PaqueteTour {
  paqueteId: number;
  nombre: string;
  descripcion: string;
  precioBase: number;
  dias: number;
  noches: number;
  imagen: string;
  estado: EstadoRegistro;
}

export interface Guia {
  guiaId: number;
  nombreCompleto: string;
  especialidad: string;
  telefono: string;
}

export interface Transporte {
  transporteId: number;
  placa: string;
  marcaModelo: string;
  capacidad: number;
}

export interface Cliente {
  clienteId: number;
  nombreCompleto: string;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  email: string;
  telefono: string;
}

export interface SalidaProgramada {
  salidaId: number;
  paqueteId: number;
  fechaInicio: string;
  fechaFin: string;
  cuposMaximos: number;
  cuposDisponibles: number;
  guiaId?: number | null;
  transporteId?: number | null;
  estado: EstadoSalida;
}

export interface Reserva {
  reservaId: number;
  clienteId: number;
  salidaId: number;
  fechaReserva: string;
  numeroViajeros: number;
  montoTotal: number;
  montoPagado: number;
  estadoReserva: EstadoReserva;
  estadoPago: EstadoPagoReserva;
  observaciones: string;
  formaReembolso?: string;
  usuarioAnulaId?: number;
}

export interface PasajeroReserva {
  pasajeroId: number;
  reservaId: number;
  nombreCompleto: string;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nacionalidad: string;
  fechaNacimiento: string;
}

export interface Pago {
  pagoId: number;
  reservaId: number;
  monto: number;
  moneda: Moneda;
  metodoPago: string;
  codigoTransaccion: string;
  fechaPago: string;
  estadoTransaccion: EstadoTransaccion;
}

export interface PaqueteForm {
  nombre: string;
  descripcion: string;
  precioBase: number;
  dias: number;
  noches: number;
  imagen: string;
  estado: EstadoRegistro;
}

export interface GuiaForm {
  nombreCompleto: string;
  especialidad: string;
  telefono: string;
}

export interface TransporteForm {
  placa: string;
  marcaModelo: string;
  capacidad: number;
}

export interface ClienteForm {
  nombreCompleto: string;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  email: string;
  telefono: string;
}

export interface SalidaForm {
  paqueteId: number;
  fechaInicio: string;
  fechaFin: string;
  cuposMaximos: number;
  guiaId?: number | null;
  transporteId?: number | null;
  estado: EstadoSalida;
}

export interface PasajeroDraft {
  nombreCompleto: string;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nacionalidad: string;
  fechaNacimiento: string;
}

export interface Sesion {
  usuarioId: number;
  username: string;
  rol: Rol;
  estado: EstadoRegistro;
}

export interface SesionGuardada {
  token: string;
  usuario: Sesion;
}
