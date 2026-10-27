import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideApiConfiguration } from '../../api/api-configuration';
import { adminTokens, memberTokens, storeSession } from '../../../../testing/auth-fixtures';
import { provideUiTesting } from '../../../../testing/ui-providers';
import { authInterceptor } from '../../auth/auth.interceptor';
import { ShellComponent } from './shell.component';

async function render(): Promise<HTMLElement> {
  TestBed.configureTestingModule({
    imports: [ShellComponent],
    providers: [
      provideRouter([]),
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(),
      provideApiConfiguration(''),
      ...provideUiTesting(),
    ],
  });
  const fixture = TestBed.createComponent(ShellComponent);
  await fixture.whenStable();
  return fixture.nativeElement as HTMLElement;
}

function menuLabels(root: HTMLElement): string[] {
  return Array.from(root.querySelectorAll('.menu-item')).map((a) => a.textContent?.trim() ?? '');
}

function activeMenuLabels(root: HTMLElement): string[] {
  return Array.from(root.querySelectorAll('.menu-item.active')).map((a) => a.textContent?.trim() ?? '');
}

async function renderAt(url: string): Promise<HTMLElement> {
  TestBed.configureTestingModule({
    imports: [ShellComponent],
    providers: [
      provideRouter([{ path: '**', children: [] }]),
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(),
      provideApiConfiguration(''),
      ...provideUiTesting(),
    ],
  });
  const fixture = TestBed.createComponent(ShellComponent);
  await TestBed.inject(Router).navigateByUrl(url);
  await fixture.whenStable();
  return fixture.nativeElement as HTMLElement;
}

describe('ShellComponent', () => {
  beforeEach(() => sessionStorage.clear());

  it('AC46: adminként az Űrlapok, Új űrlap, Csapattagok menüpont és a Kilépés látszik', async () => {
    storeSession(adminTokens());
    const root = await render();

    expect(menuLabels(root)).toEqual(['Űrlapok', 'Új űrlap', 'Csapattagok']);
    expect(root.querySelector('.logout')?.textContent).toContain('Kilépés');
    expect(root.textContent).not.toContain('Értékeléseim');
  });

  it('AC47: kollégaként csak az Értékeléseim menüpont és a Kilépés látszik', async () => {
    storeSession(memberTokens());
    const root = await render();

    expect(menuLabels(root)).toEqual(['Értékeléseim']);
    expect(root.querySelector('.user-name')?.textContent).toContain('Teszt Elek');
    expect(root.querySelector('.logout')).not.toBeNull();
  });

  it('részletes (/admin/forms/7) oldalon az Űrlapok marad kiemelve', async () => {
    storeSession(adminTokens());

    expect(activeMenuLabels(await renderAt('/admin/forms/7'))).toEqual(['Űrlapok']);
  });

  it('szerkesztő (/admin/forms/7/edit) oldalon az Űrlapok marad kiemelve', async () => {
    storeSession(adminTokens());

    expect(activeMenuLabels(await renderAt('/admin/forms/7/edit'))).toEqual(['Űrlapok']);
  });

  it('az Új űrlap oldalon az Új űrlap van kiemelve, nem az Űrlapok', async () => {
    storeSession(adminTokens());

    expect(activeMenuLabels(await renderAt('/admin/forms/new'))).toEqual(['Új űrlap']);
  });

  it('a Csapattagok oldalon a Csapattagok van kiemelve', async () => {
    storeSession(adminTokens());

    expect(activeMenuLabels(await renderAt('/admin/team'))).toEqual(['Csapattagok']);
  });

  it('kitöltő oldalon (/reviews/21) az Értékeléseim marad kiemelve', async () => {
    storeSession(memberTokens());

    expect(activeMenuLabels(await renderAt('/reviews/21'))).toEqual(['Értékeléseim']);
  });

  it('AC52: a Kilépés törli a tokeneket és a /login-ra visz', async () => {
    storeSession(memberTokens());
    const root = await render();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    (root.querySelector('.logout') as HTMLButtonElement).click();

    const logout = TestBed.inject(HttpTestingController).expectOne('/api/auth/logout');
    expect(logout.request.headers.get('Authorization')).toBe('Bearer member-access-1');
    logout.flush(null, { status: 204, statusText: 'No Content' });
    expect(sessionStorage.getItem('hugo.auth')).toBeNull();
    expect(navigate).toHaveBeenCalledWith('/login');
  });
});
