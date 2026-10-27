import { Routes } from '@angular/router';
import { authGuard, guestGuard, homeRedirectGuard } from './core/auth/auth.guard';
import { ShellComponent } from './core/layout/shell/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/pages/login-page/login-page.component').then((m) => m.LoginPageComponent),
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
              import('./features/forms/pages/form-list-page/form-list-page.component').then(
                (m) => m.FormListPageComponent,
              ),
          },
          {
            path: 'forms/new',
            loadComponent: () =>
              import('./features/forms/pages/form-editor-page/form-editor-page.component').then(
                (m) => m.FormEditorPageComponent,
              ),
          },
          {
            path: 'forms/:id',
            loadComponent: () =>
              import('./features/forms/pages/form-detail-page/form-detail-page.component').then(
                (m) => m.FormDetailPageComponent,
              ),
          },
          {
            path: 'forms/:id/edit',
            loadComponent: () =>
              import('./features/forms/pages/form-editor-page/form-editor-page.component').then(
                (m) => m.FormEditorPageComponent,
              ),
          },
          {
            path: 'team',
            loadComponent: () =>
              import('./features/team/pages/team-page/team-page.component').then((m) => m.TeamPageComponent),
          },
          { path: '', pathMatch: 'full', redirectTo: 'forms' },
        ],
      },
      {
        path: 'reviews',
        canActivate: [authGuard('MEMBER')],
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/reviews/pages/review-list-page/review-list-page.component').then(
                (m) => m.ReviewListPageComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/reviews/pages/review-fill-page/review-fill-page.component').then(
                (m) => m.ReviewFillPageComponent,
              ),
          },
        ],
      },
      { path: '', pathMatch: 'full', canActivate: [homeRedirectGuard], children: [] },
    ],
  },
  { path: '**', redirectTo: '' },
];
