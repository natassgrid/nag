/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { QuestionTranslationListComponent } from './question-translation-list.component';
import { QuestionService, QuestionResponse } from '../question.service';
import { TranslationService } from './translation.service';
import { SubjectTopicService } from '../subject-topic.service';

describe('QuestionTranslationListComponent', () => {
  let component: QuestionTranslationListComponent;
  let fixture: ComponentFixture<QuestionTranslationListComponent>;
  let questionServiceSpy: jasmine.SpyObj<QuestionService>;
  let translationServiceSpy: jasmine.SpyObj<TranslationService>;
  let subjectTopicServiceSpy: jasmine.SpyObj<SubjectTopicService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    questionServiceSpy = jasmine.createSpyObj('QuestionService', ['getQuestions']);
    translationServiceSpy = jasmine.createSpyObj('TranslationService', ['listBatchJobs', 'startBatchTranslation', 'getBatchJobStatus', 'cancelBatchJob']);
    subjectTopicServiceSpy = jasmine.createSpyObj('SubjectTopicService', ['getSubjects']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    subjectTopicServiceSpy.getSubjects.and.returnValue(of([
      { id: 1, name: 'Physics', code: 'PHY', tenantId: 'default' },
      { id: 2, name: 'Mathematics', code: 'MATH', tenantId: 'default' }
    ]));
    translationServiceSpy.listBatchJobs.and.returnValue(of([]));
    questionServiceSpy.getQuestions.and.returnValue(of({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 20,
      number: 0
    }));

    await TestBed.configureTestingModule({
      imports: [QuestionTranslationListComponent, NoopAnimationsModule],
      providers: [
        { provide: QuestionService, useValue: questionServiceSpy },
        { provide: TranslationService, useValue: translationServiceSpy },
        { provide: SubjectTopicService, useValue: subjectTopicServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(QuestionTranslationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize filter categories with Translation Language and Translation Status', () => {
    const targetLangCategory = component.filterCategories.find(c => c.key === 'targetLang');
    const translationStatusCategory = component.filterCategories.find(c => c.key === 'translationStatus');

    expect(targetLangCategory).toBeDefined();
    expect(targetLangCategory?.label).toBe('Translation Language');
    // All Languages + 22 Scheduled languages = 23 options
    expect(targetLangCategory?.options.length).toBe(23);

    expect(translationStatusCategory).toBeDefined();
    expect(translationStatusCategory?.label).toBe('Translation Status');
    expect(translationStatusCategory?.options.map(o => o.value)).toContain('ALL');
    expect(translationStatusCategory?.options.map(o => o.value)).toContain('MISSING');
    expect(translationStatusCategory?.options.map(o => o.value)).toContain('PENDING_REVIEW');
    expect(translationStatusCategory?.options.map(o => o.value)).toContain('APPROVED_PUBLISHED');
    expect(translationStatusCategory?.options.map(o => o.value)).toContain('REJECTED');
  });

  it('should pass targetLang and translationStatus from filters into questionService.getQuestions', () => {
    component.onFilterChange({
      targetLang: ['hi'],
      translationStatus: ['APPROVED_PUBLISHED'],
      subject: ['Physics']
    });

    component.fetcher({ page: 0, size: 20, search: 'momentum' });

    expect(questionServiceSpy.getQuestions).toHaveBeenCalledWith({
      subject: 'Physics',
      subjectId: undefined,
      difficulty: undefined,
      targetLang: 'hi',
      translationStatus: 'APPROVED_PUBLISHED',
      search: 'momentum',
      page: 0,
      size: 20
    });
  });

  it('should display correct translation status label and chip class when targetLang is selected', () => {
    component.filters = { targetLang: 'hi' };

    const qApproved: QuestionResponse = {
      id: 'q1',
      subjectId: 1,
      topicId: 1,
      subject: 'Physics',
      topic: 'Kinematics',
      subtopic: 'Velocity',
      chapter: '1',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'APPLY',
      questionType: 'SINGLE_MCQ',
      content: 'Sample content',
      answerKey: 'A',
      state: 'APPROVED',
      authorId: 'a1',
      createdAt: '2025-01-01T00:00:00Z',
      translationStatus: 'APPROVED',
      translationStatusMap: { hi: 'APPROVED' }
    };

    expect(component.getTranslationStatusLabel(qApproved)).toBe('Hindi: Approved');
    expect(component.getTranslationStatusClass(qApproved)).toBe('chip-trans-approved');

    const qMissing: QuestionResponse = {
      ...qApproved,
      id: 'q2',
      translationStatus: 'MISSING',
      translationStatusMap: {}
    };

    expect(component.getTranslationStatusLabel(qMissing)).toBe('Hindi: Untranslated');
    expect(component.getTranslationStatusClass(qMissing)).toBe('chip-trans-missing');

    const qReview: QuestionResponse = {
      ...qApproved,
      id: 'q3',
      translationStatus: 'DRAFT',
      translationStatusMap: { hi: 'DRAFT' }
    };

    expect(component.getTranslationStatusLabel(qReview)).toBe('Hindi: In Review');
    expect(component.getTranslationStatusClass(qReview)).toBe('chip-trans-review');

    const qRejected: QuestionResponse = {
      ...qApproved,
      id: 'q4',
      translationStatus: 'REJECTED',
      translationStatusMap: { hi: 'REJECTED' }
    };

    expect(component.getTranslationStatusLabel(qRejected)).toBe('Hindi: Needs Rework');
    expect(component.getTranslationStatusClass(qRejected)).toBe('chip-trans-rejected');
  });

  it('should display summary of translated languages when no targetLang is selected', () => {
    component.filters = {};

    const qWithLangs: QuestionResponse = {
      id: 'q1',
      subjectId: 1,
      topicId: 1,
      subject: 'Physics',
      topic: 'Kinematics',
      subtopic: 'Velocity',
      chapter: '1',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'APPLY',
      questionType: 'SINGLE_MCQ',
      content: 'Sample content',
      answerKey: 'A',
      state: 'APPROVED',
      authorId: 'a1',
      createdAt: '2025-01-01T00:00:00Z',
      translatedLanguages: ['hi', 'ta', 'te']
    };

    expect(component.getTranslationStatusLabel(qWithLangs)).toBe('3 / 22 Translated');
    expect(component.getTranslationStatusClass(qWithLangs)).toBe('chip-trans-review');

    const qEmpty: QuestionResponse = {
      ...qWithLangs,
      id: 'q2',
      translatedLanguages: []
    };

    expect(component.getTranslationStatusLabel(qEmpty)).toBe('Untranslated');
    expect(component.getTranslationStatusClass(qEmpty)).toBe('chip-trans-missing');
  });

  it('should open translation drawer with filtered language if set', () => {
    component.filters = { targetLang: 'ta' };
    const q: QuestionResponse = {
      id: 'q1',
      subjectId: 1,
      topicId: 1,
      subject: 'Physics',
      topic: 'Kinematics',
      subtopic: 'Velocity',
      chapter: '1',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'APPLY',
      questionType: 'SINGLE_MCQ',
      content: 'Sample content',
      answerKey: 'A',
      state: 'APPROVED',
      authorId: 'a1',
      createdAt: '2025-01-01T00:00:00Z'
    };

    component.openTranslationDrawer(q);
    expect(component.drawerOpen).toBeTrue();
    expect(component.selectedQuestion).toBe(q);
    expect(component.selectedLanguageForDrawer).toBe('ta');
  });
});
