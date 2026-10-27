import { Routes } from '@angular/router';

export const teamRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/team-page/team-page.component').then((m) => m.TeamPageComponent),
  },
];
