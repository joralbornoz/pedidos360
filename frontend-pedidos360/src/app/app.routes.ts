import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { LoginComponent } from './pages/login/login.component';
import { AuthCallbackComponent } from './pages/auth-callback/auth-callback.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { OrdersComponent } from './pages/orders/orders.component';
import { OrderListComponent } from './pages/orders/order-list/order-list.component';
import { OrderDetailComponent } from './pages/orders/order-detail/order-detail.component';
import { CatalogComponent } from './pages/catalog/catalog.component';
import { roleGuard } from './core/role.guard';

export const routes: Routes = [
  { path: 'login',         component: LoginComponent },
  { path: 'auth/callback', component: AuthCallbackComponent },

  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [MsalGuard]
  },

  {
    path: 'orders',
    component: OrdersComponent,
    canActivate: [MsalGuard],
    children: [
      { path: '',    component: OrderListComponent },
      { path: ':id', component: OrderDetailComponent }
    ]
  },

  {
    path: 'catalog',
    component: CatalogComponent,
    canActivate: [MsalGuard, roleGuard(['Admin', 'Operator'])]
  },

  { path: '',   redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];