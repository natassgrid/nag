import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { AdminExamManagementComponent } from './admin-exam-management.component';
import { ExaminationService, ExaminationResponse } from '@nag-frontend-workspace/examinations-data-access';

describe('AdminExamManagementComponent', () => {
  let component: AdminExamManagementComponent;
  let fixture: ComponentFixture<AdminExamManagementComponent>;
  let mockExamService: {
    exams: any;
    loading: any;
    getExams: jest.Mock;
    createExam: jest.Mock;
    updateExam: jest.Mock;
  };

  beforeEach(async () => {
    mockExamService = {
      exams: signal([]),
      loading: signal(false),
      getExams: jest.fn().mockReturnValue(of({ content: [] })),
      createExam: jest.fn().mockReturnValue(of({})),
      updateExam: jest.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [AdminExamManagementComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ExaminationService, useValue: mockExamService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminExamManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render exam management and load exams on init', () => {
    expect(component).toBeTruthy();
    expect(mockExamService.getExams).toHaveBeenCalledWith(0, 50);
  });

  it('should open and close edit exam drawer', () => {
    const mockExam = { id: 'exam-1', code: 'NES-01', title: 'National Exam' } as ExaminationResponse;
    component.openEdit(mockExam);
    expect(component.drawerOpen()).toBe(true);
    expect(component.editingExam()?.id).toBe('exam-1');

    component.closeDrawer();
    expect(component.drawerOpen()).toBe(false);
    expect(component.editingExam()).toBeNull();
  });
});
