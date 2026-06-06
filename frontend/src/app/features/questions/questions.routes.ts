import { Routes } from '@angular/router';

export const QUESTIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./question-list.component').then(m => m.QuestionListComponent)
  }
];
