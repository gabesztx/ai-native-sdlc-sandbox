import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { Observable, catchError, filter, map, of, switchMap } from 'rxjs';
import { ApiConfiguration } from '../../api/api-configuration';
import { listForms } from '../../api/fn/form-controller/list-forms';
import { listMyReviews } from '../../api/fn/review-controller/list-my-reviews';
import { AuthStoreService } from '../auth/auth-store.service';

@Service()
export class MenuBadgesService {
  private http = inject(HttpClient);
  private config = inject(ApiConfiguration);
  private router = inject(Router);
  private store = inject(AuthStoreService);

  public count = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      switchMap(() => this.loadCount()),
    ),
    { initialValue: null },
  );

  private loadCount(): Observable<number | null> {
    const role = this.store.role();
    if (role === 'ADMIN') {
      return this.countOf(
        listForms(this.http, this.config.rootUrl).pipe(
          map((response) => response.body.filter((form) => form.status === 'DRAFT').length),
        ),
      );
    }
    if (role === 'MEMBER') {
      return this.countOf(
        listMyReviews(this.http, this.config.rootUrl).pipe(
          map((response) => response.body.filter((review) => review.status === 'PENDING').length),
        ),
      );
    }
    return of(null);
  }

  private countOf(request: Observable<number>): Observable<number | null> {
    return request.pipe(catchError(() => of(null)));
  }
}
