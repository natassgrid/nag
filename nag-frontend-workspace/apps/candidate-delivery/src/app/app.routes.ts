import { Route } from '@angular/router';

export const appRoutes: Route[] = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./auth/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'verify-otp',
    loadComponent: () =>
      import('./auth/verify-otp.component').then((m) => m.VerifyOtpComponent),
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./dashboard/candidate-dashboard.component').then(
        (m) => m.CandidateDashboardComponent
      ),
  },
  {
    path: 'browse',
    loadComponent: () =>
      import('./browse/browse-exams.component').then(
        (m) => m.BrowseExamsComponent
      ),
  },
  {
    path: 'profile',
    loadComponent: () =>
      import('./profile/candidate-profile.component').then(
        (m) => m.CandidateProfileComponent
      ),
  },
  {
    path: 'delivery',
    loadComponent: () =>
      import('./exam-delivery/exam-delivery.component').then(
        (m) => m.ExamDeliveryComponent
      ),
  },
  {
    path: 'review',
    loadComponent: () =>
      import('./review/candidate-review.component').then(
        (m) => m.CandidateReviewComponent
      ),
  },
  {
    path: 'results',
    loadComponent: () =>
      import('./results/candidate-results.component').then(
        (m) => m.CandidateResultsComponent
      ),
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
