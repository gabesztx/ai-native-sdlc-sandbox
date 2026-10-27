import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthStoreService } from './auth-store.service';
import { TokenRefreshService } from './token-refresh.service';

const PUBLIC_AUTH_PATHS = [
  '/api/auth/admin/login',
  '/api/auth/member/login',
  '/api/auth/refresh',
  '/api/auth/team-members',
];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (isPublicAuthRequest(req)) {
    return next(req);
  }
  const store = inject(AuthStoreService);
  const tokenRefresh = inject(TokenRefreshService);
  return next(withAccessToken(req, store.accessToken())).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && store.refreshToken()) {
        return tokenRefresh.refresh().pipe(switchMap((token) => next(withAccessToken(req, token))));
      }
      return throwError(() => error);
    }),
  );
};

function isPublicAuthRequest(req: HttpRequest<unknown>): boolean {
  return PUBLIC_AUTH_PATHS.some((path) => req.url.endsWith(path));
}

function withAccessToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}
