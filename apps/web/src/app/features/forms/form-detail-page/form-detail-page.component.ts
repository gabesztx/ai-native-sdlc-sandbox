import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { EMPTY, catchError, finalize, of, switchMap } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Accordion, AccordionContent, AccordionHeader, AccordionPanel } from 'primeng/accordion';
import { ButtonDirective, ButtonIcon, ButtonLabel } from 'primeng/button';
import { Card } from 'primeng/card';
import { Message } from 'primeng/message';
import { ProgressBar } from 'primeng/progressbar';
import { Skeleton } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { FormDetail, FormResponse } from '../../../api/models';
import { problemOf } from '../../../core/http/problem-detail';
import { EmptyStateComponent } from '../../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';
import { StatusTagComponent } from '../../../shared/ui/status-tag/status-tag.component';
import { confirmDelete } from '../confirm-delete';
import { FormsApiService } from '../forms-api.service';

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
  private formsApi = inject(FormsApiService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private messages = inject(MessageService);
  private confirmation = inject(ConfirmationService);

  public id = input.required<string>();
  public detail = signal<FormDetail | null>(null);
  public responses = signal<FormResponse[]>([]);
  public error = signal('');
  public sending = signal(false);
  public deleting = signal(false);
  public loading = computed(() => this.detail() === null && this.error() === '');
  public progress = computed(() => {
    const form = this.detail();
    return form && form.recipientCount > 0 ? Math.round((form.submittedCount / form.recipientCount) * 100) : 0;
  });

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
    if (this.sending()) {
      return;
    }
    this.error.set('');
    this.sending.set(true);
    this.formsApi
      .send(Number(this.id()))
      .pipe(
        catchError((e: unknown) => this.fail(e)),
        finalize(() => this.sending.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((detail) => {
        this.messages.add({ severity: 'success', summary: 'Űrlap kiküldve' });
        this.show(detail);
      });
  }

  public onDelete(): void {
    if (this.deleting()) {
      return;
    }
    confirmDelete(this.confirmation, () => this.delete());
  }

  private delete(): void {
    this.deleting.set(true);
    this.formsApi
      .delete(Number(this.id()))
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
