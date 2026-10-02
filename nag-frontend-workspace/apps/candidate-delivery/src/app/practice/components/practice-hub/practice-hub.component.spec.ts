import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PracticeHubComponent } from './practice-hub.component';
import { PracticeService } from '../../services/practice.service';
import { PracticeSet } from '../../models';

describe('PracticeHubComponent', () => {
  let component: PracticeHubComponent;
  let fixture: ComponentFixture<PracticeHubComponent>;
  let practiceServiceMock: any;
  let dialogMock: any;

  const mockSets: PracticeSet[] = [
    {
      id: 'set-1',
      name: 'Mathematics Mock Practice',
      description: 'Test your algebra and calculus skills',
      source: 'EXAM_CLONE',
      durationMinutes: 45,
      subjectSlug: 'math',
      published: true,
      totalQuestions: 25,
      createdBy: 'admin-1',
      createdAt: '2026-01-01',
    },
    {
      id: 'set-2',
      name: 'Physics Practice',
      description: 'Mechanics and Waves',
      source: 'MANUAL',
      durationMinutes: 30,
      subjectSlug: 'physics',
      published: true,
      totalQuestions: 15,
      createdBy: 'admin-1',
      createdAt: '2026-01-01',
    },
  ];

  beforeEach(async () => {
    practiceServiceMock = {
      getSets: jest.fn().mockReturnValue(of(mockSets)),
    };
    dialogMock = {
      open: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [PracticeHubComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: PracticeService, useValue: practiceServiceMock },
        { provide: MatDialog, useValue: dialogMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PracticeHubComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load and render practice sets on init', () => {
    expect(component).toBeTruthy();
    expect(practiceServiceMock.getSets).toHaveBeenCalled();
    expect(component.practiceSets().length).toBe(2);
    expect(component.isLoading()).toBe(false);
  });

  it('should handle error when loading practice sets fails', () => {
    practiceServiceMock.getSets.mockReturnValue(throwError(() => new Error('Network error')));
    component.ngOnInit();
    expect(component.error()).toBe('Failed to load practice sets.');
    expect(component.isLoading()).toBe(false);
  });

  it('should open launch dialog when practice set is clicked', () => {
    component.openLaunch(mockSets[0]);
    expect(dialogMock.open).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        data: { set: mockSets[0] },
      })
    );
  });
});
