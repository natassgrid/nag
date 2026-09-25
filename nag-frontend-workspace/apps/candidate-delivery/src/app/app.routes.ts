import { Route } from '@angular/router';
import { authGuard } from '@nag-frontend-workspace/shared-data-access-auth';

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
    canActivate: [authGuard],
    loadComponent: () =>
      import('./dashboard/candidate-dashboard.component').then(
        (m) => m.CandidateDashboardComponent
      ),
  },
  {
    path: 'browse',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./browse/browse-exams.component').then(
        (m) => m.BrowseExamsComponent
      ),
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./profile/candidate-profile.component').then(
        (m) => m.CandidateProfileComponent
      ),
  },
  {
    path: 'delivery',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./exam-delivery/exam-delivery.component').then(
        (m) => m.ExamDeliveryComponent
      ),
  },
  {
    path: 'review',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./review/candidate-review.component').then(
        (m) => m.CandidateReviewComponent
      ),
  },
  {
    path: 'results',
    canActivate: [authGuard],
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
