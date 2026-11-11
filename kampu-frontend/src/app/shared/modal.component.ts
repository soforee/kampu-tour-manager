import { Component, EventEmitter, Input, Output } from '@angular/core';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './modal.component.html'
})
export class ModalComponent {
  @Input({ required: true }) titulo = '';
  @Output() cerrar = new EventEmitter<void>();
}
