import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
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
    dashboardService.enrolledExams.set(DEFAULT_ENROLLED_EXAMS);
    fixture.detectChanges();
  });

  it('should create the candidate dashboard component', () => {
    expect(component).toBeTruthy();
  });

  it('should filter exams by tab filter', () => {
    component.setFilter('LIVE');
    const liveExams = component.filteredExams();
    expect(liveExams.every((e) => e.status === 'LIVE')).toBeTrue();

    component.setFilter('UPCOMING');
    const upcoming = component.filteredExams();
    expect(upcoming.every((e) => e.status === 'UPCOMING' || e.status === 'SCHEDULED')).toBeTrue();

    component.setFilter('ALL');
    expect(component.filteredExams().length).toBe(DEFAULT_ENROLLED_EXAMS.length);
  });

  it('should open and close digital admit card dialog', () => {
    const exam = DEFAULT_ENROLLED_EXAMS[0];
    component.openAdmitCard(exam);
    expect(component.selectedAdmitCard()).toBeTruthy();

    component.closeAdmitCard();
    expect(component.selectedAdmitCard()).toBeNull();
  });
});
