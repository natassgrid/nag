import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { ExaminationsFeaturePaperGen } from './examinations-feature-paper-gen';
import {
  PaperService,
  ExaminationService,
  SchedulingService,
} from '@nag-frontend-workspace/examinations-data-access';
import {
  SubjectTopicService,
  BlueprintTemplateService,
} from '@nag-frontend-workspace/questions-data-access';

describe('ExaminationsFeaturePaperGen', () => {
  let component: ExaminationsFeaturePaperGen;
  let fixture: ComponentFixture<ExaminationsFeaturePaperGen>;

  let paperServiceMock: {
    getPapers: jest.Mock;
    getPaper: jest.Mock;
    approvePaper: jest.Mock;
    publishPaper: jest.Mock;
    papers: any;
    loading: any;
  };
  let examServiceMock: {
    getExams: jest.Mock;
  };
  let schedulingServiceMock: {
    listSchedules: jest.Mock;
    listShifts: jest.Mock;
  };
  let subjectTopicServiceMock: {
    getHierarchy: jest.Mock;
    getSubjects: jest.Mock;
  };
  let blueprintTemplateServiceMock: {
    listTemplates: jest.Mock;
  };

  beforeEach(async () => {
    paperServiceMock = {
      getPapers: jest.fn().mockReturnValue(of({ content: [], totalElements: 0, totalPages: 1 })),
      getPaper: jest.fn().mockReturnValue(of({ id: 'p-1', name: 'Paper 1' })),
      approvePaper: jest.fn().mockReturnValue(of({ message: 'Approved' })),
      publishPaper: jest.fn().mockReturnValue(of({ message: 'Published' })),
      papers: signal([]),
      loading: signal(false),
    };

    examServiceMock = {
      getExams: jest.fn().mockReturnValue(of([{ id: 'ex-1', title: 'GATE 2026' }])),
    };

    schedulingServiceMock = {
      listSchedules: jest.fn().mockReturnValue(of([])),
      listShifts: jest.fn().mockReturnValue(of([])),
    };

    subjectTopicServiceMock = {
      getHierarchy: jest.fn().mockReturnValue(of([])),
      getSubjects: jest.fn().mockReturnValue(of([])),
    };

    blueprintTemplateServiceMock = {
      listTemplates: jest.fn().mockReturnValue(of([])),
    };

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule,
        ExaminationsFeaturePaperGen,
      ],
      providers: [
        { provide: PaperService, useValue: paperServiceMock },
        { provide: ExaminationService, useValue: examServiceMock },
        { provide: SchedulingService, useValue: schedulingServiceMock },
        { provide: SubjectTopicService, useValue: subjectTopicServiceMock },
        { provide: BlueprintTemplateService, useValue: blueprintTemplateServiceMock },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({ examId: 'e1000000-0000-0000-0000-000000000001' }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExaminationsFeaturePaperGen);
    component = fixture.componentInstance;
  });

  it('should create, initialize query params filter and load initial data on init', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(component.selectedExamFilter()).toBe('e1000000-0000-0000-0000-000000000001');
    expect(paperServiceMock.getPapers).toHaveBeenCalled();
    expect(examServiceMock.getExams).toHaveBeenCalled();
  });

  it('should switch paper tabs', () => {
    expect(component.currentTab()).toBe('PAPERS');
    component.currentTab.set('GENERATOR');
    expect(component.currentTab()).toBe('GENERATOR');
    component.currentTab.set('TEMPLATES');
    expect(component.currentTab()).toBe('TEMPLATES');
  });

  it('should open summary drawer and fetch paper details for valid paperId', () => {
    component.openSummaryDrawer('p-valid-1');
    expect(component.drawerOpen()).toBe(true);
    expect(component.selectedPaperId()).toBe('p-valid-1');
    expect(paperServiceMock.getPaper).toHaveBeenCalledWith('p-valid-1');
  });

  it('should not invoke getPaper when openSummaryDrawer is called with invalid or undefined paperId', () => {
    paperServiceMock.getPaper.mockClear();
    component.openSummaryDrawer('undefined');
    expect(paperServiceMock.getPaper).not.toHaveBeenCalled();

    component.openSummaryDrawer('');
    expect(paperServiceMock.getPaper).not.toHaveBeenCalled();
  });
});
