import { Injectable } from '@angular/core';
import Swal from 'sweetalert2';

const BRAND = '#4B5B1D';
const DANGER = '#E11D48';
const CANCEL = '#FFFEF8';

@Injectable({ providedIn: 'root' })
export class AlertService {
  success(message: string, title: string = '¡Listo!') {
    Swal.fire({ title, text: message, icon: 'success', confirmButtonColor: BRAND });
  }

  error(message: string, title: string = 'No se pudo completar') {
    Swal.fire({ title, text: message, icon: 'error', confirmButtonColor: DANGER });
  }

  warning(message: string, title: string = 'Atención') {
    Swal.fire({ title, text: message, icon: 'warning', confirmButtonColor: BRAND });
  }

  async confirm(message: string, title: string = '¿Estás seguro?', confirmText: string = 'Sí, continuar'): Promise<boolean> {
    const result = await Swal.fire({
      title, text: message, icon: 'warning', showCancelButton: true,
      confirmButtonColor: BRAND, cancelButtonColor: CANCEL, reverseButtons: true,
      confirmButtonText: confirmText, cancelButtonText: 'Cancelar'
    });
    return result.isConfirmed;
  }

  async select(title: string, text: string, options: string[]): Promise<string | null> {
    const inputOptions: { [opcion: string]: string } = {};
    options.forEach(o => (inputOptions[o] = o));
    const result = await Swal.fire({
      title, text, input: 'select', inputOptions, inputPlaceholder: 'Selecciona una opción',
      showCancelButton: true, confirmButtonColor: DANGER, cancelButtonColor: CANCEL, reverseButtons: true,
      confirmButtonText: 'Anular reserva', cancelButtonText: 'Volver',
      inputValidator: v => (v ? null : 'Elige una opción')
    });
    return result.isConfirmed ? (result.value as string) : null;
  }

  toast(message: string, icon: 'success' | 'error' | 'info' = 'success') {
    Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 2500, timerProgressBar: true })
      .fire({ icon, title: message });
  }
}
