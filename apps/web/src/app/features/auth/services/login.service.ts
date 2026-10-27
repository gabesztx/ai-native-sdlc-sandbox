import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthTokens, TeamMember } from '../../../core/api/models';
import { AuthControllerService } from '../../../core/api/services/auth-controller.service';
import { TeamMemberControllerService } from '../../../core/api/services/team-member-controller.service';

@Service()
export class LoginService {
  private authApi = inject(AuthControllerService);
  private teamMemberApi = inject(TeamMemberControllerService);

  public loginAdmin(email: string, password: string): Observable<AuthTokens> {
    return this.authApi.loginAdmin({ body: { email, password } });
  }

  public loginMember(memberId: number): Observable<AuthTokens> {
    return this.authApi.loginMember({ body: { memberId } });
  }

  public teamMembers(): Observable<TeamMember[]> {
    return this.teamMemberApi.listLoginTeamMembers();
  }
}
