import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { EMPTY, catchError } from 'rxjs';
import { FormsApiService } from '../forms-api.service';
import { FORM_STATUS_LABELS } from '../../../shared/status-labels';

@Component({
  selector: 'form-list-page',
  imports: [RouterLink, DatePipe],
  templateUrl: './form-list-page.component.html',
  styleUrl: './form-list-page.component.scss',
})
export class FormListPageComponent {
  private formsApi = inject(FormsApiService);

  public forms = toSignal(this.formsApi.list().pipe(catchError(() => EMPTY)), { initialValue: [] });
  public statusLabels = FORM_STATUS_LABELS;
}
