import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'users',
    pathMatch: 'full'
  },
  {
    path: 'users',
    loadComponent: () => import('./user-management/user-management.component').then(m => m.UserManagementComponent)
  },
  {
    path: 'analytics',
    loadComponent: () => import('./analytics-dashboard/analytics-dashboard.component').then(m => m.AnalyticsDashboardComponent)
  },
  {
    path: 'reports',
    loadComponent: () => import('./report-export/report-export.component').then(m => m.ReportExportComponent)
  }
];
