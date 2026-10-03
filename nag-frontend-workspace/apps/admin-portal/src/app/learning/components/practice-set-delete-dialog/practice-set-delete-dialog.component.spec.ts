import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { PracticeSetDeleteDialogComponent } from './practice-set-delete-dialog.component';
import { PracticeSetService } from '../../services';
import { PracticeSet } from '../../models';

describe('PracticeSetDeleteDialogComponent', () => {
  let component: PracticeSetDeleteDialogComponent;
  let fixture: ComponentFixture<PracticeSetDeleteDialogComponent>;
  let practiceSetServiceMock: any;
  let dialogRefMock: any;

  const mockSet: PracticeSet = {
    id: 'set-delete-1',
    name: 'Practice Set To Delete',
    description: 'Description',
    source: 'MANUAL',
    durationMinutes: 30,
    subjectSlug: 'math',
    published: false,
    totalQuestions: 10,
    createdBy: 'user-1',
    createdAt: '2026-10-01T10:00:00Z',
  };

  beforeEach(async () => {
    practiceSetServiceMock = {
      delete: jest.fn().mockReturnValue(of(undefined)),
    };

    dialogRefMock = {
      close: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [PracticeSetDeleteDialogComponent],
      providers: [
        { provide: PracticeSetService, useValue: practiceSetServiceMock },
        { provide: MatDialogRef, useValue: dialogRefMock },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { set: mockSet },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PracticeSetDeleteDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and render confirmation message with set name', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Practice Set To Delete');
  });

  it('should call delete service and close dialog with true on confirmation', () => {
    component.onDelete();
    expect(practiceSetServiceMock.delete).toHaveBeenCalledWith('set-delete-1');
    expect(dialogRefMock.close).toHaveBeenCalledWith(true);
  });

  it('should handle delete error and reset deleting state', () => {
    practiceSetServiceMock.delete.mockReturnValue(throwError(() => new Error('Delete failed')));
    component.onDelete();
    expect(practiceSetServiceMock.delete).toHaveBeenCalledWith('set-delete-1');
    expect(component.isDeleting()).toBe(false);
    expect(dialogRefMock.close).not.toHaveBeenCalled();
  });

  it('should close dialog with false on cancel', () => {
    component.onCancel();
    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });
});
