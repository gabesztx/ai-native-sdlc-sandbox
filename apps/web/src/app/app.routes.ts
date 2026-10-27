import { Routes } from '@angular/router';
import { authGuard, guestGuard, homeRedirectGuard } from './core/auth/auth.guard';
import { ShellComponent } from './core/layout/shell/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.authRoutes),
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
            loadChildren: () => import('./features/forms/forms.routes').then((m) => m.formsRoutes),
          },
          {
            path: 'team',
            loadChildren: () => import('./features/team/team.routes').then((m) => m.teamRoutes),
          },
          { path: '', pathMatch: 'full', redirectTo: 'forms' },
        ],
      },
      {
        path: 'reviews',
        canActivate: [authGuard('MEMBER')],
        loadChildren: () => import('./features/reviews/reviews.routes').then((m) => m.reviewsRoutes),
      },
      { path: '', pathMatch: 'full', canActivate: [homeRedirectGuard], children: [] },
    ],
  },
  { path: '**', redirectTo: '' },
];
