export const FORM_STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Piszkozat',
  SENT: 'Kiküldve',
};

export const REVIEW_STATUS_LABELS: Record<string, string> = {
  PENDING: 'Kitöltendő',
  SUBMITTED: 'Beküldve',
};

export type StatusSeverity = 'secondary' | 'info' | 'warn' | 'success';

export const STATUS_SEVERITIES: Record<string, StatusSeverity> = {
  DRAFT: 'secondary',
  SENT: 'info',
  PENDING: 'warn',
  SUBMITTED: 'success',
};
