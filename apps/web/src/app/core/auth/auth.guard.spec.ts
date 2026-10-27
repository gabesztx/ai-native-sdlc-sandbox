import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { adminTokens, memberTokens, storeSession } from '../../../testing/auth-fixtures';
import { authGuard, guestGuard } from './auth.guard';

function run(guard: ReturnType<typeof authGuard>): ReturnType<ReturnType<typeof authGuard>> {
  return TestBed.runInInjectionContext(() =>
    guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
  );
}

function urlOf(result: unknown): string {
  return TestBed.inject(Router).serializeUrl(result as UrlTree);
}

describe('authGuard', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  it('AC48: belépés nélkül az admin és a kolléga oldal is a /login-ra visz', () => {
    expect(urlOf(run(authGuard('ADMIN')))).toBe('/login');
    expect(urlOf(run(authGuard('MEMBER')))).toBe('/login');
  });

  it('AC49: kollégaként az admin oldal az Értékeléseim oldalra visz', () => {
    storeSession(memberTokens());

    expect(urlOf(run(authGuard('ADMIN')))).toBe('/reviews');
    expect(run(authGuard('MEMBER'))).toBe(true);
  });

  it('AC49: adminként a kolléga oldal az Űrlapok oldalra visz', () => {
    storeSession(adminTokens());

    expect(urlOf(run(authGuard('MEMBER')))).toBe('/admin/forms');
    expect(run(authGuard('ADMIN'))).toBe(true);
  });

  it('belépve a belépőoldal a saját kezdőoldalra visz', () => {
    storeSession(adminTokens());

    expect(urlOf(run(guestGuard))).toBe('/admin/forms');
  });
});
