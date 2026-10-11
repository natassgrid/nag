import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QuestionTranslationTableComponent } from './question-translation-table.component';
import { Question } from '@nag-frontend-workspace/questions-data-access';
import { PageEvent } from '@angular/material/paginator';

describe('QuestionTranslationTableComponent', () => {
  let component: QuestionTranslationTableComponent;
  let fixture: ComponentFixture<QuestionTranslationTableComponent>;

  const mockQuestion: Question = {
    id: 'q-12345678-aaaa-bbbb-cccc-dddddddddddd',
    code: 'Q-TEST-001',
    content: 'What is the speed of light?',
    type: 'SINGLE_MCQ',
    difficulty: 'MEDIUM',
    status: 'APPROVED',
    subject: 'Physics',
    topic: 'Optics',
    marks: 4,
    negativeMarks: 1,
    options: [
      { id: 'A', text: '3x10^8 m/s', isCorrect: true },
      { id: 'B', text: '2x10^8 m/s', isCorrect: false },
    ],
    translationStatus: 'DRAFT',
    translationStatusMap: {
      hi: 'APPROVED',
      ta: 'DRAFT',
      bn: 'PUBLISHED',
    },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuestionTranslationTableComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(QuestionTranslationTableComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('questions', [mockQuestion]);
    fixture.componentRef.setInput('totalElements', 1);
    fixture.componentRef.setInput('page', 0);
    fixture.componentRef.setInput('pageSize', 20);
    fixture.componentRef.setInput('activeLanguageName', 'Hindi (हिंदी)');
    fixture.componentRef.setInput('selectedLanguage', 'hi');
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should emit pageChange when paginator event occurs', () => {
    jest.spyOn(component.pageChange, 'emit');

    const event: PageEvent = {
      pageIndex: 2,
      pageSize: 25,
      length: 100,
    };
    component.onPageChange(event);

    expect(component.pageChange.emit).toHaveBeenCalledWith({
      pageIndex: 2,
      pageSize: 25,
    });
  });

  it('should resolve question status from translationStatusMap for selected language', () => {
    expect(component.getQuestionTranslationStatus(mockQuestion)).toBe('APPROVED');

    fixture.componentRef.setInput('selectedLanguage', 'ta');
    fixture.detectChanges();
    expect(component.getQuestionTranslationStatus(mockQuestion)).toBe('DRAFT');

    fixture.componentRef.setInput('selectedLanguage', 'bn');
    fixture.detectChanges();
    expect(component.getQuestionTranslationStatus(mockQuestion)).toBe('PUBLISHED');
  });

  it('should fallback to question.translationStatus or READY_FOR_AI when map entry is missing', () => {
    fixture.componentRef.setInput('selectedLanguage', 'mr');
    fixture.detectChanges();
    // In mockQuestion, translationStatus is 'DRAFT'
    expect(component.getQuestionTranslationStatus(mockQuestion)).toBe('DRAFT');

    const unmappedQuestion: Question = {
      ...mockQuestion,
      id: 'q-unmapped',
      translationStatus: undefined,
      translationStatusMap: undefined,
    };
    expect(component.getQuestionTranslationStatus(unmappedQuestion)).toBe('READY_FOR_AI');
  });

  it('should return correct badge styling classes for statuses', () => {
    expect(component.getStatusBadgeClass('PUBLISHED')).toContain('text-emerald-700');
    expect(component.getStatusBadgeClass('APPROVED')).toContain('text-teal-700');
    expect(component.getStatusBadgeClass('IN_REVIEW')).toContain('text-violet-700');
    expect(component.getStatusBadgeClass('DRAFT')).toContain('text-sky-700');
    expect(component.getStatusBadgeClass('REJECTED')).toContain('text-rose-700');
    expect(component.getStatusBadgeClass('STALE')).toContain('text-orange-700');
    expect(component.getStatusBadgeClass('READY_FOR_AI')).toContain('text-amber-700');
  });

  it('should return human-friendly labels for statuses', () => {
    expect(component.getStatusLabel('PUBLISHED')).toBe('Published');
    expect(component.getStatusLabel('APPROVED')).toBe('Approved');
    expect(component.getStatusLabel('IN_REVIEW')).toBe('In Review');
    expect(component.getStatusLabel('DRAFT')).toBe('Draft');
    expect(component.getStatusLabel('REJECTED')).toBe('Rejected');
    expect(component.getStatusLabel('STALE')).toBe('Stale');
    expect(component.getStatusLabel('READY_FOR_AI')).toBe('Ready for AI');
    expect(component.getStatusLabel('MISSING')).toBe('Ready for AI');
  });
});
