import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { OrdersService } from '../../../core/orders.service';
import { Order, OrderStatus } from '../../../core/models/order.model';
import { OrderStatusBadgeComponent } from '../../../shared/order-status-badge/order-status-badge.component';

const STATUS_OPTIONS: OrderStatus[] = ['Pendiente', 'En proceso', 'Enviado', 'Completado', 'Cancelado'];

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, OrderStatusBadgeComponent],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.scss'
})
export class OrderDetailComponent implements OnInit {
  order: Order | undefined;
  loading  = true;
  error    = '';
  saving   = false;
  saveOk   = false;

  selectedStatus: OrderStatus = 'Pendiente';
  readonly statusOptions = STATUS_OPTIONS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private ordersService: OrdersService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id || isNaN(id)) {
      this.error = 'ID de pedido no válido.';
      this.loading = false;
      return;
    }

    this.ordersService.getOrderById(id).subscribe({
      next: order => {
        this.order = order;
        if (order) this.selectedStatus = order.estado;
        this.loading = false;
      },
      error: err => {
        this.error = `Error al cargar el pedido: ${err.message || err.statusText}`;
        this.loading = false;
      }
    });
  }

  isAdmin():    boolean { return this.authService.hasRole('Admin'); }
  isOperator(): boolean { return this.authService.hasRole('Operator'); }
  get canEditStatus(): boolean { return this.isAdmin() || this.isOperator(); }

  updateStatus(): void {
    if (!this.order || !this.canEditStatus) return;
    this.saving  = true;
    this.saveOk  = false;

    this.ordersService.updateOrderStatus(this.order.id, this.selectedStatus).subscribe({
      next: updated => {
        this.order = updated;
        this.saving = false;
        this.saveOk = true;
        setTimeout(() => this.saveOk = false, 3000);
      },
      error: err => {
        this.error  = `Error al actualizar: ${err.message || err.statusText}`;
        this.saving = false;
      }
    });
  }

  goBack(): void { this.router.navigate(['/orders']); }

  formatCLP(value: number): string {
    return new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 0 }).format(value);
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString('es-CL', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
