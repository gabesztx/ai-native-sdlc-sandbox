import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ButtonDirective, ButtonIcon, ButtonLabel } from 'primeng/button';
import { Card } from 'primeng/card';
import { ProgressBar } from 'primeng/progressbar';
import { Skeleton } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { FormSummary } from '../../../core/api/models';
import { BreakpointService } from '../../../core/layout/breakpoint.service';
import { EmptyStateComponent } from '../../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';
import { StatTileComponent } from '../../../shared/ui/stat-tile/stat-tile.component';
import { StatusTagComponent } from '../../../shared/ui/status-tag/status-tag.component';
import { FormsApiService } from '../forms-api.service';

@Component({
  selector: 'form-list-page',
  imports: [
    RouterLink,
    DatePipe,
    NgTemplateOutlet,
    ButtonDirective,
    ButtonIcon,
    ButtonLabel,
    Card,
    ProgressBar,
    Skeleton,
    TableModule,
    EmptyStateComponent,
    PageHeaderComponent,
    StatTileComponent,
    StatusTagComponent,
  ],
  templateUrl: './form-list-page.component.html',
  styleUrl: './form-list-page.component.scss',
})
export class FormListPageComponent {
  private formsApi = inject(FormsApiService);
  private router = inject(Router);

  public isMobile = inject(BreakpointService).isMobile;
  public forms = toSignal<FormSummary[] | undefined>(this.formsApi.list().pipe(catchError(() => of([]))), {
    initialValue: undefined,
  });
  public loading = computed(() => this.forms() === undefined);
  public stats = computed(() => {
    const forms = this.forms() ?? [];
    const sent = forms.filter((form) => form.status === 'SENT');
    return {
      total: forms.length,
      drafts: forms.length - sent.length,
      sent: sent.length,
      submitted: `${sum(sent, (form) => form.submittedCount)} / ${sum(sent, (form) => form.recipientCount)}`,
    };
  });
  public skeletonRows = [1, 2, 3, 4];

  public progress(form: FormSummary): number {
    return form.recipientCount === 0 ? 0 : Math.round((form.submittedCount / form.recipientCount) * 100);
  }

  public onOpen(form: FormSummary): void {
    void this.router.navigate(['/admin/forms', form.id]);
  }
}

function sum(forms: FormSummary[], value: (form: FormSummary) => number): number {
  return forms.reduce((total, form) => total + value(form), 0);
}
