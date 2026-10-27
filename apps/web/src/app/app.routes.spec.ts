import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { adminTokens, memberTokens, storeSession } from '../testing/auth-fixtures';
import { provideApiConfiguration } from './api/api-configuration';
import { routes } from './app.routes';

async function navigate(url: string): Promise<RouterTestingHarness> {
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  return harness;
}

function currentUrl(): string {
  return TestBed.inject(Router).url;
}

function flushPending(backend: HttpTestingController): void {
  backend.match(() => true).forEach((req) => req.flush([]));
}

describe('app.routes (guardokkal, valódi navigációval)', () => {
  let backend: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideApiConfiguration(''),
      ],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  it('AC48: belépés nélkül a /admin/forms megnyitása a /login-ra visz', async () => {
    await navigate('/admin/forms');
    expect(currentUrl()).toBe('/login');
  });

  it('AC48: belépés nélkül a /reviews megnyitása a /login-ra visz', async () => {
    await navigate('/reviews');
    expect(currentUrl()).toBe('/login');
  });

  it('AC49: kollégaként a /admin/forms megnyitása az Értékeléseim oldalra visz', async () => {
    storeSession(memberTokens());

    await navigate('/admin/forms');
    flushPending(backend);

    expect(currentUrl()).toBe('/reviews');
  });

  it('AC49: adminként a /reviews megnyitása az Űrlapok oldalra visz', async () => {
    storeSession(adminTokens());

    await navigate('/reviews');
    flushPending(backend);

    expect(currentUrl()).toBe('/admin/forms');
  });

  it('belépve a /login megnyitása a saját kezdőoldalra visz', async () => {
    storeSession(adminTokens());

    await navigate('/login');
    flushPending(backend);

    expect(currentUrl()).toBe('/admin/forms');
  });

  it('a gyökér (/) belépett adminnál az Űrlapok oldalra visz', async () => {
    storeSession(adminTokens());

    await navigate('/');
    flushPending(backend);

    expect(currentUrl()).toBe('/admin/forms');
  });
});
