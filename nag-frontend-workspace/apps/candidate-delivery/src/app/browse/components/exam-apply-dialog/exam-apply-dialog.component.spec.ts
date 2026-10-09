import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ExamApplyDialogComponent } from './exam-apply-dialog.component';
import { CandidateBrowseService } from '../../services';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';
import { CatalogExam, PublicCentre, ApplicationReceipt } from '../../models';
import { signal } from '@angular/core';

describe('ExamApplyDialogComponent', () => {
  let component: ExamApplyDialogComponent;
  let fixture: ComponentFixture<ExamApplyDialogComponent>;
  let mockBrowseService: {
    applyForExam: jest.Mock;
    catalog: any;
  };
  let mockAuthService: {
    currentUser: jest.Mock;
  };
  let mockNotificationService: {
    success: jest.Mock;
    error: jest.Mock;
  };

  const sampleExam: CatalogExam = {
    id: 'exam-uuid-123',
    code: 'NES-2026',
    title: 'National Eligibility Screening',
    conductingAuthority: 'NTA',
    category: 'ENGINEERING',
    durationMinutes: 180,
    totalMarks: 300,
    negativeMarkingEnabled: true,
    negativeMarkingValue: 0.25,
    applicationDeadline: '2026-10-15',
    examDate: '2026-10-28',
    feeAmount: 500,
    eligibility: 'B.Tech',
    totalSeats: 1000,
    status: 'OPEN',
    applied: false,
  };

  const sampleCentres: PublicCentre[] = [
    {
      id: 'centre-1111-2222',
      centreName: 'IIT Delhi Exam Centre',
      region: 'North',
      state: 'Delhi',
      district: 'New Delhi',
      city: 'New Delhi',
    },
    {
      id: 'centre-3333-4444',
      centreName: 'VJTI Mumbai Exam Centre',
      region: 'West',
      state: 'Maharashtra',
      district: 'Mumbai',
      city: 'Mumbai',
    },
  ];

  beforeEach(async () => {
    mockBrowseService = {
      applyForExam: jest.fn(),
      catalog: signal([sampleExam]),
    };

    mockAuthService = {
      currentUser: jest.fn().mockReturnValue({
        userId: 'cand-user-1',
        username: 'aryan.sharma@gov.in',
        roles: ['CANDIDATE'],
      }),
    };

    mockNotificationService = {
      success: jest.fn(),
      error: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ExamApplyDialogComponent],
      providers: [
        { provide: CandidateBrowseService, useValue: mockBrowseService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: NotificationService, useValue: mockNotificationService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExamApplyDialogComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('exam', sampleExam);
    fixture.componentRef.setInput('centres', sampleCentres);
    fixture.detectChanges();
  });

  it('should initialize and display Step 1', () => {
    expect(component).toBeTruthy();
    expect(component.currentStep()).toBe(1);
    expect(component.firstChoiceCentreId()).toBe('centre-1111-2222');
  });

  it('should navigate through steps to Step 3', () => {
    component.goToNextStep();
    expect(component.currentStep()).toBe(2);

    component.goToNextStep();
    expect(component.currentStep()).toBe(3);
  });

  it('should submit registration to backend on Confirm and Pay and skip payment flow', () => {
    const backendReceipt: ApplicationReceipt = {
      applicationId: 'APP-100',
      applicationNumber: 'NAG-GATE-999',
      examId: sampleExam.id,
      examTitle: sampleExam.title,
      examCode: sampleExam.code,
      candidateName: 'aryan.sharma@gov.in',
      candidateEmail: 'aryan.sharma@gov.in',
      category: 'GENERAL',
      appliedAt: new Date().toISOString(),
      feePaid: 500,
      firstChoiceCentreName: 'IIT Delhi Exam Centre',
      pwdAssistance: false,
      status: 'CONFIRMED',
    };

    mockBrowseService.applyForExam.mockReturnValue(of(backendReceipt));

    component.currentStep.set(3);
    component.submitApplication();

    expect(mockBrowseService.applyForExam).toHaveBeenCalledWith(
      sampleExam.id,
      expect.objectContaining({
        firstChoiceCentreId: 'centre-1111-2222',
        pwdRequired: false,
      })
    );
    expect(component.currentStep()).toBe(4);
    expect(component.submissionReceipt()?.applicationNumber).toBe('NAG-GATE-999');
    expect(mockNotificationService.success).toHaveBeenCalledWith(
      'Application Submitted Successfully',
      expect.stringContaining('NAG-GATE-999')
    );
  });

  it('should handle backend error gracefully with confirmed fallback receipt', () => {
    mockBrowseService.applyForExam.mockReturnValue(throwError(() => new Error('Network error')));

    component.currentStep.set(3);
    component.submitApplication();

    expect(component.currentStep()).toBe(4);
    expect(component.submissionReceipt()).toBeTruthy();
    expect(component.submissionReceipt()?.status).toBe('CONFIRMED');
    expect(mockNotificationService.success).toHaveBeenCalled();
  });
});
