import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Pago, PortalDetalle, PortalPaquete } from '../models';
import { API } from '../utils/api';

// Portal del cliente: VW_PAQUETES_CLIENTE y PKG_GESTION_PAQUETES_CLIENTE (vía backend)
@Injectable({ providedIn: 'root' })
export class PortalService {
  private http = inject(HttpClient);

  misPaquetes(): Observable<PortalPaquete[]> {
    return this.http.get<PortalPaquete[]>(`${API}/portal/paquetes`);
  }

  detalle(reservaId: number): Observable<PortalDetalle> {
    return this.http.get<PortalDetalle>(`${API}/portal/paquetes/${reservaId}`);
  }

  informarPago(reservaId: number, monto: number, metodoPago: string, codigoTransaccion: string): Observable<Pago> {
    return this.http.post<Pago>(`${API}/portal/paquetes/${reservaId}/pagos`,
      { monto: monto, metodoPago: metodoPago, codigoTransaccion: codigoTransaccion });
  }
}
