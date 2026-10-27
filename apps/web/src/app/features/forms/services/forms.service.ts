import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { FormDetail, FormInput, FormResponse, FormSummary, TeamMember } from '../../../core/api/models';
import { FormControllerService } from '../../../core/api/services/form-controller.service';
import { TeamMemberControllerService } from '../../../core/api/services/team-member-controller.service';

@Service()
export class FormsService {
  private formApi = inject(FormControllerService);
  private teamMemberApi = inject(TeamMemberControllerService);

  public list(): Observable<FormSummary[]> {
    return this.formApi.listForms();
  }

  public get(formId: number): Observable<FormDetail> {
    return this.formApi.getForm({ formId });
  }

  public create(body: FormInput): Observable<FormDetail> {
    return this.formApi.createForm({ body });
  }

  public update(formId: number, body: FormInput): Observable<FormDetail> {
    return this.formApi.updateForm({ formId, body });
  }

  public delete(formId: number): Observable<void> {
    return this.formApi.deleteForm({ formId });
  }

  public send(formId: number): Observable<FormDetail> {
    return this.formApi.sendForm({ formId });
  }

  public responses(formId: number): Observable<FormResponse[]> {
    return this.formApi.listFormResponses({ formId });
  }

  public teamMembers(): Observable<TeamMember[]> {
    return this.teamMemberApi.listTeamMembers();
  }
}
