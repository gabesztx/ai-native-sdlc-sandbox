import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideApiConfiguration } from '../../api/api-configuration';
import { adminTokens, storeSession } from '../../../testing/auth-fixtures';
import { AuthStoreService } from './auth-store.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    storeSession(adminTokens('old'));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideApiConfiguration(''),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('sends the access token as a Bearer header', () => {
    http.get('/api/admin/forms').subscribe();

    const req = backend.expectOne('/api/admin/forms');
    expect(req.request.headers.get('Authorization')).toBe('Bearer admin-access-old');
    req.flush([]);
  });

  it('AC51: lejárt access tokennél egyetlen refresh fut, és az eredeti kérések megismétlődnek', () => {
    const results: unknown[] = [];
    http.get('/api/admin/forms').subscribe((body) => results.push(body));
    http.get('/api/admin/team-members').subscribe((body) => results.push(body));

    backend.expectOne('/api/admin/forms').flush(null, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/admin/team-members').flush(null, { status: 401, statusText: 'Unauthorized' });

    const refresh = backend.expectOne('/api/auth/refresh');
    expect(refresh.request.body).toEqual({ refreshToken: 'admin-refresh-old' });
    refresh.flush(adminTokens('new'));

    const retriedForms = backend.expectOne('/api/admin/forms');
    const retriedTeam = backend.expectOne('/api/admin/team-members');
    expect(retriedForms.request.headers.get('Authorization')).toBe('Bearer admin-access-new');
    retriedForms.flush(['forms']);
    retriedTeam.flush(['team']);

    expect(results).toEqual([['forms'], ['team']]);
    expect(TestBed.inject(AuthStoreService).refreshToken()).toBe('admin-refresh-new');
  });

  it('sikertelen refresh után kijelentkeztet és a /login-ra visz', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    http.get('/api/admin/forms').subscribe({ next: () => undefined, error: () => undefined });

    backend.expectOne('/api/admin/forms').flush(null, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/auth/refresh').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(TestBed.inject(AuthStoreService).isLoggedIn()).toBe(false);
    expect(navigate).toHaveBeenCalledWith('/login');
  });
});
