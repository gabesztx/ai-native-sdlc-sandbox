import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideApiConfiguration } from '../../../api/api-configuration';
import { adminTokens, memberTokens } from '../../../../testing/auth-fixtures';
import { LoginPageComponent } from './login-page.component';

describe('LoginPageComponent', () => {
  let fixture: ComponentFixture<LoginPageComponent>;
  let backend: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideApiConfiguration('')],
    });
    backend = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(LoginPageComponent);
    backend.expectOne('/api/auth/team-members').flush([
      { id: 3, name: 'Teszt Elek' },
      { id: 4, name: 'Minta Anna' },
    ]);
    await fixture.whenStable();
  });

  function root(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function type(selector: string, value: string): void {
    const input = root().querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  it('AC43: a kezdőoldalon látszik az admin belépés és a kolléga választó', () => {
    expect(root().querySelector('#admin-email')).not.toBeNull();
    expect(root().querySelector('#admin-password')).not.toBeNull();
    const options = Array.from(root().querySelectorAll('#member-select option')).map((o) => o.textContent);
    expect(options).toContain('Teszt Elek');
    expect(options).toContain('Minta Anna');
  });

  it('AC50: sikeres admin belépés után az Űrlapok oldalra visz', async () => {
    type('#admin-email', 'admin@test.local');
    type('#admin-password', 'secret');
    (root().querySelectorAll('form')[0] as HTMLFormElement).dispatchEvent(new Event('submit'));

    const login = backend.expectOne('/api/auth/admin/login');
    expect(login.request.body).toEqual({ email: 'admin@test.local', password: 'secret' });
    login.flush(adminTokens());

    expect(navigate).toHaveBeenCalledWith('/admin/forms');
    expect(sessionStorage.getItem('hugo.auth')).toContain('admin-access-1');
  });

  it('AC50: sikeres kolléga belépés után az Értékeléseim oldalra visz', async () => {
    const select = root().querySelector('#member-select') as HTMLSelectElement;
    select.value = select.options[1].value;
    select.dispatchEvent(new Event('change'));
    (root().querySelectorAll('form')[1] as HTMLFormElement).dispatchEvent(new Event('submit'));

    const login = backend.expectOne('/api/auth/member/login');
    expect(login.request.body).toEqual({ memberId: 3 });
    login.flush(memberTokens());

    expect(navigate).toHaveBeenCalledWith('/reviews');
  });

  it('hibás admin adatoknál általános hibaüzenet jelenik meg', async () => {
    type('#admin-email', 'admin@test.local');
    type('#admin-password', 'wrong');
    (root().querySelectorAll('form')[0] as HTMLFormElement).dispatchEvent(new Event('submit'));

    backend.expectOne('/api/auth/admin/login').flush(null, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(root().querySelector('.error')?.textContent).toContain('Hibás e-mail cím vagy jelszó');
    expect(navigate).not.toHaveBeenCalled();
  });
});
