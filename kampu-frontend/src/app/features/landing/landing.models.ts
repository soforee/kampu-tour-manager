import { PaqueteTour } from '../../core/models';

export type TipoTour = '' | 'FULL_DAY' | 'ESCAPADA' | 'EXPEDICION';

export interface FiltroBusqueda {
  destino: string;
  fecha: string;
  tipo: TipoTour;
}

export interface TourCard {
  paquete: PaqueteTour;
  gradiente: string;
  duracion: string;
  proximaSalida: string | null;
}

export interface Contacto {
  email: string;
  telefono: string;
  ciudad: string;
}
