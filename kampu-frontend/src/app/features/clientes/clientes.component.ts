import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TourStore } from '../../core/services/tour-store.service';
import { AlertService } from '../../core/services/alert.service';
import { ClienteForm, TipoDocumento } from '../../core/models';
import { IconComponent } from '../../shared/icon.component';
import { filtrarEscritura, filtrarPegado } from '../../core/utils/filtro';
import { FormValidacion, V } from '../../core/utils/validacion';
import { ModalComponent } from '../../shared/modal.component';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [FormsModule, IconComponent, ModalComponent],
  templateUrl: './clientes.component.html',
  styleUrl: './clientes.component.css'
})
export class ClientesComponent {
  store = inject(TourStore);
  private alerts = inject(AlertService);

  tipos: TipoDocumento[] = ['DNI', 'CE', 'PASAPORTE'];
  texto = signal('');
  abierto = signal(false);
  editId: number | null = null;
  form: ClienteForm = this.vacio();
  v = new FormValidacion(() => ({
    nombre: V.nombrePersona(this.form.nombreCompleto),
    documento: V.documento(this.form.tipoDocumento, this.form.numeroDocumento),
    email: V.email(this.form.email),
    telefono: V.telefono(this.form.telefono)
  }));

  private vacio(): ClienteForm {
    return { nombreCompleto: '', tipoDocumento: 'DNI' as TipoDocumento, numeroDocumento: '', email: '', telefono: '' };
  }

  filas = computed(() => {
    const t = this.texto().toLowerCase().trim();
    return this.store.clientes()
      .filter(c => !t || c.nombreCompleto.toLowerCase().includes(t) || c.numeroDocumento.includes(t))
      .map(c => ({ cliente: c, reservas: this.store.reservas().filter(r => r.clienteId === c.clienteId).length }));
  });

  nuevo() { this.editId = null; this.form = this.vacio(); this.v.reiniciar(); this.abierto.set(true); }

  editar(id: number) {
    const c = this.store.getCliente(id)!;
    this.editId = c.clienteId;
    this.form = { nombreCompleto: c.nombreCompleto, tipoDocumento: c.tipoDocumento, numeroDocumento: c.numeroDocumento, email: c.email, telefono: c.telefono };
    this.v.reiniciar();
    this.abierto.set(true);
  }

  guardar() {
    if (this.store.ocupado()) return;
    if (!this.v.valido()) { this.alerts.toast('Revisa los campos marcados en rojo', 'error'); return; }
    const f = this.form;
    const datos: ClienteForm = {
      nombreCompleto: f.nombreCompleto.trim(), tipoDocumento: f.tipoDocumento, numeroDocumento: f.numeroDocumento.trim(),
      email: f.email.trim(), telefono: f.telefono.trim()
    };
    const editando = this.editId !== null;
    this.store.ocupado.set(true);
    this.store.guardarCliente(datos, this.editId).subscribe({
      next: () => {
        this.store.ocupado.set(false);
        this.store.cargarClientes();
        this.abierto.set(false);
        this.alerts.toast(editando ? 'Cliente actualizado' : 'Cliente registrado');
      },
      error: (err) => {
        this.store.ocupado.set(false);
        this.alerts.error(this.store.error(err));
      }
    });
  }

  async eliminar(id: number) {
    if (!(await this.alerts.confirm('Esta acción no se puede deshacer.', 'Eliminar cliente'))) return;
    this.store.eliminarCliente(id).subscribe({
      next: () => {
        this.store.cargarClientes();
        this.alerts.toast('Cliente eliminado');
      },
      error: (err) => this.alerts.error(this.store.error(err))
    });
  }

  filtrar(evento: Event, tipo: string) { filtrarEscritura(evento, tipo); }
  pegar(evento: Event, tipo: string) { filtrarPegado(evento, tipo); }
}
