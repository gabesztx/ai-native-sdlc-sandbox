import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiConfiguration } from '../../core/api/api-configuration';
import { getMyReview } from '../../core/api/fn/review-controller/get-my-review';
import { listMyReviews } from '../../core/api/fn/review-controller/list-my-reviews';
import { submitMyReview } from '../../core/api/fn/review-controller/submit-my-review';
import { ReviewDetail, ReviewSummary, SubmitInput } from '../../core/api/models';

@Service()
export class ReviewsApiService {
  private http = inject(HttpClient);
  private config = inject(ApiConfiguration);

  public list(): Observable<ReviewSummary[]> {
    return listMyReviews(this.http, this.config.rootUrl).pipe(map((response) => response.body));
  }

  public get(reviewId: number): Observable<ReviewDetail> {
    return getMyReview(this.http, this.config.rootUrl, { reviewId }).pipe(map((response) => response.body));
  }

  public submit(reviewId: number, body: SubmitInput): Observable<ReviewSummary> {
    return submitMyReview(this.http, this.config.rootUrl, { reviewId, body }).pipe(
      map((response) => response.body),
    );
  }
}
