import { Service, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, shareReplay, tap, throwError } from 'rxjs';
import { AuthApiService } from './auth-api.service';
import { AuthStoreService } from './auth-store.service';

@Service()
export class TokenRefreshService {
  private authApi = inject(AuthApiService);
  private store = inject(AuthStoreService);
  private router = inject(Router);
  private inFlight: Observable<string> | null = null;

  public refresh(): Observable<string> {
    if (this.inFlight) {
      return this.inFlight;
    }
    const refreshToken = this.store.refreshToken();
    if (!refreshToken) {
      return this.expire();
    }
    this.inFlight = this.authApi.refresh(refreshToken).pipe(
      tap((tokens) => this.store.save(tokens)),
      map((tokens) => tokens.accessToken),
      catchError(() => this.expire()),
      finalize(() => (this.inFlight = null)),
      shareReplay(1),
    );
    return this.inFlight;
  }

  private expire(): Observable<never> {
    this.store.clear();
    void this.router.navigateByUrl('/login');
    return throwError(() => new Error('Session expired'));
  }
}
