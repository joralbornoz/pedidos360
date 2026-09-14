import { Injectable } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(private msalService: MsalService) {}

  login(): void {
    this.msalService.loginRedirect({
      scopes: environment.apiConfig.scopes,
      redirectStartPage: window.location.origin + '/dashboard',
      extraQueryParameters: {
        'dc': 'ESTS-PILOT-NONINTERACTIVE',
        'p': 'Pedidos360SignUpSignIn'
      }
    });
  }

  logout(): void {
    this.msalService.logoutRedirect({
      postLogoutRedirectUri: environment.msalConfig.postLogoutRedirectUri
    });
  }

  getActiveAccountName(): string {
    const account = this.msalService.instance.getActiveAccount();
    return account?.name || account?.username || '';
  }

  getUserRoles(): string[] {
    const account = this.msalService.instance.getActiveAccount();
    const claims = account?.idTokenClaims as any;
    return claims?.roles || [];
  }

  hasRole(role: string): boolean {
    return this.getUserRoles().includes(role);
  }

  isLoggedIn(): boolean {
    return this.msalService.instance.getActiveAccount() !== null;
  }
  getActiveAccount() {
    return this.msalService.instance.getActiveAccount();
  }

  async acquireToken(account: any): Promise<string> {
    const result = await this.msalService.instance.acquireTokenSilent({
      scopes: environment.apiConfig.scopes,
      account: account
    });
    return result.accessToken;
  }
}