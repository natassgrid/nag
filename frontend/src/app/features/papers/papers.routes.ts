import { Routes } from '@angular/router';

export const PAPERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./paper-list.component').then((m) => m.PaperListComponent)
  }
];
