import { Routes } from '@angular/router';

export const formsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/form-list-page/form-list-page.component').then((m) => m.FormListPageComponent),
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./pages/form-editor-page/form-editor-page.component').then((m) => m.FormEditorPageComponent),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/form-detail-page/form-detail-page.component').then((m) => m.FormDetailPageComponent),
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./pages/form-editor-page/form-editor-page.component').then((m) => m.FormEditorPageComponent),
  },
];
