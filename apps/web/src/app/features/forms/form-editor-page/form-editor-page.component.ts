import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { EMPTY, catchError, filter, switchMap } from 'rxjs';
import { FormDetail, FormInput, Question } from '../../../api/models';
import { problemOf } from '../../../core/http/problem-detail';
import { TeamApiService } from '../../../core/team/team-api.service';
import { FormsApiService } from '../forms-api.service';

type QuestionType = Question['type'];

type QuestionGroup = FormGroup<{
  text: FormControl<string>;
  type: FormControl<QuestionType>;
  required: FormControl<boolean>;
  scaleMin: FormControl<number | null>;
  scaleMax: FormControl<number | null>;
}>;

@Component({
  selector: 'form-editor-page',
  imports: [ReactiveFormsModule],
  templateUrl: './form-editor-page.component.html',
  styleUrl: './form-editor-page.component.scss',
})
export class FormEditorPageComponent {
  private formsApi = inject(FormsApiService);
  private teamApi = inject(TeamApiService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  public id = input<string>();
  public isEdit = computed(() => this.id() !== undefined);
  public members = toSignal(this.teamApi.list().pipe(catchError(() => EMPTY)), { initialValue: [] });
  public serverErrors = signal<Record<string, string>>({});
  public generalError = signal('');

  public form = new FormGroup({
    topic: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(200)],
    }),
    evaluatedMemberId: new FormControl<number | null>(null, Validators.required),
    questions: new FormArray<QuestionGroup>([]),
  });

  public constructor() {
    toObservable(this.id)
      .pipe(
        filter((id): id is string => id !== undefined),
        switchMap((id) => this.formsApi.get(Number(id)).pipe(catchError(() => this.notFound()))),
        takeUntilDestroyed(),
      )
      .subscribe((detail) => this.load(detail));
  }

  public get questions(): FormArray<QuestionGroup> {
    return this.form.controls.questions;
  }

  public onAddQuestion(type: QuestionType): void {
    this.questions.push(newQuestion(type));
  }

  public onRemoveQuestion(index: number): void {
    this.questions.removeAt(index);
  }

  public error(key: string): string {
    return this.serverErrors()[key] ?? '';
  }

  public onSave(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const body = this.toInput();
    const id = this.id();
    const request = id === undefined ? this.formsApi.create(body) : this.formsApi.update(Number(id), body);
    this.serverErrors.set({});
    this.generalError.set('');
    request
      .pipe(
        catchError((error: unknown) => this.showError(error)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((detail) => void this.router.navigate(['/admin/forms', detail.id]));
  }

  public onDelete(): void {
    const id = this.id();
    if (id === undefined || !window.confirm('Biztosan törlöd?')) {
      return;
    }
    this.formsApi
      .delete(Number(id))
      .pipe(
        catchError((error: unknown) => this.showError(error)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => void this.router.navigateByUrl('/admin/forms'));
  }

  private load(detail: FormDetail): void {
    if (detail.status !== 'DRAFT') {
      void this.router.navigate(['/admin/forms', detail.id]);
      return;
    }
    this.form.patchValue({ topic: detail.topic, evaluatedMemberId: detail.evaluatedMember.id });
    this.questions.clear();
    detail.questions.forEach((question) => {
      const group = newQuestion(question.type);
      group.patchValue({
        text: question.text,
        required: question.required,
        scaleMin: question.scaleMin ?? null,
        scaleMax: question.scaleMax ?? null,
      });
      this.questions.push(group);
    });
  }

  private toInput(): FormInput {
    const value = this.form.getRawValue();
    return {
      topic: value.topic,
      evaluatedMemberId: value.evaluatedMemberId ?? 0,
      questions: value.questions.map((question) => ({
        text: question.text,
        type: question.type,
        required: question.required,
        scaleMin: question.type === 'SCALE' ? (question.scaleMin ?? undefined) : undefined,
        scaleMax: question.type === 'SCALE' ? (question.scaleMax ?? undefined) : undefined,
      })),
    };
  }

  private showError(error: unknown) {
    const problem = problemOf(error);
    this.serverErrors.set(problem.errors ?? {});
    this.generalError.set(problem.errors ? 'Javítsd a megjelölt mezőket.' : (problem.detail ?? ''));
    return EMPTY;
  }

  private notFound() {
    this.generalError.set('Az űrlap nem található.');
    return EMPTY;
  }
}

function newQuestion(type: QuestionType): QuestionGroup {
  const scale = type === 'SCALE';
  return new FormGroup({
    text: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(500)],
    }),
    type: new FormControl<QuestionType>(type, { nonNullable: true }),
    required: new FormControl(true, { nonNullable: true }),
    scaleMin: new FormControl<number | null>(scale ? 1 : null),
    scaleMax: new FormControl<number | null>(scale ? 5 : null),
  });
}
