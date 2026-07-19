import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'auth/login',
    pathMatch: 'full'
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'profile',
    loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [authGuard]
  },
  {
    path: 'exam',
    loadChildren: () => import('./features/exam/exam.routes').then(m => m.EXAM_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: 'results',
    loadChildren: () => import('./features/results/results.routes').then(m => m.RESULTS_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.ADMIN_ROUTES),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['SUPER_ADMIN', 'SECURITY_ADMIN'] }
  },
  {
    path: 'questions',
    loadChildren: () => import('./features/questions/questions.routes').then(m => m.QUESTIONS_ROUTES),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'SUPER_ADMIN', 'EXAM_CONTROLLER'] }
  },
  {
    path: 'analytics',
    loadComponent: () => import('./features/analytics/analytics-dashboard.component').then(m => m.AnalyticsDashboardComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['EXAM_CONTROLLER'] }
  },
  {
    path: 'notifications',
    loadComponent: () => import('./features/notifications/notification-list.component').then(m => m.NotificationListComponent),
    canActivate: [authGuard]
  },
  {
    path: '**',
    redirectTo: 'auth/login'
  }
];
