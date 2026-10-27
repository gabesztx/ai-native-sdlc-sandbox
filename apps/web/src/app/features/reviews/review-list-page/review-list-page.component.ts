import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { EMPTY, catchError } from 'rxjs';
import { REVIEW_STATUS_LABELS } from '../../../shared/status-labels';
import { ReviewsApiService } from '../reviews-api.service';

@Component({
  selector: 'review-list-page',
  imports: [RouterLink, DatePipe],
  templateUrl: './review-list-page.component.html',
  styleUrl: './review-list-page.component.scss',
})
export class ReviewListPageComponent {
  private reviewsApi = inject(ReviewsApiService);

  public reviews = toSignal(this.reviewsApi.list().pipe(catchError(() => EMPTY)), { initialValue: [] });
  public statusLabels = REVIEW_STATUS_LABELS;
}
