import { Routes } from '@angular/router';

export const reviewsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/review-list-page/review-list-page.component').then((m) => m.ReviewListPageComponent),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/review-fill-page/review-fill-page.component').then((m) => m.ReviewFillPageComponent),
  },
];
