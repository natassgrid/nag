import { Routes } from '@angular/router';

export const SCHEDULING_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./schedule-list.component').then(m => m.ScheduleListComponent)
  },
  {
    path: 'centres',
    loadComponent: () => import('./centre-list.component').then(m => m.CentreListComponent)
  },
  {
    path: ':examId',
    loadComponent: () => import('./schedule-detail.component').then(m => m.ScheduleDetailComponent)
  }
];
