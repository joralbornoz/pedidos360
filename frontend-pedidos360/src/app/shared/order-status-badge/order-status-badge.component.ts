import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderStatus } from '../../core/models/order.model';

const STATUS_CONFIG: Record<OrderStatus, { label: string; cssClass: string; icon: string }> = {
  'Pendiente':   { label: 'Pendiente',   cssClass: 'badge-warning', icon: '⏳' },
  'En proceso':  { label: 'En proceso',  cssClass: 'badge-info',    icon: '🔄' },
  'Enviado':     { label: 'Enviado',     cssClass: 'badge-primary', icon: '🚚' },
  'Completado':  { label: 'Completado',  cssClass: 'badge-success', icon: '✅' },
  'Cancelado':   { label: 'Cancelado',   cssClass: 'badge-danger',  icon: '✕' },
};

@Component({
  selector: 'app-order-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="badge {{ config.cssClass }}">
      {{ config.label }}
    </span>
  `,
  styles: [`
    :host { display: inline-block; }
  `]
})
export class OrderStatusBadgeComponent {
  @Input({ required: true }) status!: OrderStatus;

  get config() {
    return STATUS_CONFIG[this.status] ?? { label: this.status, cssClass: 'badge-neutral', icon: '•' };
  }
}
