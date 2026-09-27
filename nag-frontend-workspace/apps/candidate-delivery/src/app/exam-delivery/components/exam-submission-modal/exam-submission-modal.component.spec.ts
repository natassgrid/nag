import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ExamSubmissionModalComponent } from './exam-submission-modal.component';
import { ExamSubmissionReceipt } from '../../models';

describe('ExamSubmissionModalComponent', () => {
  let component: ExamSubmissionModalComponent;
  let fixture: ComponentFixture<ExamSubmissionModalComponent>;

  const mockReceipt: ExamSubmissionReceipt = {
    submissionId: 'sub-001',
    timestamp: new Date().toISOString(),
    totalAnswered: 45,
    totalQuestions: 50,
    digitalSignature: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExamSubmissionModalComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ExamSubmissionModalComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('receipt', mockReceipt);
    fixture.detectChanges();
  });

  it('should render submission receipt details', () => {
    expect(component).toBeTruthy();
    expect(component.receipt().totalAnswered).toBe(45);
    expect(component.receipt().submissionId).toBe('sub-001');
  });

  it('should emit close on modal dismiss', () => {
    let closed = false;
    component.close.subscribe(() => (closed = true));
    component.onClose();
    expect(closed).toBe(true);
  });
});
