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
    data: { roles: ['Super_Admin', 'Security_Admin'] }
  },
  {
    path: '**',
    redirectTo: 'auth/login'
  }
];
