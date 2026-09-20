import { Injectable, signal } from '@angular/core';
import { keycloak } from './keycloak';

type TokenClaims = {
  realm_access?: { roles?: string[] };
  resource_access?: Record<string, { roles?: string[] }>;
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly authenticated = signal(Boolean(keycloak.authenticated));
  readonly username = keycloak.tokenParsed?.['preferred_username'] ?? '';
  readonly locale = keycloak.tokenParsed?.['locale'];

  login(): void {
    void keycloak.login({ redirectUri: window.location.origin });
  }

  logout(): void {
    void keycloak.logout({ redirectUri: window.location.origin });
  }

  hasRole(role: string): boolean {
    const token = keycloak.tokenParsed as TokenClaims | undefined;
    const realmRoles = token?.realm_access?.roles ?? [];
    const clientRoles = token?.resource_access?.[keycloak.clientId ?? '']?.roles ?? [];
    return realmRoles.includes(role) || clientRoles.includes(role);
  }

  hasAnyRole(...roles: string[]): boolean {
    return roles.some((role) => this.hasRole(role));
  }
}
