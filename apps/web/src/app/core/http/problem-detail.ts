import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from '../../shared/models/problem-detail.model';

export function problemOf(error: unknown): ProblemDetail {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
    return error.error as ProblemDetail;
  }
  return { detail: 'Váratlan hiba történt' };
}
