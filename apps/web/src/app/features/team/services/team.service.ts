import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TeamMember } from '../../../core/api/models';
import { TeamMemberControllerService } from '../../../core/api/services/team-member-controller.service';

@Service()
export class TeamService {
  private teamMemberApi = inject(TeamMemberControllerService);

  public list(): Observable<TeamMember[]> {
    return this.teamMemberApi.listTeamMembers();
  }
}
