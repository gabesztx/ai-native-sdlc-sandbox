import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ButtonDirective, ButtonIcon, ButtonLabel } from 'primeng/button';
import { Card } from 'primeng/card';
import { Skeleton } from 'primeng/skeleton';
import { ReviewSummary } from '../../../core/api/models';
import { EmptyStateComponent } from '../../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';
import { StatTileComponent } from '../../../shared/ui/stat-tile/stat-tile.component';
import { StatusTagComponent } from '../../../shared/ui/status-tag/status-tag.component';
import { ReviewsApiService } from '../reviews-api.service';

@Component({
  selector: 'review-list-page',
  imports: [
    RouterLink,
    DatePipe,
    ButtonDirective,
    ButtonIcon,
    ButtonLabel,
    Card,
    Skeleton,
    EmptyStateComponent,
    PageHeaderComponent,
    StatTileComponent,
    StatusTagComponent,
  ],
  templateUrl: './review-list-page.component.html',
  styleUrl: './review-list-page.component.scss',
})
export class ReviewListPageComponent {
  private reviewsApi = inject(ReviewsApiService);

  public reviews = toSignal<ReviewSummary[] | undefined>(this.reviewsApi.list().pipe(catchError(() => of([]))), {
    initialValue: undefined,
  });
  public loading = computed(() => this.reviews() === undefined);
  public stats = computed(() => {
    const reviews = this.reviews() ?? [];
    return {
      pending: reviews.filter((review) => review.status === 'PENDING').length,
      submitted: reviews.filter((review) => review.status === 'SUBMITTED').length,
    };
  });
  public skeletonRows = [1, 2, 3, 4];
}
