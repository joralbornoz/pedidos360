import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss'
})
export class TopbarComponent implements OnInit {
  userName = '';
  userRoles: string[] = [];
  mobileMenuOpen = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userName = this.authService.getActiveAccountName();
    this.userRoles = this.authService.getUserRoles();
  }

  get userInitials(): string {
    return this.userName.split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase() || 'U';
  }

  get showOrders(): boolean {
    return this.userRoles.some(r => ['Admin', 'Operator', 'Customer'].includes(r));
  }

  get showCatalog(): boolean {
    return this.userRoles.some(r => ['Admin', 'Operator'].includes(r));
  }

  logout(): void {
    this.authService.logout();
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }
}
