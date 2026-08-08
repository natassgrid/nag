import { Routes } from '@angular/router';

export const ASSETS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./asset-list.component').then(m => m.AssetListComponent)
  }
];
