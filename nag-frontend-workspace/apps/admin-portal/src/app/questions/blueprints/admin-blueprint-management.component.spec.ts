import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AdminBlueprintManagementComponent } from './admin-blueprint-management.component';
import {
  BlueprintTemplateService,
  SubjectTopicService,
} from '@nag-frontend-workspace/questions-data-access';

describe('AdminBlueprintManagementComponent', () => {
  let component: AdminBlueprintManagementComponent;
  let fixture: ComponentFixture<AdminBlueprintManagementComponent>;
  let mockBlueprintService: {
    listTemplates: jest.Mock;
  };
  let mockSubjectTopicService: {
    getSubjects: jest.Mock;
    getHierarchy: jest.Mock;
  };

  beforeEach(async () => {
    mockBlueprintService = {
      listTemplates: jest.fn().mockReturnValue(of([])),
    };
    mockSubjectTopicService = {
      getSubjects: jest.fn().mockReturnValue(of([])),
      getHierarchy: jest.fn().mockReturnValue(of([])),
    };

    await TestBed.configureTestingModule({
      imports: [AdminBlueprintManagementComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: BlueprintTemplateService, useValue: mockBlueprintService },
        { provide: SubjectTopicService, useValue: mockSubjectTopicService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminBlueprintManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render blueprint management and fetch templates', () => {
    expect(component).toBeTruthy();
    expect(mockBlueprintService.listTemplates).toHaveBeenCalled();
  });
});
