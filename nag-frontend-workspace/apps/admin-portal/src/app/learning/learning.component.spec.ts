import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { LearningComponent } from './learning.component';
import { PracticeSetService } from './services';
import { PracticeSet } from './models';
import { PracticeSetFormModalComponent, PracticeSetDeleteDialogComponent } from './components';

describe('LearningComponent', () => {
  let component: LearningComponent;
  let fixture: ComponentFixture<LearningComponent>;
  let practiceSetServiceMock: any;
  let dialogMock: any;

  const mockSets: PracticeSet[] = [
    {
      id: 'set-1',
      name: 'Physics Practice 1',
      description: 'Mock test for kinematics',
      source: 'MANUAL',
      durationMinutes: 45,
      subjectSlug: 'physics',
      published: true,
      totalQuestions: 20,
      createdBy: 'user-1',
      createdAt: '2026-10-01T10:00:00Z',
    },
    {
      id: 'set-2',
      name: 'Mathematics Paper Practice',
      description: 'Calculus mock',
      source: 'EXAM_CLONE',
      durationMinutes: 60,
      subjectSlug: 'maths',
      published: false,
      totalQuestions: 30,
      createdBy: 'user-1',
      createdAt: '2026-10-02T10:00:00Z',
    },
  ];

  beforeEach(async () => {
    practiceSetServiceMock = {
      getAll: jest.fn().mockReturnValue(of(mockSets)),
      publish: jest.fn().mockReturnValue(of(undefined)),
      unpublish: jest.fn().mockReturnValue(of(undefined)),
    };

    dialogMock = {
      open: jest.fn().mockReturnValue({
        afterClosed: jest.fn().mockReturnValue(of(true)),
      }),
    };

    await TestBed.configureTestingModule({
      imports: [LearningComponent, NoopAnimationsModule],
      providers: [
        { provide: PracticeSetService, useValue: practiceSetServiceMock },
        { provide: MatDialog, useValue: dialogMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LearningComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load practice sets on init', () => {
    expect(component).toBeTruthy();
    expect(practiceSetServiceMock.getAll).toHaveBeenCalled();
    expect(component.practiceSets().length).toBe(2);
    expect(component.totalSets()).toBe(2);
    expect(component.publishedCount()).toBe(1);
    expect(component.totalQuestionsCount()).toBe(50);
  });

  it('should filter practice sets by search query', () => {
    component.searchQuery.set('kinematics');
    expect(component.filteredSets().length).toBe(1);
    expect(component.filteredSets()[0].name).toBe('Physics Practice 1');

    component.searchQuery.set('maths');
    expect(component.filteredSets().length).toBe(1);
    expect(component.filteredSets()[0].name).toBe('Mathematics Paper Practice');

    component.searchQuery.set('nonexistent');
    expect(component.filteredSets().length).toBe(0);
  });

  it('should filter practice sets by published status', () => {
    component.statusFilter.set('PUBLISHED');
    expect(component.filteredSets().length).toBe(1);
    expect(component.filteredSets()[0].published).toBe(true);

    component.statusFilter.set('DRAFT');
    expect(component.filteredSets().length).toBe(1);
    expect(component.filteredSets()[0].published).toBe(false);

    component.statusFilter.set('ALL');
    expect(component.filteredSets().length).toBe(2);
  });

  it('should open create practice set modal and reload on save', () => {
    component.openCreateModal();
    expect(dialogMock.open).toHaveBeenCalledWith(PracticeSetFormModalComponent, {
      width: '768px',
      maxWidth: '95vw',
      panelClass: 'custom-dialog-container',
      data: { set: null, mode: 'create' },
    });
    expect(practiceSetServiceMock.getAll).toHaveBeenCalledTimes(2);
  });

  it('should open edit practice set modal', () => {
    component.openEditModal(mockSets[0]);
    expect(dialogMock.open).toHaveBeenCalledWith(PracticeSetFormModalComponent, {
      width: '768px',
      maxWidth: '95vw',
      panelClass: 'custom-dialog-container',
      data: { set: mockSets[0], mode: 'edit' },
    });
  });

  it('should open curate practice set modal', () => {
    component.openCurateModal(mockSets[1]);
    expect(dialogMock.open).toHaveBeenCalledWith(PracticeSetFormModalComponent, {
      width: '768px',
      maxWidth: '95vw',
      panelClass: 'custom-dialog-container',
      data: { set: mockSets[1], mode: 'edit' },
    });
  });

  it('should open delete dialog and reload on confirmation', () => {
    component.openDeleteDialog(mockSets[0]);
    expect(dialogMock.open).toHaveBeenCalledWith(PracticeSetDeleteDialogComponent, {
      width: '440px',
      maxWidth: '95vw',
      panelClass: 'custom-dialog-container',
      data: { set: mockSets[0] },
    });
    expect(practiceSetServiceMock.getAll).toHaveBeenCalledTimes(2);
  });

  it('should toggle publish status: unpublish if already published', () => {
    component.togglePublish(mockSets[0]);
    expect(practiceSetServiceMock.unpublish).toHaveBeenCalledWith('set-1');
  });

  it('should toggle publish status: publish if currently draft', () => {
    component.togglePublish(mockSets[1]);
    expect(practiceSetServiceMock.publish).toHaveBeenCalledWith('set-2');
  });
});
