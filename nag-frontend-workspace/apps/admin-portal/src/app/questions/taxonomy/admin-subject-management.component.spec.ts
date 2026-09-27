import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AdminSubjectManagementComponent } from './admin-subject-management.component';
import { SubjectTopicService } from '@nag-frontend-workspace/questions-data-access';

describe('AdminSubjectManagementComponent', () => {
  let component: AdminSubjectManagementComponent;
  let fixture: ComponentFixture<AdminSubjectManagementComponent>;
  let mockSubjectTopicService: {
    getHierarchy: jest.Mock;
    createSubject: jest.Mock;
    createTopic: jest.Mock;
    createSubtopic: jest.Mock;
  };

  beforeEach(async () => {
    mockSubjectTopicService = {
      getHierarchy: jest.fn().mockReturnValue(of([])),
      createSubject: jest.fn().mockReturnValue(of({})),
      createTopic: jest.fn().mockReturnValue(of({})),
      createSubtopic: jest.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [AdminSubjectManagementComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: SubjectTopicService, useValue: mockSubjectTopicService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminSubjectManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render subject taxonomy management and fetch hierarchy', () => {
    expect(component).toBeTruthy();
    expect(mockSubjectTopicService.getHierarchy).toHaveBeenCalled();
  });
});
