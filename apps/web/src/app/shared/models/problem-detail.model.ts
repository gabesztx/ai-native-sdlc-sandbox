export interface ProblemDetail {
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}
