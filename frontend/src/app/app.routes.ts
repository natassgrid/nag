/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
    data: { roles: ['SUPER_ADMIN', 'SECURITY_ADMIN', 'AUDITOR'] }
  },
  {
    path: 'questions',
    loadChildren: () => import('./features/questions/questions.routes').then(m => m.QUESTIONS_ROUTES),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'SUPER_ADMIN', 'EXAM_CONTROLLER', 'TRANSLATOR'] }
  },
  {
    path: 'papers',
    loadChildren: () => import('./features/papers/papers.routes').then(m => m.PAPERS_ROUTES),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] }
  },
  {
    path: 'analytics',
    loadComponent: () => import('./features/analytics/analytics-dashboard.component').then(m => m.AnalyticsDashboardComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['EXAM_CONTROLLER'] }
  },
  {
    path: 'assets',
    loadChildren: () => import('./features/assets/assets.routes').then(m => m.ASSETS_ROUTES),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['QUESTION_AUTHOR', 'CONTENT_MANAGER', 'SUPER_ADMIN', 'ADMIN'] }
  },
  {
    path: 'notifications',
    loadComponent: () => import('./features/notifications/notification-list.component').then(m => m.NotificationListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'evaluations',
    loadComponent: () => import('./features/evaluations/evaluation-list.component').then(m => m.EvaluationListComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['EVALUATOR'] }
  },
  {
    path: 'preferences',
    loadComponent: () => import('./features/preferences/preferences.component').then(m => m.PreferencesComponent),
    canActivate: [authGuard]
  },
  {
    path: '**',
    redirectTo: 'auth/login'
  }
];
