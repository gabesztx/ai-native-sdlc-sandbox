import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, filter, map } from 'rxjs';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { Badge } from 'primeng/badge';
import { ButtonDirective, ButtonIcon, ButtonLabel } from 'primeng/button';
import { MemberAvatarComponent } from '../../../shared/components/member-avatar/member-avatar.component';
import { ThemeToggleComponent } from '../../../shared/components/theme-toggle/theme-toggle.component';
import { AuthApiService } from '../../auth/auth-api.service';
import { AuthStoreService } from '../../auth/auth-store.service';
import { BreakpointService } from '../breakpoint.service';
import { MenuBadgesService } from '../menu-badges.service';

interface MenuItem {
  label: string;
  path: string;
  icon: string;
  badgeUnit?: string;
}

const DASHBOARD_MENU_ITEM: MenuItem = { label: 'Dashboard', path: '/dashboard', icon: 'pi pi-chart-bar' };

const ADMIN_MENU: MenuItem[] = [
  { label: 'Űrlapok', path: '/admin/forms', icon: 'pi pi-file', badgeUnit: 'piszkozat' },
  { label: 'Új űrlap', path: '/admin/forms/new', icon: 'pi pi-plus' },
  { label: 'Csapattagok', path: '/admin/team', icon: 'pi pi-users' },
  DASHBOARD_MENU_ITEM,
];

const MEMBER_MENU: MenuItem[] = [
  { label: 'Értékeléseim', path: '/reviews', icon: 'pi pi-inbox', badgeUnit: 'kitöltendő' },
  DASHBOARD_MENU_ITEM,
];

@Component({
  selector: 'shell',
  imports: [
    RouterOutlet,
    RouterLink,
    NgTemplateOutlet,
    Badge,
    ButtonDirective,
    ButtonIcon,
    ButtonLabel,
    MemberAvatarComponent,
    ThemeToggleComponent,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private store = inject(AuthStoreService);
  private authApi = inject(AuthApiService);
  private router = inject(Router);
  private badges = inject(MenuBadgesService);

  public isMobile = inject(BreakpointService).isMobile;
  public userName = computed(() => this.store.me()?.name ?? '');
  public menu = computed(() => (this.store.role() === 'ADMIN' ? ADMIN_MENU : MEMBER_MENU));
  public showBottomNav = computed(() => this.isMobile() && this.menu().length > 1);
  public headerBadgeItem = computed(() =>
    this.isMobile() && !this.showBottomNav() ? (this.menu().find((item) => item.badgeUnit) ?? null) : null,
  );

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

  public badgeCount(item: MenuItem): number | null {
    const count = this.badges.count();
    return item.badgeUnit && count !== null && count > 0 ? count : null;
  }

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
