import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { LoginComponent } from './pages/login/login.component';
import { AuthCallbackComponent } from './pages/auth-callback/auth-callback.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'auth/callback', component: AuthCallbackComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [MsalGuard]
  },
  // Ejemplo para rutas futuras protegidas por rol específico (ej. Reports = solo Admin):
  // Recuerda importar: import { roleGuard } from './core/role.guard';
  // {
  //   path: 'reports',
  //   component: ReportsComponent,
  //   canActivate: [MsalGuard, roleGuard('Admin')]
  // },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];