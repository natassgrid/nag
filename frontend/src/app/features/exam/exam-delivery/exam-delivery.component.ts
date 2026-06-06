import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatRadioModule } from '@angular/material/radio';
import { Subject, takeUntil, interval } from 'rxjs';
import { ExamService, ExamSession, Question, ResponseSave } from '../services/exam.service';
import { OfflineBufferService } from '../services/offline-buffer.service';
import { QuestionPaletteComponent } from '../question-palette/question-palette.component';

@Component({
  selector: 'app-exam-delivery',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatRadioModule,
    QuestionPaletteComponent
  ],
  templateUrl: './exam-delivery.component.html',
  styles: [`
    .exam-delivery { display: flex; flex-direction: column; height: 100vh; }
    .exam-header {
      display: flex; align-items: center; gap: var(--spacing-md);
      padding: var(--spacing-md); background: var(--color-surface);
      border-bottom: 1px solid var(--color-border);
    }
    .exam-name { font-size: 1.1rem; margin: 0; flex: 1; }
    .timer-bar { display: flex; align-items: center; gap: var(--spacing-sm); min-width: 200px; }
    .timer-bar.warning .timer-display { color: var(--color-warning); font-weight: bold; }
    .timer-bar.critical .timer-display { color: var(--color-error); font-weight: bold; animation: pulse 1s infinite; }
    .timer-display { font-family: monospace; font-size: 1.2rem; }
    .question-area { flex: 1; padding: var(--spacing-lg); overflow-y: auto; }
    .question-meta { margin-bottom: var(--spacing-md); display: flex; gap: var(--spacing-md); align-items: center; }
    .question-number { font-weight: 500; font-size: 1.1rem; }
    .question-marks { color: var(--color-text-secondary); }
    .negative-marks { color: var(--color-error); font-size: 0.9rem; }
    .question-content { margin-bottom: var(--spacing-lg); line-height: 1.8; font-size: 1rem; }
    .options-area { display: flex; flex-direction: column; gap: var(--spacing-md); }
    .option-item { padding: var(--spacing-sm) var(--spacing-md); border: 1px solid var(--color-border); border-radius: 4px; }
    .option-item.selected { border-color: var(--color-primary); background: rgba(21, 101, 192, 0.05); }
    .option-label { font-weight: 500; margin-right: var(--spacing-sm); }
    .text-input-area { max-width: 600px; }
    .full-width { width: 100%; }
    .exam-nav {
      display: flex; align-items: center; gap: var(--spacing-md);
      padding: var(--spacing-md); border-top: 1px solid var(--color-border);
      background: var(--color-surface); flex-wrap: wrap;
    }
    .nav-buttons { margin-left: auto; display: flex; gap: var(--spacing-sm); }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
  `]
})
export class ExamDeliveryComponent implements OnInit, OnDestroy {
  session: ExamSession | null = null;
  currentQuestion: Question | null = null;
  selectedOptionIds: string[] = [];
  enteredValue = '';
  remainingSeconds = 0;
  timerProgress = 100;
  totalDurationSeconds = 0;
  isFullscreen = false;
  answeredQuestions = new Set<number>();
  flaggedQuestions = new Set<number>();
  renderedContent = '';

  private destroy$ = new Subject<void>();
  private questionStartTime = Date.now();

  constructor(
    private examService: ExamService,
    private offlineBuffer: OfflineBufferService
  ) {}

  ngOnInit(): void {
    this.offlineBuffer.init();
    // In a real scenario, shiftId would come from route params or session
    this.startExam('00000000-0000-0000-0000-000000000001');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private startExam(shiftId: string): void {
    this.examService.startSession(shiftId).subscribe({
      next: (session) => {
        this.session = session;
        this.totalDurationSeconds = session.durationMinutes * 60;
        this.remainingSeconds = this.totalDurationSeconds;
        this.startTimer();
        this.loadQuestion(1);
        this.enterFullscreen();
      },
      error: () => {
        // Handle session start failure
      }
    });
  }

  private startTimer(): void {
    interval(1000).pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.remainingSeconds--;
      this.timerProgress = (this.remainingSeconds / this.totalDurationSeconds) * 100;

      if (this.remainingSeconds <= 0) {
        this.autoSubmit();
      }
    });
  }

  private loadQuestion(sequenceNumber: number): void {
    if (!this.session) return;

    this.examService.getQuestion(this.session.sessionId, sequenceNumber).subscribe({
      next: (question) => {
        this.currentQuestion = question;
        this.selectedOptionIds = [];
        this.enteredValue = '';
        this.questionStartTime = Date.now();
        this.renderContent(question);
      },
      error: () => {
        // Serve from offline buffer if network fails
      }
    });
  }

  private renderContent(question: Question): void {
    // For LaTeX/MathML, KaTeX would render here. For now, pass HTML through.
    this.renderedContent = question.content;
  }

  selectOption(optionId: string): void {
    this.selectedOptionIds = [optionId];
  }

  toggleOption(optionId: string): void {
    const idx = this.selectedOptionIds.indexOf(optionId);
    if (idx >= 0) {
      this.selectedOptionIds.splice(idx, 1);
    } else {
      this.selectedOptionIds.push(optionId);
    }
  }

  onAnswerChange(): void {
    // Auto-save triggers on change
  }

  clearResponse(): void {
    this.selectedOptionIds = [];
    this.enteredValue = '';
  }

  markForReview(): void {
    if (this.currentQuestion) {
      const seq = this.currentQuestion.sequenceNumber;
      if (this.flaggedQuestions.has(seq)) {
        this.flaggedQuestions.delete(seq);
      } else {
        this.flaggedQuestions.add(seq);
      }
    }
  }

  saveAndNext(): void {
    this.saveCurrentResponse('USER');
    if (this.currentQuestion && this.session) {
      const next = this.currentQuestion.sequenceNumber + 1;
      if (next <= this.session.totalQuestions) {
        this.loadQuestion(next);
      }
    }
  }

  previousQuestion(): void {
    this.saveCurrentResponse('NAVIGATION');
    if (this.currentQuestion && this.currentQuestion.sequenceNumber > 1) {
      this.loadQuestion(this.currentQuestion.sequenceNumber - 1);
    }
  }

  navigateToQuestion(sequenceNumber: number): void {
    this.saveCurrentResponse('NAVIGATION');
    this.loadQuestion(sequenceNumber);
  }

  toggleReviewFlag(sequenceNumber: number): void {
    if (this.flaggedQuestions.has(sequenceNumber)) {
      this.flaggedQuestions.delete(sequenceNumber);
    } else {
      this.flaggedQuestions.add(sequenceNumber);
    }
  }

  private saveCurrentResponse(source: 'USER' | 'AUTO_SAVE' | 'NAVIGATION'): void {
    if (!this.currentQuestion || !this.session) return;

    const timeSpent = Date.now() - this.questionStartTime;
    const response: ResponseSave = {
      questionId: this.currentQuestion.questionId,
      selectedOptionIds: this.selectedOptionIds.length > 0 ? this.selectedOptionIds : undefined,
      enteredValue: this.enteredValue || undefined,
      cumulativeTimeSpentMs: timeSpent,
      saveSource: source
    };

    if (this.selectedOptionIds.length > 0 || this.enteredValue) {
      this.answeredQuestions.add(this.currentQuestion.sequenceNumber);
    }

    // Try online save, fallback to IndexedDB
    this.examService.saveResponse(this.session.sessionId, response).subscribe({
      error: () => {
        this.offlineBuffer.bufferResponse(this.session!.sessionId, response);
      }
    });
  }

  submitExam(): void {
    if (!this.session) return;
    this.examService.submitExam(this.session.sessionId).subscribe({
      next: () => {
        // Navigate to submission confirmation
      }
    });
  }

  private autoSubmit(): void {
    this.saveCurrentResponse('AUTO_SAVE');
    this.submitExam();
  }

  toggleFullscreen(): void {
    if (!document.fullscreenElement) {
      this.enterFullscreen();
    } else {
      document.exitFullscreen();
      this.isFullscreen = false;
    }
  }

  private enterFullscreen(): void {
    document.documentElement.requestFullscreen?.().then(() => {
      this.isFullscreen = true;
    }).catch(() => {});
  }

  formatTime(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    return h > 0
      ? `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
      : `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }
}
