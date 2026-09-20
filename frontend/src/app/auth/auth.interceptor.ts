import { HttpInterceptorFn } from '@angular/common/http';
import { EMPTY, catchError, from, switchMap } from 'rxjs';
import { keycloak } from './keycloak';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api')) {
    return next(request);
  }

  return from(keycloak.updateToken(30)).pipe(
    switchMap(() => {
      const authenticatedRequest = keycloak.token
        ? request.clone({ setHeaders: { Authorization: `Bearer ${keycloak.token}` } })
        : request;
      return next(authenticatedRequest);
    }),
    catchError(() => {
      void keycloak.login();
      return EMPTY;
    }),
  );
};
