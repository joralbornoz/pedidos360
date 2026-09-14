import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { OrdersService } from '../../../core/orders.service';
import { Order, OrderStatus } from '../../../core/models/order.model';
import { OrderStatusBadgeComponent } from '../../../shared/order-status-badge/order-status-badge.component';

const ALL_STATUSES: OrderStatus[] = ['Pendiente', 'En proceso', 'Enviado', 'Completado', 'Cancelado'];

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, OrderStatusBadgeComponent],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.scss'
})
export class OrderListComponent implements OnInit {
  allOrders: Order[] = [];
  filteredOrders: Order[] = [];

  searchText    = '';
  filterStatus  = '';
  filterDate    = '';

  loading = true;
  error   = '';

  readonly statuses = ALL_STATUSES;

  constructor(
    private ordersService: OrdersService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  isAdmin():    boolean { return this.authService.hasRole('Admin'); }
  isOperator(): boolean { return this.authService.hasRole('Operator'); }
  isCustomer(): boolean { return this.authService.hasRole('Customer'); }

  get canCreate(): boolean { return this.isCustomer() || this.isOperator(); }
  get canChangeStatus(): boolean { return this.isAdmin() || this.isOperator(); }
  get showClientFilter(): boolean { return this.isAdmin() || this.isOperator(); }

  get pageTitle(): string {
    if (this.isAdmin())    return 'Todos los pedidos';
    if (this.isOperator()) return 'Gestión de pedidos';
    return 'Mis pedidos';
  }

  loadOrders(): void {
    this.loading = true;
    this.error   = '';
    const obs = this.isCustomer()
      ? this.ordersService.getOrdersByCustomer('user-001') // Mock: ID estático
      : this.ordersService.getOrders();

    obs.subscribe({
      next: orders => {
        this.allOrders = orders;
        this.applyFilters();
        this.loading = false;
      },
      error: err => {
        this.error = `Error al cargar pedidos: ${err.message || err.statusText}`;
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let result = [...this.allOrders];

    if (this.searchText.trim()) {
      const q = this.searchText.toLowerCase();
      result = result.filter(o =>
        o.numero.toLowerCase().includes(q) ||
        o.cliente.toLowerCase().includes(q) ||
        o.clienteEmail.toLowerCase().includes(q)
      );
    }

    if (this.filterStatus) {
      result = result.filter(o => o.estado === this.filterStatus);
    }

    if (this.filterDate) {
      result = result.filter(o => o.fecha.startsWith(this.filterDate));
    }

    this.filteredOrders = result;
  }

  clearFilters(): void {
    this.searchText   = '';
    this.filterStatus = '';
    this.filterDate   = '';
    this.applyFilters();
  }

  goToDetail(id: number): void {
    this.router.navigate(['/orders', id]);
  }

  createOrder(): void {
    this.router.navigate(['/orders', 'new']);
  }

  formatCLP(value: number): string {
    return new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 0 }).format(value);
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('es-CL', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
