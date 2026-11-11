export type EstadoSalida = 'PROGRAMADA' | 'CONFIRMADA' | 'COMPLETADA' | 'CANCELADA';
export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'ANULADA' | 'COMPLETADA';
export type EstadoPagoReserva = 'PENDIENTE' | 'PARCIAL' | 'PAGADO' | 'REEMBOLSADO';
export type EstadoTransaccion = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'REEMBOLSADO';
export type TipoDocumento = 'DNI' | 'CE' | 'PASAPORTE';
// Los clientes también pueden ser empresas (RUC); los pasajeros no
export type TipoDocumentoCliente = TipoDocumento | 'RUC';
export type Moneda = 'PEN' | 'USD' | 'EUR';
// chk_usu_rol (personal interno) + CLIENTE (portal web)
export type Rol = 'ADMIN' | 'VENDEDOR' | 'OPERADOR' | 'CLIENTE';
export type EstadoRegistro = 'ACTIVO' | 'INACTIVO';
export type CategoriaLicencia = 'A-I' | 'A-IIa' | 'A-IIb' | 'A-IIIa' | 'A-IIIb' | 'A-IIIc';
export type EstadoProveedor = 'ACTIVO' | 'INACTIVO' | 'SUSPENDIDO';
export type TipoLugar = 'AEROPUERTO' | 'TERMINAL' | 'HOTEL' | 'ATRACTIVO' | 'CIUDAD' | 'OTRO';
export type TipoVehiculo = 'AUTO' | 'CAMIONETA_4X4' | 'VAN' | 'MINIBUS' | 'BUS';
export type EstadoVehiculo = 'OPERATIVO' | 'MANTENIMIENTO' | 'INACTIVO';
export type RolPersonal = 'GUIA' | 'CONDUCTOR' | 'AMBOS';
export type EstadoPersonal = 'DISPONIBLE' | 'OCUPADO' | 'INACTIVO';
// Estados de tráfico v4 (EstadoOperacion.java)
export type EstadoOperacion = 'PROGRAMADO' | 'ASIGNADO' | 'EMBARQUE' | 'EN_RUTA' | 'FINALIZADO' | 'CANCELADO';
// Portal del cliente (VW_PAQUETES_CLIENTE)
export type EstadoEjecucion = 'POR_EMPEZAR' | 'EN_CURSO' | 'TERMINADO' | 'CANCELADO';
export type EstadoPagoPaquete = 'PAGADO_COMPLETO' | 'FALTA_PAGAR' | 'PENDIENTE_PAGO' | 'ANULADO';

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
  // Lugar al que llega el paquete (02_extension_kampu.sql)
  lugarDestinoId: number | null;
}


// ---------- Proveedores (v4): Kampu Tours (propio) y terceros ----------
export interface Proveedor {
  proveedorId: number;
  nombreComercial: string;
  ruc: string;
  telefono: string;
  esPropio: boolean;
  estado: EstadoProveedor;
  vehiculos: number;
  capacidadOperativa: number;
  conductores: number;
  guias: number;
}

// Guías y conductores (un registro puede ser AMBOS)
export interface Personal {
  personalId: number;
  proveedorId: number;
  nombreCompleto: string;
  rol: RolPersonal;
  telefono: string;
  numLicencia: string | null;
  catLicencia: CategoriaLicencia | null;
  vencLicencia: string | null;
  estado: EstadoPersonal;
}

export interface Lugar {
  lugarId: number;
  nombre: string;
  tipoLugar: TipoLugar;
  ciudad: string;
  // Coordenadas reales: con ellas se calcula la duración de las rutas
  latitud: number | null;
  longitud: number | null;
}

export interface Vehiculo {
  vehiculoId: number;
  proveedorId: number;
  placa: string;
  tipoVehiculo: TipoVehiculo;
  marcaModelo: string;
  capacidadPax: number;
  proxMantenimiento: string | null;
  estado: EstadoVehiculo;
}

// Duración calculada con datos reales: OSRM (OpenStreetMap) o ESTIMADO (coordenadas)
export interface EstimacionDuracion {
  minutos: number | null;
  distanciaKm: number | null;
  fuente: 'OSRM' | 'ESTIMADO' | 'SIN_COORDENADAS';
  detalle: string;
}

// ---------- Tráfico (OPERACIONES_TRANSPORTE) ----------
export interface Operacion {
  operacionId: number;
  codigoOperacion: string;
  salidaId: number | null;
  origenId: number;
  destinoId: number;
  vehiculoId: number | null;
  conductorId: number | null;
  paxProgramados: number;
  horaProgramadaSalida: string;
  horaEstimadaLlegada: string;
  estadoActual: EstadoOperacion;
  observaciones: string;
  origen: string;
  destino: string;
  proveedorId: number | null;
  proveedor: string | null;
  proveedorPropio: boolean;
  placa: string | null;
  conductor: string | null;
  estadoNombre: string;
  ordenEstado: number;
  horaRealSalida: string | null;
  horaRealLlegada: string | null;
  minRetrasoSalida: number | null;
  minRetrasoLlegada: number | null;
}

export interface EstadoOperacionInfo {
  codigo: EstadoOperacion;
  nombre: string;
  orden: number;
  requiereRecursos: boolean;
  ocupaRecursos: boolean;
  esFinal: boolean;
  siguientes: EstadoOperacion[];
}

export interface HistorialOperacion {
  historialId: number;
  operacionId: number;
  estadoAnterior: EstadoOperacion | null;
  estadoNuevo: EstadoOperacion;
  fechaCambio: string;
  usuarioRegistro: string;
  observacion: string;
}

// Vehículo o conductor posible para una operación (motivo = null: elegible)
export interface RecursoCandidato {
  tipoRecurso: 'VEHICULO' | 'CONDUCTOR';
  recursoId: number;
  proveedorId: number;
  proveedor: string;
  proveedorPropio: boolean;
  descripcion: string;
  capacidadPax: number | null;
  horasDia: number | null;
  motivo: string | null;
}

export interface Cliente {
  clienteId: number;
  nombreCompleto: string;
  tipoDocumento: TipoDocumentoCliente;
  numeroDocumento: string;
  email: string;
  telefono: string;
  cuentaWeb: boolean;       // ya se registró en el portal
  estadoCuenta: string;
  fechaRegistro: string | null;
}

export interface SalidaProgramada {
  salidaId: number;
  paqueteId: number;
  fechaInicio: string;
  fechaFin: string;
  cuposMaximos: number;
  cuposDisponibles: number;
  guiaId?: number | null;
  estado: EstadoSalida;
  // Operaciones de transporte vigentes y PKG_ASIGNACION.faltante_transporte (null = alcanza)
  operaciones: number;
  faltanteTransporte: string | null;
}

// Cabecera + detalle de la salida reservada (DETALLE_RESERVA)
export interface Reserva {
  reservaId: number;
  clienteId: number | null;
  detalleId: number;
  salidaId: number;
  fechaReserva: string;
  numeroViajeros: number;
  precioUnitario: number;
  descuento: number;
  montoTotal: number;
  montoPagado: number;
  saldo: number;
  estadoReserva: EstadoReserva;
  estadoPago: EstadoPagoReserva;
  observaciones: string;
  formaReembolso?: string;
  usuarioAnulaId?: number;
  usuarioAnula?: string | null;
  boletaEnviadaEn?: string | null;
}

// Cliente nuevo que se registra al reservar (v4: toda reserva tiene cliente)
export interface ContactoReserva {
  nombre: string;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  email: string;
  telefono: string;
}

export interface PasajeroReserva {
  pasajeroId: number;
  detalleId: number;
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
  tipoCambio: number;
  montoPen: number;
  metodoPago: string;
  codigoTransaccion: string;
  fechaPago: string;
  estadoTransaccion: EstadoTransaccion;
  registradoPor: 'ADMIN' | 'VENDEDOR' | 'CLIENTE';
}

// ---------- Portal del cliente ----------
export interface PortalPaquete {
  reservaId: number;
  salidaId: number;
  paqueteId: number;
  paqueteNombre: string;
  imagen: string;
  dias: number;
  noches: number;
  fechaInicio: string;
  fechaFin: string;
  pasajeros: number;
  fechaReserva: string;
  estadoReserva: EstadoReserva;
  estadoEjecucion: EstadoEjecucion;
  estadoPago: EstadoPagoPaquete;
  montoTotal: number;
  montoPagado: number;
  saldo: number;
  montoPorValidar: number;
}

export interface PortalDetalle {
  paquete: PortalPaquete;
  descripcion: string;
  guia: string | null;
  pasajeros: { nombreCompleto: string; tipoDocumento: string; numeroDocumento: string; nacionalidad: string }[];
  pagos: Pago[];
}

// VW_RESUMEN_PAQUETES_POR_ROL (montos nulos para el Operador)
export interface ResumenPaquete {
  salidaId: number;
  paqueteId: number;
  paqueteNombre: string;
  fechaInicio: string;
  fechaFin: string;
  estadoSalida: EstadoSalida;
  estadoEjecucion: EstadoEjecucion;
  cuposMaximos: number;
  cuposDisponibles: number;
  pasajeros: number;
  reservasActivas: number;
  operaciones: number;
  guia: string | null;
  montoVendido: number | null;
  montoCobrado: number | null;
}

export interface RegistroForm {
  nombreCompleto: string;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  email: string;
  telefono: string;
  password: string;
  confirmacion: string;
}

export interface PaqueteForm {
  nombre: string;
  descripcion: string;
  precioBase: number;
  dias: number;
  noches: number;
  imagen: string;
  estado: EstadoRegistro;
  lugarDestinoId: number | null;
}


export interface ProveedorForm {
  nombreComercial: string;
  ruc: string;
  telefono: string;
  esPropio: boolean;
  estado: EstadoProveedor;
}

export interface PersonalForm {
  proveedorId: number | null;
  nombreCompleto: string;
  rol: RolPersonal;
  telefono: string;
  numLicencia: string;
  catLicencia: CategoriaLicencia | '';
  vencLicencia: string;
  estado: EstadoPersonal;
}

export interface LugarForm {
  nombre: string;
  tipoLugar: TipoLugar;
  ciudad: string;
  latitud: number | null;
  longitud: number | null;
}

export interface VehiculoForm {
  proveedorId: number | null;
  placa: string;
  tipoVehiculo: TipoVehiculo;
  marcaModelo: string;
  capacidadPax: number | null;
  proxMantenimiento: string;
  estado: EstadoVehiculo;
}

export interface OperacionForm {
  salidaId: number | null;
  origenId: number | null;
  destinoId: number | null;
  vehiculoId: number | null;
  conductorId: number | null;
  paxProgramados: number | null;
  horaProgramadaSalida: string;      // 'YYYY-MM-DDTHH:mm'
  horaEstimadaLlegada: string | null;
  observaciones: string;
}

export interface ClienteForm {
  nombreCompleto: string;
  tipoDocumento: TipoDocumentoCliente;
  numeroDocumento: string;
  email: string;
  telefono: string;
}

// La fecha fin la calcula la base de datos (trg_salida_biu) con los días del paquete
export interface SalidaForm {
  paqueteId: number;
  fechaInicio: string;
  cuposMaximos: number;
  guiaId?: number | null;
  estado: EstadoSalida;
}

export interface PasajeroDraft {
  nombreCompleto: string;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nacionalidad: string;
  fechaNacimiento: string;
}

// Personal interno (usuarioId) o cliente del portal (clienteId). token: "Authorization: Bearer"
export interface Sesion {
  usuarioId: number | null;
  clienteId: number | null;
  username: string;
  nombre: string;
  rol: Rol;
  estado: string;
  token: string;
  // Acciones permitidas por PKG_SEGURIDAD_ROLES (VER_LOGISTICA, GESTIONAR_RESERVAS, ...)
  permisos: Permiso[];
}

export type Permiso =
  | 'VER_USUARIOS' | 'GESTIONAR_USUARIOS' | 'VER_AUDITORIA'
  | 'VER_CLIENTES' | 'GESTIONAR_CLIENTES' | 'VER_RESERVAS' | 'GESTIONAR_RESERVAS' | 'VER_MONTOS' | 'REGISTRAR_PAGO'
  | 'GESTIONAR_CATALOGO' | 'ACTUALIZAR_EJECUCION' | 'VER_RESUMEN'
  | 'VER_LOGISTICA' | 'GESTIONAR_LOGISTICA' | 'VER_OPERACIONES' | 'GESTIONAR_OPERACIONES'
  | 'VER_PAQUETES_PROPIOS' | 'INFORMAR_PAGO';

// ---------- Auditoría (solo ADMIN) ----------
export type AccionAuditoria = 'CREAR' | 'ACTUALIZAR' | 'ELIMINAR' | 'REGISTRO' | 'LOGIN' | 'LOGOUT' | 'LOGIN_FALLIDO';

export interface RegistroAuditoria {
  auditoriaId: number;
  accion: AccionAuditoria;
  entidad: string;
  entidadId: number | null;
  campo: string | null;
  valorAnterior: string | null;
  valorNuevo: string | null;
  detalle: string | null;
  usuario: string;
  rolUsuario: Rol | 'SISTEMA';
  origen: 'BD' | 'APP';
  fecha: string;
}

export interface FiltroAuditoria {
  desde: string;
  hasta: string;
  rol: string;
  accion: string;
  entidad: string;
  usuario: string;
  q: string;
  entidadId?: number | null;
}

export interface Pagina<T> {
  contenido: T[];
  total: number;
  pagina: number;
  tamanio: number;
}
