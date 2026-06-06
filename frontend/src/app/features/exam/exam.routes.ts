import { Routes } from '@angular/router';

export const EXAM_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'delivery',
    pathMatch: 'full'
  },
  {
    path: 'delivery',
    loadComponent: () => import('./exam-delivery/exam-delivery.component').then(m => m.ExamDeliveryComponent)
  },
  {
    path: 'manage',
    loadComponent: () => import('./exam-manage/exam-list.component').then(m => m.ExamListComponent)
  }
];
