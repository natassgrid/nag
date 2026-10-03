import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { CandidateDashboardComponent } from './candidate-dashboard.component';
import { CandidateDashboardService, DEFAULT_ENROLLED_EXAMS } from './services';

describe('CandidateDashboardComponent', () => {
  let component: CandidateDashboardComponent;
  let fixture: ComponentFixture<CandidateDashboardComponent>;
  let dashboardService: CandidateDashboardService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CandidateDashboardComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [provideRouter([]), CandidateDashboardService],
    }).compileComponents();

    fixture = TestBed.createComponent(CandidateDashboardComponent);
    component = fixture.componentInstance;
    dashboardService = TestBed.inject(CandidateDashboardService);
    jest.spyOn(dashboardService, 'loadEnrolledExams').mockReturnValue(of(DEFAULT_ENROLLED_EXAMS));
    dashboardService.enrolledExams.set(DEFAULT_ENROLLED_EXAMS);
    fixture.detectChanges();
  });

  it('should create the candidate dashboard component', () => {
    expect(component).toBeTruthy();
  });

  it('should filter exams by tab filter', () => {
    component.setFilter('LIVE');
    const liveExams = component.filteredExams();
    expect(liveExams.every((e) => e.status === 'LIVE')).toBe(true);

    component.setFilter('UPCOMING');
    const upcoming = component.filteredExams();
    expect(upcoming.every((e) => e.status === 'UPCOMING' || e.status === 'SCHEDULED')).toBe(true);

    component.setFilter('ALL');
    expect(component.filteredExams().length).toBe(DEFAULT_ENROLLED_EXAMS.length);
  });

  it('should open and close digital admit card dialog', () => {
    const exam = DEFAULT_ENROLLED_EXAMS[0];
    jest.spyOn(dashboardService, 'getAdmitCard').mockReturnValue(of({
      applicationId: exam.applicationId,
      examId: exam.id,
      examCode: exam.code,
      examTitle: exam.title,
      conductingAuthority: exam.conductingAuthority,
      candidateName: 'Aryan Sharma',
      rollNumber: exam.rollNumber,
      candidateCategory: 'General',
      scheduledDate: exam.scheduledDate,
      reportingTime: '07:45 AM',
      gateClosingTime: '08:30 AM',
      examTime: exam.scheduledTime,
      durationMinutes: exam.durationMinutes,
      centerName: exam.centerName,
      centerAddress: exam.centerAddress,
      centerCode: 'CTR-01',
      qrVerificationToken: 'TOKEN',
      instructions: [],
    }));

    component.openAdmitCard(exam);
    expect(component.selectedAdmitCard()).toBeTruthy();

    component.closeAdmitCard();
    expect(component.selectedAdmitCard()).toBeNull();
  });
});
