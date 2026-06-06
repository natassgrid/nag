import { Routes } from '@angular/router';

export const QUESTIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./question-list.component').then(m => m.QuestionListComponent)
  },
  {
    path: 'review',
    loadComponent: () =>
      import('./question-review.component').then(m => m.QuestionReviewComponent)
  },
  {
    path: 'subjects',
    loadComponent: () =>
      import('./subject-management.component').then(m => m.SubjectManagementComponent)
  }
];
