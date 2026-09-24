import { Route } from '@angular/router';

export const appRoutes: Route[] = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./auth/admin-login.component').then((m) => m.AdminLoginComponent),
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./dashboard/admin-dashboard.component').then(
        (m) => m.AdminDashboardComponent
      ),
  },
  {
    path: 'questions',
    loadComponent: () =>
      import('@nag-frontend-workspace/questions-feature-bank').then(
        (m) => m.QuestionsFeatureBank
      ),
  },
  {
    path: 'questions/authoring',
    loadComponent: () =>
      import('@nag-frontend-workspace/questions-feature-authoring').then(
        (m) => m.QuestionsFeatureAuthoring
      ),
  },
  {
    path: 'assets',
    loadComponent: () =>
      import('./assets/admin-assets.component').then(
        (m) => m.AdminAssetsComponent
      ),
  },
  {
    path: 'examinations/scheduling',
    loadComponent: () =>
      import('@nag-frontend-workspace/examinations-feature-scheduling').then(
        (m) => m.ExaminationsFeatureScheduling
      ),
  },
  {
    path: 'examinations/paper-gen',
    loadComponent: () =>
      import('@nag-frontend-workspace/examinations-feature-paper-gen').then(
        (m) => m.ExaminationsFeaturePaperGen
      ),
  },
  {
    path: 'evaluation/grading',
    loadComponent: () =>
      import('@nag-frontend-workspace/evaluation-feature-grading').then(
        (m) => m.EvaluationFeatureGrading
      ),
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('./reports/admin-reports.component').then(
        (m) => m.AdminReportsComponent
      ),
  },
  {
    path: 'audit',
    loadComponent: () =>
      import('./audit/admin-audit-log.component').then(
        (m) => m.AdminAuditLogComponent
      ),
  },
  {
    path: 'users',
    loadComponent: () =>
      import('./users/admin-user-management.component').then(
        (m) => m.AdminUserManagementComponent
      ),
  },
  {
    path: 'settings',
    loadComponent: () =>
      import('./settings/admin-settings.component').then(
        (m) => m.AdminSettingsComponent
      ),
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
