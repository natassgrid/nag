import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { PracticeLaunchDialogComponent } from './practice-launch-dialog.component';
import { PracticeService } from '../../services/practice.service';
import { PracticeSet } from '../../models';

describe('PracticeLaunchDialogComponent', () => {
  let component: PracticeLaunchDialogComponent;
  let fixture: ComponentFixture<PracticeLaunchDialogComponent>;
  let practiceServiceMock: any;
  let dialogRefMock: any;
  let routerMock: any;

  const mockSet: PracticeSet = {
    id: 'set-abc-123',
    name: 'CS Practice Paper',
    description: 'Algorithms and Data Structures',
    source: 'EXAM_CLONE',
    durationMinutes: 60,
    subjectSlug: 'cs',
    published: true,
    totalQuestions: 30,
  };

  beforeEach(async () => {
    practiceServiceMock = {
      startSession: jest.fn().mockReturnValue(
        of({
          id: 'session-xyz-789',
          practiceSetId: 'set-abc-123',
          candidateId: 'cand-1',
          mode: 'TIMED',
          status: 'IN_PROGRESS',
        })
      ),
    };
    dialogRefMock = {
      close: jest.fn(),
    };
    routerMock = {
      navigate: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [PracticeLaunchDialogComponent],
      providers: [
        { provide: PracticeService, useValue: practiceServiceMock },
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: { set: mockSet } },
        { provide: Router, useValue: routerMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PracticeLaunchDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with given practice set', () => {
    expect(component).toBeTruthy();
    expect(component.set.id).toBe('set-abc-123');
    expect(component.selectedMode()).toBe('TIMED');
  });

  it('should start session and navigate to delivery runner', () => {
    component.startSession();

    expect(practiceServiceMock.startSession).toHaveBeenCalledWith({
      practiceSetId: 'set-abc-123',
      mode: 'TIMED',
    });
    expect(dialogRefMock.close).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/delivery'], {
      queryParams: {
        mode: 'PRACTICE',
        paperId: 'set-abc-123',
        sessionId: 'session-xyz-789',
      },
    });
  });
});
