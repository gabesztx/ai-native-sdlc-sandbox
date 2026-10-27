import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { EMPTY, catchError, of, switchMap } from 'rxjs';
import { FormDetail, FormResponse } from '../../../api/models';
import { problemOf } from '../../../core/http/problem-detail';
import { FormsApiService } from '../forms-api.service';
import { FORM_STATUS_LABELS, REVIEW_STATUS_LABELS } from '../shared/status-labels';

@Component({
  selector: 'form-detail-page',
  imports: [RouterLink, DatePipe],
  templateUrl: './form-detail-page.component.html',
  styleUrl: './form-detail-page.component.scss',
})
export class FormDetailPageComponent {
  private formsApi = inject(FormsApiService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  public id = input.required<string>();
  public detail = signal<FormDetail | null>(null);
  public responses = signal<FormResponse[]>([]);
  public error = signal('');
  public formStatusLabels = FORM_STATUS_LABELS;
  public reviewStatusLabels = REVIEW_STATUS_LABELS;

  public constructor() {
    toObservable(this.id)
      .pipe(
        switchMap((id) => this.formsApi.get(Number(id)).pipe(catchError((e: unknown) => this.fail(e)))),
        takeUntilDestroyed(),
      )
      .subscribe((detail) => this.show(detail));
  }

  public questionText(questionId: number): string {
    return this.detail()?.questions.find((question) => question.id === questionId)?.text ?? '';
  }

  public onSend(): void {
    this.error.set('');
    this.formsApi
      .send(Number(this.id()))
      .pipe(
        catchError((e: unknown) => this.fail(e)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((detail) => this.show(detail));
  }

  public onDelete(): void {
    if (!window.confirm('Biztosan törlöd?')) {
      return;
    }
    this.formsApi
      .delete(Number(this.id()))
      .pipe(
        catchError((e: unknown) => this.fail(e)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => void this.router.navigateByUrl('/admin/forms'));
  }

  private show(detail: FormDetail): void {
    this.detail.set(detail);
    const responses$ =
      detail.status === 'SENT'
        ? this.formsApi.responses(detail.id).pipe(catchError((e: unknown) => this.fail(e)))
        : of([]);
    responses$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((responses) => this.responses.set(responses));
  }

  private fail(error: unknown) {
    this.error.set(problemOf(error).detail ?? 'Váratlan hiba történt');
    return EMPTY;
  }
}
