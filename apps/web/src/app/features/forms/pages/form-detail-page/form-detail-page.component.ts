import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, input, linkedSignal, numberAttribute, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { EMPTY, Observable, catchError, finalize, map, of, startWith, switchMap, tap } from 'rxjs';
import { MessageService } from 'primeng/api';
import { Accordion, AccordionContent, AccordionHeader, AccordionPanel } from 'primeng/accordion';
import { ButtonDirective, ButtonIcon, ButtonLabel } from 'primeng/button';
import { Card } from 'primeng/card';
import { Message } from 'primeng/message';
import { ProgressBar } from 'primeng/progressbar';
import { Skeleton } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { FormDetail, FormResponse } from '../../../../core/api/models';
import { problemOf } from '../../../../core/http/problem-detail';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusTagComponent } from '../../../../shared/components/status-tag/status-tag.component';
import { ConfirmDeleteService } from '../../services/confirm-delete.service';
import { FormsService } from '../../services/forms.service';

interface FormPage {
  detail: FormDetail | null;
  responses: FormResponse[];
  error: string;
}

@Component({
  selector: 'form-detail-page',
  imports: [
    RouterLink,
    DatePipe,
    Accordion,
    AccordionContent,
    AccordionHeader,
    AccordionPanel,
    ButtonDirective,
    ButtonIcon,
    ButtonLabel,
    Card,
    Message,
    ProgressBar,
    Skeleton,
    TableModule,
    Tag,
    EmptyStateComponent,
    PageHeaderComponent,
    StatusTagComponent,
  ],
  templateUrl: './form-detail-page.component.html',
  styleUrl: './form-detail-page.component.scss',
})
export class FormDetailPageComponent {
  private formsService = inject(FormsService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private messages = inject(MessageService);
  private confirmDelete = inject(ConfirmDeleteService);

  public id = input.required({ transform: numberAttribute });
  private loadedPage = toSignal(
    toObservable(this.id).pipe(
      switchMap((id) =>
        this.formsService.get(id).pipe(
          switchMap((detail) => this.withResponses(detail)),
          catchError((e: unknown) => of(failedPage(e))),
        ),
      ),
    ),
  );
  private page = linkedSignal(() => this.loadedPage());
  private actionError = signal('');
  public detail = computed(() => this.page()?.detail ?? null);
  public responses = computed(() => this.page()?.responses ?? []);
  public error = computed(() => this.actionError() || (this.page()?.error ?? ''));
  public sending = signal(false);
  public deleting = signal(false);
  public loading = computed(() => this.page() === undefined);
  public progress = computed(() => {
    const form = this.detail();
    return form && form.recipientCount > 0 ? Math.round((form.submittedCount / form.recipientCount) * 100) : 0;
  });

  public questionText(questionId: number): string {
    return this.detail()?.questions.find((question) => question.id === questionId)?.text ?? '';
  }

  public onSend(): void {
    if (this.sending()) {
      return;
    }
    this.actionError.set('');
    this.sending.set(true);
    this.formsService
      .send(this.id())
      .pipe(
        catchError((e: unknown) => this.fail(e)),
        finalize(() => this.sending.set(false)),
        tap(() => this.messages.add({ severity: 'success', summary: 'Űrlap kiküldve' })),
        switchMap((detail) => this.withResponses(detail)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((page) => this.page.set(page));
  }

  public onDelete(): void {
    if (this.deleting()) {
      return;
    }
    this.confirmDelete.confirm(() => this.delete());
  }

  private delete(): void {
    this.deleting.set(true);
    this.formsService
      .delete(this.id())
      .pipe(
        catchError((e: unknown) => this.fail(e)),
        finalize(() => this.deleting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.messages.add({ severity: 'success', summary: 'Piszkozat törölve' });
        void this.router.navigateByUrl('/admin/forms');
      });
  }

  private withResponses(detail: FormDetail): Observable<FormPage> {
    const loaded: FormPage = { detail, responses: [], error: '' };
    if (detail.status !== 'SENT') {
      return of(loaded);
    }
    return this.formsService.responses(detail.id).pipe(
      map((responses) => ({ ...loaded, responses })),
      catchError((e: unknown) => of({ ...loaded, error: errorMessage(e) })),
      startWith(loaded),
    );
  }

  private fail(error: unknown) {
    this.actionError.set(errorMessage(error));
    return EMPTY;
  }
}

function failedPage(error: unknown): FormPage {
  return { detail: null, responses: [], error: errorMessage(error) };
}

function errorMessage(error: unknown): string {
  return problemOf(error).detail ?? 'Váratlan hiba történt';
}
