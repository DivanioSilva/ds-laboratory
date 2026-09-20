import Keycloak from 'keycloak-js';

export const keycloak = new Keycloak({
  url: window.location.origin,
  realm: 'users',
  clientId: 'angular-client',
});

export async function initializeAuthentication(): Promise<void> {
  await keycloak.init({
    pkceMethod: 'S256',
    checkLoginIframe: false,
  });
}
