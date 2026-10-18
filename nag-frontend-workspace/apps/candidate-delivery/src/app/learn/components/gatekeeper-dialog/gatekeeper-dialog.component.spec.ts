import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { GatekeeperDialogComponent } from './gatekeeper-dialog.component';

describe('GatekeeperDialogComponent', () => {
  let component: GatekeeperDialogComponent;
  let fixture: ComponentFixture<GatekeeperDialogComponent>;
  let mockDialogRef: jest.Mocked<Partial<MatDialogRef<GatekeeperDialogComponent>>>;
  let mockRouter: jest.Mocked<Partial<Router>>;

  beforeEach(async () => {
    mockDialogRef = {
      close: jest.fn(),
    };
    mockRouter = {
      navigate: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [GatekeeperDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: mockDialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            statusResponse: {
              gatekeeperPassed: false,
              active: true,
              hasGaps: true,
              conceptualGaps: [{ subtopicName: 'Moment of Inertia' }],
              applicationGaps: [],
            },
          },
        },
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GatekeeperDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should list conceptual gaps', () => {
    expect(component.conceptualGaps.length).toBe(1);
    expect(component.conceptualGaps[0].subtopicName).toBe('Moment of Inertia');
  });

  it('should navigate to roadmap on onGoToRoadmap', () => {
    component.onGoToRoadmap();
    expect(mockDialogRef.close).toHaveBeenCalledWith('GO_TO_ROADMAP');
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/learn/roadmap']);
  });
});
