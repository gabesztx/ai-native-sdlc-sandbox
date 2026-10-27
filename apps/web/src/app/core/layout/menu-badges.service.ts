import { Service, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { Observable, catchError, filter, map, of, switchMap } from 'rxjs';
import { FormControllerService } from '../api/services/form-controller.service';
import { ReviewControllerService } from '../api/services/review-controller.service';
import { AuthStoreService } from '../auth/auth-store.service';

@Service()
export class MenuBadgesService {
  private formApi = inject(FormControllerService);
  private reviewApi = inject(ReviewControllerService);
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
        this.formApi.listForms().pipe(map((forms) => forms.filter((form) => form.status === 'DRAFT').length)),
      );
    }
    if (role === 'MEMBER') {
      return this.countOf(
        this.reviewApi
          .listMyReviews()
          .pipe(map((reviews) => reviews.filter((review) => review.status === 'PENDING').length)),
      );
    }
    return of(null);
  }

  private countOf(request: Observable<number>): Observable<number | null> {
    return request.pipe(catchError(() => of(null)));
  }
}
