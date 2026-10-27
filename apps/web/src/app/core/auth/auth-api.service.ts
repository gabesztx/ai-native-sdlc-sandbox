import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthTokens } from '../api/models';
import { AuthControllerService } from '../api/services/auth-controller.service';

@Service()
export class AuthApiService {
  private authApi = inject(AuthControllerService);

  public refresh(refreshToken: string): Observable<AuthTokens> {
    return this.authApi.refreshTokens({ body: { refreshToken } });
  }

  public logout(refreshToken: string): Observable<void> {
    return this.authApi.logout({ body: { refreshToken } });
  }
}
