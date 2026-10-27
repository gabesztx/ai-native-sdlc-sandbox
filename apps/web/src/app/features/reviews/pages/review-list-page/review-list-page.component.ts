import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ButtonDirective, ButtonIcon, ButtonLabel } from 'primeng/button';
import { Card } from 'primeng/card';
import { Skeleton } from 'primeng/skeleton';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatTileComponent } from '../../../../shared/components/stat-tile/stat-tile.component';
import { StatusTagComponent } from '../../../../shared/components/status-tag/status-tag.component';
import { ReviewsService } from '../../services/reviews.service';

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
  private reviewsService = inject(ReviewsService);

  private reviewsResource = rxResource({ stream: () => this.reviewsService.list().pipe(catchError(() => of([]))) });
  public reviews = computed(() => this.reviewsResource.value());
  public loading = computed(() => this.reviewsResource.isLoading());
  public stats = computed(() => {
    const reviews = this.reviews() ?? [];
    return {
      pending: reviews.filter((review) => review.status === 'PENDING').length,
      submitted: reviews.filter((review) => review.status === 'SUBMITTED').length,
    };
  });
  public skeletonRows = [1, 2, 3, 4];
}
