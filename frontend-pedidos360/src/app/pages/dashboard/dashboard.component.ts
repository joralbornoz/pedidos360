import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  userName: string = '';
  userRoles: string[] = [];

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.userName = this.authService.getActiveAccountName();
    this.userRoles = this.authService.getUserRoles();
  }

  logout(): void {
    this.authService.logout();
  }
}