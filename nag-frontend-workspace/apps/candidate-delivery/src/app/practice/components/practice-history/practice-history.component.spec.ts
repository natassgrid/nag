import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PracticeHistoryComponent } from './practice-history.component';
import { PracticeService } from '../../services/practice.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('PracticeHistoryComponent', () => {
  let component: PracticeHistoryComponent;
  let fixture: ComponentFixture<PracticeHistoryComponent>;
  let practiceServiceMock: any;

  const mockHistoryData = {
    content: [
      {
        sessionId: '01a0fd3e-ae81-7c07-a1a1-1164d114f02a',
        practiceSetId: '01a0fd1d-ed38-74fd-a812-e7160d029654',
        practiceSetName: 'Gate CS Algorithms Mock',
        submittedAt: '2026-10-02T12:00:00Z',
        obtainedMarks: 55,
        totalMarks: 60,
        accuracyPercent: 90.0,
        totalQuestions: 30,
        correctCount: 27,
        incorrectCount: 3,
      },
      {
        sessionId: '01a0fd3e-ae81-7c07-a1a1-1164d114f01b',
        practiceSetId: '01a0fd1d-ed38-74fd-a812-e7160d029654',
        practiceSetName: 'Gate CS Algorithms Mock',
        submittedAt: '2026-10-02T10:00:00Z',
        obtainedMarks: 40,
        totalMarks: 60,
        accuracyPercent: 70.0,
        totalQuestions: 30,
        correctCount: 21,
        incorrectCount: 9,
      },
    ],
    totalElements: 2,
    totalPages: 1,
    number: 0,
    size: 10,
  };

  beforeEach(async () => {
    practiceServiceMock = {
      getHistory: jest.fn().mockReturnValue(of(mockHistoryData)),
    };

    await TestBed.configureTestingModule({
      imports: [PracticeHistoryComponent, NoopAnimationsModule],
      providers: [
        { provide: PracticeService, useValue: practiceServiceMock },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PracticeHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load multi-attempt history records with aggregated KPIs', () => {
    expect(component).toBeTruthy();
    expect(practiceServiceMock.getHistory).toHaveBeenCalledWith(0, 10);
    expect(component.history().length).toBe(2);
    expect(component.totalAttempts()).toBe(2);
    expect(component.avgAccuracy()).toBe(80.0); // (90 + 70) / 2 = 80
    expect(component.highestScore()).toBe(55);
    expect(component.totalSolved()).toBe(60); // 30 + 30
  });

  it('should filter history by search query', () => {
    component.searchQuery.set('Algorithms');
    expect(component.filteredHistory().length).toBe(2);

    component.searchQuery.set('NonExistent');
    expect(component.filteredHistory().length).toBe(0);

    component.clearSearch();
    expect(component.searchQuery()).toBe('');
    expect(component.filteredHistory().length).toBe(2);
  });

  it('should handle pagination changes', () => {
    component.onPageChange({ pageIndex: 1, pageSize: 10, length: 2 });
    expect(practiceServiceMock.getHistory).toHaveBeenCalledWith(1, 10);
  });
});
