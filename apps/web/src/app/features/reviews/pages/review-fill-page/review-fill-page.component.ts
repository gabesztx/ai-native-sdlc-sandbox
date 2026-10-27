import { Component, DestroyRef, computed, inject, input, linkedSignal, numberAttribute, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormField, FormRoot, applyEach, form, maxLength, required, requiredError, validate } from '@angular/forms/signals';
import { Router } from '@angular/router';
import { EMPTY, catchError, finalize, switchMap } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonDirective, ButtonIcon, ButtonLabel } from 'primeng/button';
import { Card } from 'primeng/card';
import { InputNumber } from 'primeng/inputnumber';
import { Message } from 'primeng/message';
import { SelectButton } from 'primeng/selectbutton';
import { Textarea } from 'primeng/textarea';
import { AnswerInput, Question } from '../../../../core/api/models';
import { problemOf } from '../../../../core/http/problem-detail';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { ReviewsService } from '../../services/reviews.service';

const MAX_TEXT_LENGTH = 2000;
const MAX_SELECT_BUTTON_VALUES = 11;

interface AnswerDraft {
  type: Question['type'];
  required: boolean;
  scale: number | null;
  text: string;
}

@Component({
  selector: 'review-fill-page',
  imports: [
    FormField,
    FormRoot,
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
  private reviewsService = inject(ReviewsService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private messages = inject(MessageService);

  public id = input.required({ transform: numberAttribute });
  private loaded = toSignal(
    toObservable(this.id).pipe(
      switchMap((id) => this.reviewsService.get(id).pipe(catchError((e: unknown) => this.fail(e)))),
    ),
  );
  public review = computed(() => this.loaded() ?? null);
  private answers = linkedSignal(() => ({ answers: (this.review()?.questions ?? []).map(answerDraft) }));
  public fill = form(this.answers, (path) => {
    applyEach(path.answers, (answer) => {
      required(answer.scale, { when: ({ valueOf }) => valueOf(answer.required) && valueOf(answer.type) === 'SCALE' });
      maxLength(answer.text, MAX_TEXT_LENGTH);
      validate(answer.text, ({ value, valueOf }) =>
        valueOf(answer.required) && valueOf(answer.type) === 'TEXT' && !value().trim() ? requiredError() : undefined,
      );
    });
  });
  public questionErrors = signal<Record<number, string>>({});
  public error = signal('');
  public submitting = signal(false);
  public loading = computed(() => this.review() === null && this.error() === '');

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
    if (this.fill().invalid()) {
      this.fill().markAsTouched();
      this.error.set('Töltsd ki a kötelező kérdéseket.');
      return;
    }
    const body = { answers: this.toAnswers(review.questions) };
    this.error.set('');
    this.questionErrors.set({});
    this.submitting.set(true);
    this.reviewsService
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

  private toAnswers(questions: Question[]): AnswerInput[] {
    return questions.flatMap((question, index): AnswerInput[] => {
      const answer = this.answers().answers[index];
      if (question.type === 'SCALE') {
        return answer.scale === null ? [] : [{ questionId: question.id, scaleValue: answer.scale }];
      }
      const text = answer.text.trim();
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

function answerDraft(question: Question): AnswerDraft {
  return { type: question.type, required: question.required, scale: null, text: '' };
}

function questionIdOf(key: string, sent: AnswerInput[]): number | null {
  const direct = /^question\.(\d+)$/.exec(key);
  if (direct) {
    return Number(direct[1]);
  }
  const indexed = /^answers\[(\d+)\]/.exec(key);
  return indexed ? (sent[Number(indexed[1])]?.questionId ?? null) : null;
}
