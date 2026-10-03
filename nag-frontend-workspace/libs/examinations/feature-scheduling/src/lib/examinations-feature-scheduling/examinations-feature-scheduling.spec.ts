import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { ExaminationsFeatureScheduling } from './examinations-feature-scheduling';
import {
  ExaminationService,
  SchedulingService,
  CentreManagementService,
} from '@nag-frontend-workspace/examinations-data-access';

describe('ExaminationsFeatureScheduling', () => {
  let component: ExaminationsFeatureScheduling;
  let fixture: ComponentFixture<ExaminationsFeatureScheduling>;

  let examServiceMock: {
    getExams: jest.Mock;
    exams: any;
    loading: any;
  };
  let scheduleServiceMock: {
    listSchedules: jest.Mock;
    listShifts: jest.Mock;
    schedules: any;
    shifts: any;
    loading: any;
  };
  let centreServiceMock: {
    listCentres: jest.Mock;
    centres: any;
  };

  beforeEach(async () => {
    examServiceMock = {
      getExams: jest.fn().mockReturnValue(of([{ id: 'exam-1', title: 'UPSC CSE 2026' }])),
      exams: signal([{ id: 'exam-1', title: 'UPSC CSE 2026' }]),
      loading: signal(false),
    };

    scheduleServiceMock = {
      listSchedules: jest.fn().mockReturnValue(of([{ id: 'sched-1', title: 'Phase 1' }])),
      listShifts: jest.fn().mockReturnValue(of([{ id: 'shift-1', name: 'Morning Shift' }])),
      schedules: signal([]),
      shifts: signal([]),
      loading: signal(false),
    };

    centreServiceMock = {
      listCentres: jest.fn().mockReturnValue(of([])),
      centres: signal([]),
    };

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule,
        ExaminationsFeatureScheduling,
      ],
      providers: [
        { provide: ExaminationService, useValue: examServiceMock },
        { provide: SchedulingService, useValue: scheduleServiceMock },
        { provide: CentreManagementService, useValue: centreServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExaminationsFeatureScheduling);
    component = fixture.componentInstance;
  });

  it('should create and load initial exams and schedules', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(examServiceMock.getExams).toHaveBeenCalled();
    expect(component.selectedExamId()).toBe('exam-1');
  });

  it('should change tab correctly', () => {
    expect(component.currentTab()).toBe('SCHEDULES');
    component.currentTab.set('SHIFTS');
    expect(component.currentTab()).toBe('SHIFTS');
    component.currentTab.set('ALLOCATIONS');
    expect(component.currentTab()).toBe('ALLOCATIONS');
  });
});
