import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideApiConfiguration } from '../../../api/api-configuration';
import { adminTokens, memberTokens, storeSession } from '../../../../testing/auth-fixtures';
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
    ],
  });
  const fixture = TestBed.createComponent(ShellComponent);
  await fixture.whenStable();
  return fixture.nativeElement as HTMLElement;
}

function menuLabels(root: HTMLElement): string[] {
  return Array.from(root.querySelectorAll('.menu-item')).map((a) => a.textContent?.trim() ?? '');
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
