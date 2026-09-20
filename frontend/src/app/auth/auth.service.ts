import { Injectable, signal } from '@angular/core';
import { keycloak } from './keycloak';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly authenticated = signal(Boolean(keycloak.authenticated));
  readonly username = keycloak.tokenParsed?.['preferred_username'] ?? '';

  login(): void {
    void keycloak.login({ redirectUri: window.location.origin });
  }

  logout(): void {
    void keycloak.logout({ redirectUri: window.location.origin });
  }
}
