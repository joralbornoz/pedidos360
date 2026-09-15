import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { OrdersService } from '../../core/orders.service';
import { TopbarComponent } from '../../shared/topbar/topbar.component';
import { OrderStatusBadgeComponent } from '../../shared/order-status-badge/order-status-badge.component';
import { Order } from '../../core/models/order.model';

interface KpiCard {
  icon: string;
  label: string;
  value: string | number;
  change?: string;
  positive?: boolean;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, TopbarComponent, OrderStatusBadgeComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  userName = '';
  userRoles: string[] = [];

  // Admin KPIs
  adminKpis: KpiCard[] = [];

  // Operator data
  operatorEnProceso: Order[] = [];
  operatorPendientes: Order[] = [];

  // Customer data
  customerOrders: Order[] = [];

  constructor(
    private authService: AuthService,
    private ordersService: OrdersService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userName = this.authService.getActiveAccountName();
    this.userRoles = this.authService.getUserRoles();

    if (this.isAdmin()) this.loadAdminData();
    if (this.isOperator()) this.loadOperatorData();
    if (this.isCustomer()) this.loadCustomerData();
  }

  isAdmin():    boolean { return this.authService.hasRole('Admin'); }
  isOperator(): boolean { return this.authService.hasRole('Operator'); }
  isCustomer(): boolean { return this.authService.hasRole('Customer'); }

  get greeting(): string {
    const h = new Date().getHours();
    if (h < 12) return 'Buenos días';
    if (h < 19) return 'Buenas tardes';
    return 'Buenas noches';
  }

  get firstRole(): string {
    return this.userRoles[0] ?? 'Usuario';
  }

  private loadAdminData(): void {
    this.ordersService.getSummaryStats().subscribe(stats => {
      this.adminKpis = [
        { icon: '📦', label: 'Total pedidos',    value: stats.totalPedidos,   change: '+12% este mes', positive: true },
        { icon: '💰', label: 'Ventas del día',   value: this.formatCLP(stats.ventasDelDia), change: '+8% vs ayer', positive: true },
        { icon: '🔄', label: 'En proceso',        value: stats.pedidosEnProceso, change: '3 nuevos hoy', positive: true },
        { icon: '⏳', label: 'Pendientes',        value: stats.pedidosPendientes, change: '2 sin atención', positive: false },
        { icon: '✅', label: 'Completados',       value: stats.pedidosCompletados, change: 'Hoy', positive: true },
        { icon: '👥', label: 'Usuarios activos',  value: 24, change: '+3 esta semana', positive: true },
      ];
    });
  }

  private loadOperatorData(): void {
    this.ordersService.getOrders().subscribe(orders => {
      this.operatorEnProceso = orders.filter(o => o.estado === 'En proceso');
      this.operatorPendientes = orders.filter(o => o.estado === 'Pendiente');
    });
  }

  private loadCustomerData(): void {
    // En producción usaría getOrdersByCustomer(this.activeAccount.localAccountId)
    this.ordersService.getOrders().subscribe(orders => {
      this.customerOrders = orders.slice(0, 5); // Mock: muestra todos como si fueran del cliente
    });
  }

  goToOrders(): void { this.router.navigate(['/orders']); }
  goToOrder(id: number): void { this.router.navigate(['/orders', id]); }

  formatCLP(value: number): string {
    return new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 0 }).format(value);
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('es-CL', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  logout(): void { this.authService.logout(); }

  // Debug helper
  async logToken(): Promise<void> {
    const account = this.authService.getActiveAccount();
    if (!account) return;
    const token = await this.authService.acquireToken(account);
    console.log('JWT Token:', token);
  }
}