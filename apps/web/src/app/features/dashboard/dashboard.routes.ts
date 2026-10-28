import { Routes } from '@angular/router';

export const dashboardRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/dashboard-page/dashboard-page.component').then((m) => m.DashboardPageComponent),
    children: [
      {
        path: ':tileSegment',
        loadComponent: () =>
          import('./pages/dashboard-list-page/dashboard-list-page.component').then((m) => m.DashboardListPageComponent),
      },
    ],
  },
];
