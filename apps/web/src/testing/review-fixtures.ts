import { ReviewDetail, ReviewSummary } from '../app/core/api/models';

export function pendingReview(): ReviewDetail {
  return {
    id: 21,
    topic: 'Csapatmunka Q4',
    evaluatedMember: { id: 3, name: 'Teszt Elek' },
    status: 'PENDING',
    questions: [
      { id: 11, position: 1, text: 'Mennyire segítőkész?', type: 'SCALE', required: true, scaleMin: 1, scaleMax: 5 },
      { id: 12, position: 2, text: 'Miben fejlődhetne?', type: 'TEXT', required: true },
      { id: 13, position: 3, text: 'Egyéb', type: 'TEXT', required: false },
    ],
  };
}

export function reviewSummary(status: ReviewSummary['status']): ReviewSummary {
  return {
    id: 21,
    formId: 7,
    topic: 'Csapatmunka Q4',
    evaluatedMember: { id: 3, name: 'Teszt Elek' },
    status,
    submittedAt: status === 'SUBMITTED' ? '2026-10-27T12:00:00Z' : undefined,
  };
}
