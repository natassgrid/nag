import { inject } from '@angular/core';
import { Route } from '@angular/router';
import { AuthService, authGuard } from '@nag-frontend-workspace/shared-data-access-auth';

export const appRoutes: Route[] = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: () => (inject(AuthService).isAuthenticated() ? 'dashboard' : 'login'),
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./auth/admin-login.component').then((m) => m.AdminLoginComponent),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./dashboard/admin-dashboard.component').then(
        (m) => m.AdminDashboardComponent
      ),
  },
  {
    path: 'questions',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@nag-frontend-workspace/questions-feature-bank').then(
        (m) => m.QuestionsFeatureBank
      ),
  },
  {
    path: 'questions/authoring',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@nag-frontend-workspace/questions-feature-authoring').then(
        (m) => m.QuestionsFeatureAuthoring
      ),
  },
  {
    path: 'assets',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./assets/admin-assets.component').then(
        (m) => m.AdminAssetsComponent
      ),
  },
  {
    path: 'examinations/scheduling',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@nag-frontend-workspace/examinations-feature-scheduling').then(
        (m) => m.ExaminationsFeatureScheduling
      ),
  },
  {
    path: 'examinations/paper-gen',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@nag-frontend-workspace/examinations-feature-paper-gen').then(
        (m) => m.ExaminationsFeaturePaperGen
      ),
  },
  {
    path: 'evaluation/grading',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@nag-frontend-workspace/evaluation-feature-grading').then(
        (m) => m.EvaluationFeatureGrading
      ),
  },
  {
    path: 'reports',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./reports/admin-reports.component').then(
        (m) => m.AdminReportsComponent
      ),
  },
  {
    path: 'audit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./audit/admin-audit-log.component').then(
        (m) => m.AdminAuditLogComponent
      ),
  },
  {
    path: 'users',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./users/admin-user-management.component').then(
        (m) => m.AdminUserManagementComponent
      ),
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./settings/admin-settings.component').then(
        (m) => m.AdminSettingsComponent
      ),
  },
  {
    path: '**',
    redirectTo: () => (inject(AuthService).isAuthenticated() ? 'dashboard' : 'login'),
  },
];
