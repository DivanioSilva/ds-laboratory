import Keycloak from 'keycloak-js';

export const keycloak = new Keycloak({
  url: `${window.location.protocol}//${window.location.hostname}:8081`,
  realm: 'users',
  clientId: 'angular-client',
});

export async function initializeAuthentication(): Promise<void> {
  await keycloak.init({
    onLoad: 'check-sso',
    pkceMethod: 'S256',
    checkLoginIframe: false,
    silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    logoutMethod: 'POST',
  });
}
