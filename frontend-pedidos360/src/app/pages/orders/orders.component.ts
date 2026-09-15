import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TopbarComponent } from '../../shared/topbar/topbar.component';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [RouterOutlet, TopbarComponent],
  template: `
    <div class="page-layout" id="orders-page">
      <app-topbar></app-topbar>
      <router-outlet></router-outlet>
    </div>
  `
})
export class OrdersComponent {}
