import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Guard de roles para rutas protegidas.
 * Acepta un rol simple o un array de roles permitidos.
 * Ejemplo: roleGuard('Admin') | roleGuard(['Admin', 'Operator'])
 */
export function roleGuard(roles: string | string[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const allowed = Array.isArray(roles) ? roles : [roles];

    const hasAccess = allowed.some(role => authService.hasRole(role));
    if (hasAccess) return true;

    router.navigate(['/dashboard']);
    return false;
  };
}