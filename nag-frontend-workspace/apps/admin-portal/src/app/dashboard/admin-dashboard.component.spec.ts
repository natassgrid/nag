import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { AdminDashboardComponent } from './admin-dashboard.component';
import { DashboardService } from './services/dashboard.service';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { DEFAULT_DASHBOARD_SUMMARY, DashboardSummary } from './models/dashboard.model';

describe('AdminDashboardComponent', () => {
  let component: AdminDashboardComponent;
  let fixture: ComponentFixture<AdminDashboardComponent>;
  let mockDashboardService: {
    getDashboardSummary: jest.Mock;
  };
  let mockAuthService: {
    hasRole: jest.Mock;
    hasAnyRole: jest.Mock;
    getTenantId: jest.Mock;
  };

  const testSummary: DashboardSummary = {
    ...DEFAULT_DASHBOARD_SUMMARY,
    kpis: {
      ...DEFAULT_DASHBOARD_SUMMARY.kpis,
      totalQuestions: 52000,
      activeExaminations: 18,
      pendingGradingTasks: 150,
    },
  };

  beforeEach(async () => {
    mockDashboardService = {
      getDashboardSummary: jest.fn().mockReturnValue(of(testSummary)),
    };

    mockAuthService = {
      hasRole: jest.fn().mockReturnValue(false),
      hasAnyRole: jest.fn().mockReturnValue(false),
      getTenantId: jest.fn().mockReturnValue('default'),
    };

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, AdminDashboardComponent],
      providers: [
        { provide: DashboardService, useValue: mockDashboardService },
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create admin dashboard and load summary data on init', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(mockDashboardService.getDashboardSummary).toHaveBeenCalledWith('default');
    expect(component.summary().kpis.totalQuestions).toBe(52000);
    expect(component.summary().kpis.activeExaminations).toBe(18);
  });

  it('should format numbers with locale delimiters', () => {
    expect(component.formatNumber(1480200)).toBe('1,480,200');
    expect(component.formatNumber(null)).toBe('0');
    expect(component.formatNumber(undefined)).toBe('0');
  });

  it('should render correct telemetry health and security audit count', () => {
    fixture.detectChanges();
    expect(component.systemServices().length).toBeGreaterThan(0);
    expect(component.auditEvents().length).toBeGreaterThan(0);
  });

  it('should correctly compute role flags for SUPER_ADMIN', () => {
    mockAuthService.hasAnyRole.mockImplementation((roles: string[]) =>
      roles.includes('SUPER_ADMIN')
    );
    mockAuthService.hasRole.mockImplementation((role: string) =>
      role === 'SUPER_ADMIN'
    );

    fixture.detectChanges();

    expect(component.isSuperAdmin()).toBe(true);
    expect(component.isExamController()).toBe(true);
    expect(component.isQuestionStaff()).toBe(true);
    expect(component.isEvaluator()).toBe(true);
  });

  it('should correctly compute role flags for EXAM_CONTROLLER', () => {
    mockAuthService.hasAnyRole.mockReturnValue(false);
    mockAuthService.hasRole.mockImplementation((role: string) =>
      role === 'EXAM_CONTROLLER'
    );

    fixture.detectChanges();

    expect(component.isSuperAdmin()).toBe(false);
    expect(component.isExamController()).toBe(true);
  });

  it('should correctly compute role flags for QUESTION_AUTHOR', () => {
    mockAuthService.hasAnyRole.mockImplementation((roles: string[]) =>
      roles.includes('QUESTION_AUTHOR')
    );
    mockAuthService.hasRole.mockReturnValue(false);

    fixture.detectChanges();

    expect(component.isQuestionStaff()).toBe(true);
    expect(component.isSuperAdmin()).toBe(false);
  });

  it('should handle service errors gracefully and set error signal', () => {
    mockDashboardService.getDashboardSummary.mockReturnValue(
      throwError(() => new Error('Network error'))
    );

    component.loadDashboardData();

    expect(component.loading()).toBe(false);
    expect(component.error()).toContain('Unable to synchronize live metrics');
  });

  it('should refresh dashboard data on demand', () => {
    fixture.detectChanges();
    mockDashboardService.getDashboardSummary.mockClear();

    component.loadDashboardData();

    expect(mockDashboardService.getDashboardSummary).toHaveBeenCalledTimes(1);
  });
});
