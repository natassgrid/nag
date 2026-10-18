import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { MicroLessonTranslationPanelComponent } from './micro-lesson-translation-panel.component';
import { MicroLessonAdminService } from '../../../../services/micro-lesson-admin.service';
import { MicroLessonReviewItem, MicroLessonTranslation } from '../../../../models/micro-lesson.model';

describe('MicroLessonTranslationPanelComponent', () => {
  let component: MicroLessonTranslationPanelComponent;
  let fixture: ComponentFixture<MicroLessonTranslationPanelComponent>;
  let mockService: {
    getTranslations: jest.Mock;
    autoTranslateMicroLesson: jest.Mock;
    approveTranslation: jest.Mock;
    rejectTranslation: jest.Mock;
  };

  const sampleLesson: MicroLessonReviewItem = {
    id: 'lesson-1',
    subtopicName: 'Moment of Inertia',
    topicName: 'Rotational Motion',
    subjectName: 'Physics',
    targetExam: 'JEE_MAIN',
    status: 'APPROVED',
    contentVersion: 1,
    content: {
      coreConcept: 'Resistance to angular acceleration: $$I = \\sum m_i r_i^2$$',
      trapAlert: 'Axis matters!',
      formulaCheatSheet: '$$I = m r^2$$',
    },
  };

  const sampleTranslation: MicroLessonTranslation = {
    id: 'trans-hi-1',
    microlessonId: 'lesson-1',
    languageCode: 'hi',
    coreConcept: 'कोणीय त्वरण का विरोध',
    trap: 'अक्ष महत्वपूर्ण है',
    cheatSheet: ['$$I = m r^2$$'],
    stepByStepBreakdown: {},
    sourceVersion: 1,
    status: 'IN_REVIEW',
  };

  beforeEach(async () => {
    mockService = {
      getTranslations: jest.fn().mockReturnValue(of([sampleTranslation])),
      autoTranslateMicroLesson: jest.fn().mockReturnValue(of(sampleTranslation)),
      approveTranslation: jest.fn().mockReturnValue(of({ ...sampleTranslation, status: 'APPROVED' })),
      rejectTranslation: jest.fn().mockReturnValue(of({ ...sampleTranslation, status: 'REJECTED' })),
    };

    await TestBed.configureTestingModule({
      imports: [MicroLessonTranslationPanelComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MicroLessonAdminService, useValue: mockService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MicroLessonTranslationPanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('lesson', sampleLesson);
    fixture.detectChanges();
  });

  it('should create and load translations for the lesson', () => {
    expect(component).toBeTruthy();
    expect(mockService.getTranslations).toHaveBeenCalledWith('lesson-1');
    expect(component.translations().length).toBe(1);
    expect(component.activeTranslation()?.languageCode).toBe('hi');
  });

  it('should select language and find translation', () => {
    component.selectLanguage('hi');
    expect(component.selectedLanguage()).toBe('hi');
    expect(component.activeTranslation()).toBeTruthy();
    expect(component.activeTranslation()?.coreConcept).toBe('कोणीय त्वरण का विरोध');
  });

  it('should trigger on-demand IndicTrans2 translation', () => {
    component.selectLanguage('hi');
    component.triggerAutoTranslate();

    expect(mockService.autoTranslateMicroLesson).toHaveBeenCalledWith('lesson-1', 'hi');
    expect(component.statusMessage()).toContain('IndicTrans2');
  });

  it('should approve current translation', () => {
    component.selectLanguage('hi');
    component.reviewComments.set('Looks accurate');
    component.approveCurrentTranslation();

    expect(mockService.approveTranslation).toHaveBeenCalledWith('trans-hi-1', 'Looks accurate');
    expect(component.activeTranslation()?.status).toBe('APPROVED');
  });

  it('should reject current translation', () => {
    component.selectLanguage('hi');
    component.reviewComments.set('Formulas translated incorrectly');
    component.rejectCurrentTranslation();

    expect(mockService.rejectTranslation).toHaveBeenCalledWith('trans-hi-1', 'Formulas translated incorrectly');
    expect(component.activeTranslation()?.status).toBe('REJECTED');
  });

  it('should toggle bilingual comparison view', () => {
    expect(component.showBilingualComparison()).toBe(true);
    component.showBilingualComparison.set(false);
    expect(component.showBilingualComparison()).toBe(false);
  });
});
