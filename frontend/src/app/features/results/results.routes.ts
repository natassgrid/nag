import { Routes } from '@angular/router';

export const RESULTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./result-page/result-page.component').then(m => m.ResultPageComponent)
  }
];
