import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ExamDetailsDrawerComponent } from './exam-details-drawer.component';
import { CatalogExam } from '../../models';

describe('ExamDetailsDrawerComponent', () => {
  let component: ExamDetailsDrawerComponent;
  let fixture: ComponentFixture<ExamDetailsDrawerComponent>;

  const mockExam: CatalogExam = {
    id: 'exam-1',
    code: 'UPSC-CSE-2026',
    title: 'Civil Services Examination',
    conductingAuthority: 'Union Public Service Commission',
    category: 'CIVIL_SERVICES',
    durationMinutes: 120,
    totalMarks: 200,
    negativeMarkingEnabled: true,
    negativeMarkingValue: 0.33,
    applicationDeadline: '2026-03-31',
    examDate: '2026-05-24',
    feeAmount: 100,
    eligibility: 'Bachelor Degree in any discipline from a recognized university.',
    totalSeats: 1056,
    status: 'OPEN',
    applied: false,
    sections: [
      {
        name: 'General Studies I',
        durationMinutes: 120,
        totalQuestions: 100,
        totalMarks: 200,
        cutoffMarks: 66,
      },
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExamDetailsDrawerComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ExamDetailsDrawerComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('exam', mockExam);
    fixture.detectChanges();
  });

  it('should create the details drawer', () => {
    expect(component).toBeTruthy();
  });

  it('should display exam details', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('UPSC-CSE-2026');
    expect(el.textContent).toContain('Civil Services Examination');
  });

  it('should emit closeDrawer on escape key', () => {
    const spy = jest.spyOn(component.closeDrawer, 'emit');
    component.handleEscapeKey();
    expect(spy).toHaveBeenCalled();
  });

  it('should emit applyExam when clicking proceed to application', () => {
    const spy = jest.spyOn(component.applyExam, 'emit');
    const applyButton = fixture.nativeElement.querySelector('button[color="primary"]');
    applyButton?.click();
    expect(spy).toHaveBeenCalledWith(mockExam);
  });
});
