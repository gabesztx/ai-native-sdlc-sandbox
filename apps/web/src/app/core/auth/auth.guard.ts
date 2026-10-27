import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Me } from '../api/models';
import { AuthStoreService } from './auth-store.service';

export function authGuard(role?: Me['role']): CanActivateFn {
  return () => {
    const store = inject(AuthStoreService);
    const router = inject(Router);
    if (!store.isLoggedIn()) {
      return router.parseUrl('/login');
    }
    if (role && store.role() !== role) {
      return router.parseUrl(store.homePath());
    }
    return true;
  };
}

export const homeRedirectGuard: CanActivateFn = () => {
  const store = inject(AuthStoreService);
  return inject(Router).parseUrl(store.homePath());
};

export const guestGuard: CanActivateFn = () => {
  const store = inject(AuthStoreService);
  return store.isLoggedIn() ? inject(Router).parseUrl(store.homePath()) : true;
};
