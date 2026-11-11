// Textos para los códigos de los CHECK de la v3
export const ETIQUETAS: { [codigo: string]: string } = {
  // Empresa
  PENDIENTE: 'Pendiente', ACTIVA: 'Activa', SUSPENDIDA: 'Suspendida', RETIRADA: 'Retirada',
  DISPONIBLE: 'Disponible', LIMITADA: 'Limitada', NO_DISPONIBLE: 'No disponible',
  // Lugares
  AEROPUERTO: 'Aeropuerto', TERMINAL: 'Terminal', HOTEL: 'Hotel', ATRACTIVO: 'Atractivo', CIUDAD: 'Ciudad', OTRO: 'Otro',
  // Rutas
  TRASLADO: 'Traslado', TOUR: 'Tour', INTERPROVINCIAL: 'Interprovincial', INACTIVA: 'Inactiva',
  // Vehículos
  AUTO: 'Auto', CAMIONETA_4X4: 'Camioneta 4x4', VAN: 'Van', MINIBUS: 'Minibús', BUS: 'Bus',
  OPERATIVO: 'Operativo', EN_MANTENIMIENTO: 'En mantenimiento', FUERA_DE_SERVICIO: 'Fuera de servicio',
  // Conductores
  DESCANSO: 'Descanso', VACACIONES: 'Vacaciones', INACTIVO: 'Inactivo',
  // Operaciones (DOMINIO_ESTADOS)
  PROGRAMADO: 'Programado', ASIGNADO: 'Asignado', EMBARQUE: 'Embarque', EN_ESPERA_SALIDA: 'En espera de salida',
  EN_RUTA: 'En ruta', LLEGADA_PROXIMA: 'Llegada próxima', FINALIZADO: 'Finalizado', RETRASADO: 'Retrasado', CANCELADO: 'Cancelado',
  // Portal del cliente (v4)
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
