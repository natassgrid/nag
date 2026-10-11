import {
  Component,
  OnInit,
  signal,
  computed,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import {
  PageHeaderComponent,
  StatCardComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { DashboardService } from './services/dashboard.service';
import {
  DashboardSummary,
  DEFAULT_DASHBOARD_SUMMARY,
  SystemServiceHealth,
  SecurityEvent,
} from './models/dashboard.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    PageHeaderComponent,
    StatCardComponent,
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  readonly authService = inject(AuthService);

  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly summary = signal<DashboardSummary>(DEFAULT_DASHBOARD_SUMMARY);

  // Role-tailored computed flags
  readonly isSuperAdmin = computed(() =>
    this.authService.hasAnyRole(['SUPER_ADMIN', 'ADMIN', 'SECURITY_ADMIN'])
  );
  readonly isExamController = computed(() =>
    this.authService.hasRole('EXAM_CONTROLLER') || this.isSuperAdmin()
  );
  readonly isQuestionStaff = computed(() =>
    this.authService.hasAnyRole(['QUESTION_AUTHOR', 'REVIEWER', 'SUBJECT_MATTER_EXPERT']) || this.isSuperAdmin()
  );
  readonly isEvaluator = computed(() =>
    this.authService.hasRole('EVALUATOR') || this.isSuperAdmin()
  );

  // Derived signals for template bindings
  readonly systemServices = computed<SystemServiceHealth[]>(
    () => this.summary().systemServices
  );
  readonly auditEvents = computed<SecurityEvent[]>(
    () => this.summary().recentAuditEvents
  );

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading.set(true);
    this.error.set(null);

    const tenantId = this.authService.getTenantId();
    this.dashboardService.getDashboardSummary(tenantId).subscribe({
      next: (data) => {
        this.summary.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.warn('Error loading dashboard summary:', err);
        this.error.set('Unable to synchronize live metrics. Showing cached state.');
        this.loading.set(false);
      },
    });
  }

  formatNumber(val: number | undefined | null): string {
    if (val === undefined || val === null) return '0';
    return Number(val).toLocaleString();
  }
}
