import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { EMPTY, catchError, finalize, switchMap } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonDirective, ButtonIcon, ButtonLabel } from 'primeng/button';
import { Card } from 'primeng/card';
import { InputNumber } from 'primeng/inputnumber';
import { Message } from 'primeng/message';
import { SelectButton } from 'primeng/selectbutton';
import { Textarea } from 'primeng/textarea';
import { AnswerInput, Question, ReviewDetail } from '../../../api/models';
import { problemOf } from '../../../core/http/problem-detail';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';
import { ReviewsApiService } from '../reviews-api.service';

const MAX_TEXT_LENGTH = 2000;
const MAX_SELECT_BUTTON_VALUES = 11;

type AnswerControl = FormControl<number | string | null>;

@Component({
  selector: 'review-fill-page',
  imports: [
    ReactiveFormsModule,
    ButtonDirective,
    ButtonIcon,
    ButtonLabel,
    Card,
    InputNumber,
    Message,
    SelectButton,
    Textarea,
    PageHeaderComponent,
  ],
  templateUrl: './review-fill-page.component.html',
  styleUrl: './review-fill-page.component.scss',
})
export class ReviewFillPageComponent {
  private reviewsApi = inject(ReviewsApiService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private messages = inject(MessageService);

  public id = input.required<string>();
  public review = signal<ReviewDetail | null>(null);
  public form = new FormGroup({ answers: new FormArray<AnswerControl>([]) });
  public questionErrors = signal<Record<number, string>>({});
  public error = signal('');
  public submitting = signal(false);
  public loading = computed(() => this.review() === null && this.error() === '');

  public constructor() {
    toObservable(this.id)
      .pipe(
        switchMap((id) => this.reviewsApi.get(Number(id)).pipe(catchError((e: unknown) => this.fail(e)))),
        takeUntilDestroyed(),
      )
      .subscribe((review) => this.load(review));
  }

  public get answers(): FormArray<AnswerControl> {
    return this.form.controls.answers;
  }

  public scaleValues(question: Question): number[] {
    const min = question.scaleMin ?? 0;
    const max = question.scaleMax ?? min;
    return Array.from({ length: max - min + 1 }, (_, index) => min + index);
  }

  public scaleOptions(question: Question): { label: string; value: number }[] {
    return this.scaleValues(question).map((value) => ({ label: String(value), value }));
  }

  public useSelectButton(question: Question): boolean {
    return this.scaleValues(question).length <= MAX_SELECT_BUTTON_VALUES;
  }

  public onSubmit(): void {
    const review = this.review();
    if (!review || this.submitting()) {
      return;
    }
    if (this.answers.invalid) {
      this.answers.markAllAsTouched();
      this.error.set('Töltsd ki a kötelező kérdéseket.');
      return;
    }
    const body = { answers: this.toAnswers(review.questions) };
    this.error.set('');
    this.questionErrors.set({});
    this.submitting.set(true);
    this.reviewsApi
      .submit(review.id, body)
      .pipe(
        catchError((e: unknown) => this.showSubmitError(e, body.answers)),
        finalize(() => this.submitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.messages.add({ severity: 'success', summary: 'Értékelés beküldve' });
        void this.router.navigateByUrl('/reviews');
      });
  }

  private load(review: ReviewDetail): void {
    this.review.set(review);
    this.answers.clear();
    review.questions.forEach((question) => this.answers.push(answerControl(question)));
  }

  private toAnswers(questions: Question[]): AnswerInput[] {
    return questions.flatMap((question, index): AnswerInput[] => {
      const value = this.answers.at(index).value;
      if (question.type === 'SCALE') {
        return value === null || value === '' ? [] : [{ questionId: question.id, scaleValue: Number(value) }];
      }
      const text = typeof value === 'string' ? value.trim() : '';
      return text ? [{ questionId: question.id, textValue: text }] : [];
    });
  }

  private showSubmitError(error: unknown, sent: AnswerInput[]) {
    const problem = problemOf(error);
    const byQuestion: Record<number, string> = {};
    Object.entries(problem.errors ?? {}).forEach(([key, message]) => {
      const questionId = questionIdOf(key, sent);
      if (questionId !== null) {
        byQuestion[questionId] = message;
      }
    });
    this.questionErrors.set(byQuestion);
    this.error.set(problem.detail === 'Invalid request' ? 'Javítsd a megjelölt válaszokat.' : (problem.detail ?? ''));
    return EMPTY;
  }

  private fail(error: unknown) {
    this.error.set(problemOf(error).detail ?? 'Váratlan hiba történt');
    return EMPTY;
  }
}

function answerControl(question: Question): AnswerControl {
  if (question.type === 'SCALE') {
    return new FormControl<number | string | null>(null, question.required ? [Validators.required] : []);
  }
  const validators = [Validators.maxLength(MAX_TEXT_LENGTH)];
  return new FormControl<number | string | null>('', question.required ? [...validators, notBlank] : validators);
}

function notBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && control.value.trim() ? null : { required: true };
}

function questionIdOf(key: string, sent: AnswerInput[]): number | null {
  const direct = /^question\.(\d+)$/.exec(key);
  if (direct) {
    return Number(direct[1]);
  }
  const indexed = /^answers\[(\d+)\]/.exec(key);
  return indexed ? (sent[Number(indexed[1])]?.questionId ?? null) : null;
}
