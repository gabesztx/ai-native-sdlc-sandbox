import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiConfiguration } from '../api/api-configuration';
import { loginAdmin } from '../api/fn/auth-controller/login-admin';
import { loginMember } from '../api/fn/auth-controller/login-member';
import { logout } from '../api/fn/auth-controller/logout';
import { refreshTokens } from '../api/fn/auth-controller/refresh-tokens';
import { listLoginTeamMembers } from '../api/fn/team-member-controller/list-login-team-members';
import { AuthTokens, TeamMember } from '../api/models';

@Service()
export class AuthApiService {
  private http = inject(HttpClient);
  private config = inject(ApiConfiguration);

  public loginAdmin(email: string, password: string): Observable<AuthTokens> {
    return loginAdmin(this.http, this.config.rootUrl, { body: { email, password } }).pipe(
      map((response) => response.body),
    );
  }

  public loginMember(memberId: number): Observable<AuthTokens> {
    return loginMember(this.http, this.config.rootUrl, { body: { memberId } }).pipe(
      map((response) => response.body),
    );
  }

  public refresh(refreshToken: string): Observable<AuthTokens> {
    return refreshTokens(this.http, this.config.rootUrl, { body: { refreshToken } }).pipe(
      map((response) => response.body),
    );
  }

  public logout(refreshToken: string): Observable<void> {
    return logout(this.http, this.config.rootUrl, { body: { refreshToken } }).pipe(map(() => undefined));
  }

  public teamMembers(): Observable<TeamMember[]> {
    return listLoginTeamMembers(this.http, this.config.rootUrl).pipe(map((response) => response.body));
  }
}
