import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/auth.service';
import { OrdersService, Order } from '../../core/orders.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  userName: string = '';
  userRoles: string[] = [];

  orders: Order[] = [];
  ordersError: string = '';
  ordersLoading: boolean = false;

  constructor(
    private authService: AuthService,
    private ordersService: OrdersService
  ) {}

  ngOnInit(): void {
    this.userName = this.authService.getActiveAccountName();
    this.userRoles = this.authService.getUserRoles();
  }

  logout(): void {
    this.authService.logout();
  }

  loadOrders(): void {
    this.ordersLoading = true;
    this.ordersError = '';
    this.orders = [];

    this.ordersService.getOrders().subscribe({
      next: (data) => {
        this.orders = data;
        this.ordersLoading = false;
      },
      error: (err) => {
        this.ordersError = `Error ${err.status}: ${err.error?.message || err.statusText}`;
        this.ordersLoading = false;
      }
    });
  }
}