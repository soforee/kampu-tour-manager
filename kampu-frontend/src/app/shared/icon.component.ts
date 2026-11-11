import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-icon',
  standalone: true,
  templateUrl: './icon.component.html'
})
export class IconComponent {
  @Input({ required: true }) name = '';
  @Input() size = 'w-5 h-5';
  @Input() trazo = 1.8;
}
