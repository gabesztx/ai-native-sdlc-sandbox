import { Routes } from '@angular/router';
import { authGuard, guestGuard, homeRedirectGuard } from './core/auth/auth.guard';
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
    children: [
      {
        path: 'admin',
        canActivate: [authGuard('ADMIN')],
        children: [
          {
            path: 'forms',
            loadComponent: () =>
              import('./features/forms/form-list-page/form-list-page.component').then(
                (m) => m.FormListPageComponent,
              ),
          },
          {
            path: 'forms/new',
            loadComponent: () =>
              import('./features/forms/form-editor-page/form-editor-page.component').then(
                (m) => m.FormEditorPageComponent,
              ),
          },
          {
            path: 'forms/:id',
            loadComponent: () =>
              import('./features/forms/form-detail-page/form-detail-page.component').then(
                (m) => m.FormDetailPageComponent,
              ),
          },
          {
            path: 'forms/:id/edit',
            loadComponent: () =>
              import('./features/forms/form-editor-page/form-editor-page.component').then(
                (m) => m.FormEditorPageComponent,
              ),
          },
          {
            path: 'team',
            loadComponent: () =>
              import('./features/team/team-page/team-page.component').then((m) => m.TeamPageComponent),
          },
          { path: '', pathMatch: 'full', redirectTo: 'forms' },
        ],
      },
      { path: '', pathMatch: 'full', canActivate: [homeRedirectGuard], children: [] },
    ],
  },
  { path: '**', redirectTo: '' },
];
