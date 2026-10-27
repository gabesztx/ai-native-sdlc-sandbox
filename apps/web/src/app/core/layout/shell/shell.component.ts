import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, filter, map } from 'rxjs';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
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
  imports: [RouterOutlet, RouterLink],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private store = inject(AuthStoreService);
  private authApi = inject(AuthApiService);
  private router = inject(Router);

  public userName = computed(() => this.store.me()?.name ?? '');
  public menu = computed(() => (this.store.role() === 'ADMIN' ? ADMIN_MENU : MEMBER_MENU));

  private currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  // routerLinkActive highlights every link whose path is a prefix of the current URL,
  // so a detail page like /admin/forms/7 would light up both "Űrlapok" (/admin/forms)
  // and, more specifically, "Új űrlap" (/admin/forms/new) has no such ambiguity itself
  // but a sibling could. Picking the longest matching menu path keeps exactly one item
  // highlighted, the most specific one.
  public activePath = computed(() => {
    const url = this.currentUrl();
    return this.menu()
      .filter((item) => isSameOrChildPath(url, item.path))
      .reduce<MenuItem | null>(
        (best, item) => (best === null || item.path.length > best.path.length ? item : best),
        null,
      )?.path;
  });

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

function isSameOrChildPath(url: string, path: string): boolean {
  return url === path || url.startsWith(`${path}/`);
}
