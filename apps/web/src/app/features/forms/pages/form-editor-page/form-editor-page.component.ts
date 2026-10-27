import { Component, DestroyRef, computed, inject, input, linkedSignal, numberAttribute, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FieldTree, FormField, FormRoot, applyEach, form, maxLength, required } from '@angular/forms/signals';
import { Router } from '@angular/router';
import { EMPTY, catchError, filter, finalize, of, switchMap, tap } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonDirective, ButtonIcon, ButtonLabel } from 'primeng/button';
import { Card } from 'primeng/card';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { FormDetail, FormInput, Question } from '../../../../core/api/models';
import { problemOf } from '../../../../core/http/problem-detail';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { ConfirmDeleteService } from '../../services/confirm-delete.service';
import { FormsService } from '../../services/forms.service';

type QuestionType = Question['type'];

interface QuestionDraft {
  key: number;
  text: string;
  type: QuestionType;
  required: boolean;
  scaleMin: number | null;
  scaleMax: number | null;
}

interface EditorModel {
  topic: string;
  evaluatedMemberId: number | null;
  questions: QuestionDraft[];
}

@Component({
  selector: 'form-editor-page',
  imports: [
    FormField,
    FormRoot,
    ButtonDirective,
    ButtonIcon,
    ButtonLabel,
    Card,
    InputNumber,
    InputText,
    Message,
    Select,
    Tag,
    ToggleSwitch,
    PageHeaderComponent,
  ],
  templateUrl: './form-editor-page.component.html',
  styleUrl: './form-editor-page.component.scss',
})
export class FormEditorPageComponent {
  private formsService = inject(FormsService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private messages = inject(MessageService);
  private confirmDelete = inject(ConfirmDeleteService);
  private nextKey = 0;

  public id = input<number | undefined, unknown>(undefined, { transform: optionalNumber });
  public isEdit = computed(() => this.id() !== undefined);
  private membersResource = rxResource({
    stream: () => this.formsService.teamMembers().pipe(catchError(() => of([]))),
  });
  public members = computed(() => this.membersResource.value() ?? []);
  public serverErrors = signal<Record<string, string>>({});
  private loadError = signal('');
  private actionError = signal('');
  public generalError = computed(() => this.actionError() || this.loadError());
  public saving = signal(false);
  public deleting = signal(false);

  private draft = toSignal(
    toObservable(this.id).pipe(
      filter((id): id is number => id !== undefined),
      switchMap((id) =>
        this.formsService.get(id).pipe(
          tap((detail) => this.leaveIfNotDraft(detail)),
          catchError(() => this.notFound()),
        ),
      ),
    ),
  );
  private model = linkedSignal<EditorModel>(() => this.toModel(this.draft()));
  public questions = computed(() => this.model().questions);
  public editor = form(this.model, (path) => {
    required(path.topic);
    maxLength(path.topic, 200);
    required(path.evaluatedMemberId);
    applyEach(path.questions, (question) => {
      required(question.text);
      maxLength(question.text, 500);
    });
  });

  public onAddQuestion(type: QuestionType): void {
    this.model.update((model) => ({ ...model, questions: [...model.questions, this.newQuestion(type)] }));
  }

  public onRemoveQuestion(index: number): void {
    this.model.update((model) => ({ ...model, questions: model.questions.filter((_, i) => i !== index) }));
  }

  public error(key: string): string {
    return this.serverErrors()[key] ?? '';
  }

  public isInvalid<T>(field: FieldTree<T>, errorKey: string): boolean {
    return (field().invalid() && field().touched()) || this.error(errorKey) !== '';
  }

  public onSave(): void {
    if (this.saving()) {
      return;
    }
    if (this.editor().invalid()) {
      this.editor().markAsTouched();
      return;
    }
    const body = this.toInput();
    const id = this.id();
    const request = id === undefined ? this.formsService.create(body) : this.formsService.update(id, body);
    this.serverErrors.set({});
    this.actionError.set('');
    this.loadError.set('');
    this.saving.set(true);
    request
      .pipe(
        catchError((error: unknown) => this.showError(error)),
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((detail) => {
        this.messages.add({ severity: 'success', summary: 'Piszkozat mentve' });
        void this.router.navigate(['/admin/forms', detail.id]);
      });
  }

  public onDelete(): void {
    const id = this.id();
    if (id === undefined || this.deleting()) {
      return;
    }
    this.confirmDelete.confirm(() => this.delete(id));
  }

  private delete(id: number): void {
    this.deleting.set(true);
    this.formsService
      .delete(id)
      .pipe(
        catchError((error: unknown) => this.showError(error)),
        finalize(() => this.deleting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.messages.add({ severity: 'success', summary: 'Piszkozat törölve' });
        void this.router.navigateByUrl('/admin/forms');
      });
  }

  private leaveIfNotDraft(detail: FormDetail): void {
    if (detail.status !== 'DRAFT') {
      void this.router.navigate(['/admin/forms', detail.id]);
    }
  }

  private toModel(detail: FormDetail | undefined): EditorModel {
    if (!detail || detail.status !== 'DRAFT') {
      return { topic: '', evaluatedMemberId: null, questions: [] };
    }
    return {
      topic: detail.topic,
      evaluatedMemberId: detail.evaluatedMember.id,
      questions: detail.questions.map((question) => ({
        ...this.newQuestion(question.type),
        text: question.text,
        required: question.required,
        scaleMin: question.scaleMin ?? null,
        scaleMax: question.scaleMax ?? null,
      })),
    };
  }

  private newQuestion(type: QuestionType): QuestionDraft {
    const scale = type === 'SCALE';
    return {
      key: this.nextKey++,
      text: '',
      type,
      required: true,
      scaleMin: scale ? 1 : null,
      scaleMax: scale ? 5 : null,
    };
  }

  private toInput(): FormInput {
    const value = this.model();
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
    this.actionError.set(problem.errors ? 'Javítsd a megjelölt mezőket.' : (problem.detail ?? ''));
    return EMPTY;
  }

  private notFound() {
    this.loadError.set('Az űrlap nem található.');
    return EMPTY;
  }
}

function optionalNumber(value: unknown): number | undefined {
  return value === undefined ? undefined : numberAttribute(value);
}
