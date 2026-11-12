// Textos para los códigos de los CHECK de la base de datos
export const ETIQUETAS: { [codigo: string]: string } = {
  // Proveedores, personal y vehículos
  SUSPENDIDO: 'Suspendido', DISPONIBLE: 'Disponible', OCUPADO: 'Ocupado', INACTIVO: 'Inactivo',
  OPERATIVO: 'Operativo', MANTENIMIENTO: 'En mantenimiento',
  GUIA: 'Guía', CONDUCTOR: 'Conductor', AMBOS: 'Guía y conductor',
  // Lugares
  AEROPUERTO: 'Aeropuerto', TERMINAL: 'Terminal', HOTEL: 'Hotel', ATRACTIVO: 'Atractivo', CIUDAD: 'Ciudad', OTRO: 'Otro',
  // Tipos de vehículo
  AUTO: 'Auto', CAMIONETA_4X4: 'Camioneta 4x4', VAN: 'Van', MINIBUS: 'Minibús', BUS: 'Bus',
  // Operaciones de transporte (EstadoOperacion del backend)
  PROGRAMADO: 'Programado', ASIGNADO: 'Asignado', EMBARQUE: 'Embarque', EN_RUTA: 'En ruta', FINALIZADO: 'Finalizado', CANCELADO: 'Cancelado',
  // Portal del cliente
  POR_EMPEZAR: 'Por empezar', EN_CURSO: 'En curso', TERMINADO: 'Terminado',
  PAGADO_COMPLETO: 'Pagado completo', FALTA_PAGAR: 'Falta pagar', PENDIENTE_PAGO: 'Pendiente de pago', ANULADO: 'Anulado',
  // Roles
  ADMIN: 'Administrador', VENDEDOR: 'Ventas', OPERADOR: 'Operador', CLIENTE: 'Cliente'
};

export function etiqueta(codigo?: string | null): string {
  if (!codigo) return '';
  return ETIQUETAS[codigo] ?? codigo.charAt(0) + codigo.slice(1).toLowerCase().replace(/_/g, ' ');
}

// Minutos -> "1 h 30 min"
export function duracion(minutos?: number | null): string {
  if (minutos === null || minutos === undefined) return '';
  const h = Math.floor(minutos / 60);
  const m = minutos % 60;
  return h ? (m ? `${h} h ${m} min` : `${h} h`) : `${m} min`;
}
