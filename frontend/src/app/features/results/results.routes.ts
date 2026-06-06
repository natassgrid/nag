import { Routes } from '@angular/router';

export const RESULTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./result-list/result-list.component').then(m => m.ResultListComponent)
  },
  {
    path: ':examId',
    loadComponent: () => import('./result-detail/result-detail.component').then(m => m.ResultDetailComponent)
  }
];
