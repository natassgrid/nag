import { Route } from '@angular/router';
import { authGuard, rootGuard } from '@nag-frontend-workspace/shared-data-access-auth';

export const appRoutes: Route[] = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [rootGuard],
    loadComponent: () =>
      import('./auth/admin-login.component').then((m) => m.AdminLoginComponent),
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
    path: 'questions/taxonomy',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./questions/taxonomy/admin-subject-management.component').then(
        (m) => m.AdminSubjectManagementComponent
      ),
  },
  {
    path: 'questions/subjects',
    redirectTo: 'questions/taxonomy',
    pathMatch: 'full',
  },
  {
    path: 'questions/ai-generate',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./questions/ai-generation/admin-ai-question-generation.component').then(
        (m) => m.AdminAiQuestionGenerationComponent
      ),
  },
  {
    path: 'questions/blueprints',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./questions/blueprints/admin-blueprint-management.component').then(
        (m) => m.AdminBlueprintManagementComponent
      ),
  },
  {
    path: 'questions/translations',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./questions/translations/admin-question-translation.component').then(
        (m) => m.AdminQuestionTranslationComponent
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
    path: 'examinations/paper-gen',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@nag-frontend-workspace/examinations-feature-paper-gen').then(
        (m) => m.ExaminationsFeaturePaperGen
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
    path: 'evaluation/grading',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@nag-frontend-workspace/evaluation-feature-grading').then(
        (m) => m.EvaluationFeatureGrading
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
    path: '**',
    redirectTo: '',
  },
];
