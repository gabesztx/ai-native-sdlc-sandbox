import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiConfiguration } from '../../api/api-configuration';
import { listTeamMembers } from '../../api/fn/team-member-controller/list-team-members';
import { TeamMember } from '../../api/models';

@Service()
export class TeamApiService {
  private http = inject(HttpClient);
  private config = inject(ApiConfiguration);

  public list(): Observable<TeamMember[]> {
    return listTeamMembers(this.http, this.config.rootUrl).pipe(map((response) => response.body));
  }
}
