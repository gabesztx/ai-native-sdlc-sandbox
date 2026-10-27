import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiConfiguration } from '../../api/api-configuration';
import { createForm } from '../../api/fn/form-controller/create-form';
import { deleteForm } from '../../api/fn/form-controller/delete-form';
import { getForm } from '../../api/fn/form-controller/get-form';
import { listFormResponses } from '../../api/fn/form-controller/list-form-responses';
import { listForms } from '../../api/fn/form-controller/list-forms';
import { sendForm } from '../../api/fn/form-controller/send-form';
import { updateForm } from '../../api/fn/form-controller/update-form';
import { FormDetail, FormInput, FormResponse, FormSummary } from '../../api/models';

@Service()
export class FormsApiService {
  private http = inject(HttpClient);
  private config = inject(ApiConfiguration);

  public list(): Observable<FormSummary[]> {
    return listForms(this.http, this.config.rootUrl).pipe(map((response) => response.body));
  }

  public get(formId: number): Observable<FormDetail> {
    return getForm(this.http, this.config.rootUrl, { formId }).pipe(map((response) => response.body));
  }

  public create(body: FormInput): Observable<FormDetail> {
    return createForm(this.http, this.config.rootUrl, { body }).pipe(map((response) => response.body));
  }

  public update(formId: number, body: FormInput): Observable<FormDetail> {
    return updateForm(this.http, this.config.rootUrl, { formId, body }).pipe(
      map((response) => response.body),
    );
  }

  public delete(formId: number): Observable<void> {
    return deleteForm(this.http, this.config.rootUrl, { formId }).pipe(map(() => undefined));
  }

  public send(formId: number): Observable<FormDetail> {
    return sendForm(this.http, this.config.rootUrl, { formId }).pipe(map((response) => response.body));
  }

  public responses(formId: number): Observable<FormResponse[]> {
    return listFormResponses(this.http, this.config.rootUrl, { formId }).pipe(
      map((response) => response.body),
    );
  }
}
