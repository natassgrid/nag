import { Route } from '@angular/router';
import { authGuard, rootGuard } from '@nag-frontend-workspace/shared-data-access-auth';

export const appRoutes: Route[] = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [rootGuard],
    loadComponent: () =>
      import('./auth/components/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./auth/components/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./auth/components/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'verify-otp',
    loadComponent: () =>
      import('./auth/components/verify-otp/verify-otp.component').then((m) => m.VerifyOtpComponent),
  },
  {
    path: 'auth',
    loadChildren: () => import('./auth/auth.routes').then((m) => m.authRoutes),
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
    path: 'practice',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./practice/components/practice-hub/practice-hub.component').then(
        (m) => m.PracticeHubComponent
      ),
  },
  {
    path: 'practice/result/:sessionId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./practice/components/practice-result-panel/practice-result-panel.component').then(
        (m) => m.PracticeResultPanelComponent
      ),
  },
  {
    path: 'practice/history',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./practice/components/practice-history/practice-history.component').then(
        (m) => m.PracticeHistoryComponent
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
    path: 'learn',
    canActivate: [authGuard],
    loadComponent: () => import('./learn/learn.component').then(m => m.LearnComponent),
    children: [
      { path: 'recommendations', loadComponent: () => import('./learn/components/recommendations-dashboard/recommendations-dashboard.component').then(m => m.RecommendationsDashboardComponent) },
      { path: '', redirectTo: 'recommendations', pathMatch: 'full' }
    ]
  },
  {
    path: '**',
    canActivate: [rootGuard],
    loadComponent: () =>
      import('./auth/components/login/login.component').then((m) => m.LoginComponent),
  },
];
