import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EducationDetailsPanelComponent } from './education-details-panel.component';
import { EducationEntry } from '../../models';

describe('EducationDetailsPanelComponent', () => {
  let component: EducationDetailsPanelComponent;
  let fixture: ComponentFixture<EducationDetailsPanelComponent>;

  const mockEducation: EducationEntry[] = [
    {
      id: 'edu-1',
      qualification: 'BACHELORS',
      courseName: 'B.Tech Computer Science',
      boardOrUniversity: 'Delhi Technical University',
      passingYear: 2020,
      percentageOrCgpa: '8.8',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EducationDetailsPanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(EducationDetailsPanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('education', mockEducation);
    fixture.detectChanges();
  });

  it('should render education list and handle modal opens', () => {
    expect(component).toBeTruthy();
    expect(component.education().length).toBe(1);

    component.openAddModal();
    expect(component.isModalOpen()).toBe(true);
    expect(component.isEditing()).toBe(false);

    component.closeModal();
    expect(component.isModalOpen()).toBe(false);
  });
});
