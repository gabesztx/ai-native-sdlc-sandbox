import { Component, computed, inject } from '@angular/core';
import { EMPTY, catchError } from 'rxjs';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthApiService } from '../../auth/auth-api.service';
import { AuthStoreService } from '../../auth/auth-store.service';

interface MenuItem {
  label: string;
  path: string;
}

const ADMIN_MENU: MenuItem[] = [
  { label: 'Űrlapok', path: '/admin/forms' },
  { label: 'Új űrlap', path: '/admin/forms/new' },
  { label: 'Csapattagok', path: '/admin/team' },
];

const MEMBER_MENU: MenuItem[] = [{ label: 'Értékeléseim', path: '/reviews' }];

@Component({
  selector: 'shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private store = inject(AuthStoreService);
  private authApi = inject(AuthApiService);
  private router = inject(Router);

  public userName = computed(() => this.store.me()?.name ?? '');
  public menu = computed(() => (this.store.role() === 'ADMIN' ? ADMIN_MENU : MEMBER_MENU));

  public onLogout(): void {
    const refreshToken = this.store.refreshToken();
    const cleanup = (): void => {
      this.store.clear();
      void this.router.navigateByUrl('/login');
    };
    if (!refreshToken) {
      cleanup();
      return;
    }
    this.authApi
      .logout(refreshToken)
      .pipe(catchError(() => EMPTY))
      .subscribe({ complete: cleanup });
  }
}
