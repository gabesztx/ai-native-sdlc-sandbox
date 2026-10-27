import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ReviewDetail, ReviewSummary, SubmitInput } from '../../../core/api/models';
import { ReviewControllerService } from '../../../core/api/services/review-controller.service';

@Service()
export class ReviewsService {
  private reviewApi = inject(ReviewControllerService);

  public list(): Observable<ReviewSummary[]> {
    return this.reviewApi.listMyReviews();
  }

  public get(reviewId: number): Observable<ReviewDetail> {
    return this.reviewApi.getMyReview({ reviewId });
  }

  public submit(reviewId: number, body: SubmitInput): Observable<ReviewSummary> {
    return this.reviewApi.submitMyReview({ reviewId, body });
  }
}
