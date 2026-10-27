import { Routes } from '@angular/router';
import { guestGuard, homeRedirectGuard } from './core/auth/auth.guard';
import { ShellComponent } from './core/layout/shell/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login-page/login-page.component').then((m) => m.LoginPageComponent),
  },
  {
    path: '',
    component: ShellComponent,
    children: [{ path: '', pathMatch: 'full', canActivate: [homeRedirectGuard], children: [] }],
  },
  { path: '**', redirectTo: '' },
];
