import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ExamService, ExamSession, Question, ResponsePayload } from '../services/exam.service';
import { ExamTimerComponent } from './exam-timer.component';
import { QuestionDisplayComponent } from './question-display.component';
import { NavigationPaletteComponent } from './navigation-palette.component';
import { ConfirmSubmitDialogComponent } from './confirm-submit-dialog.component';

export type ExamState = 'PRE_EXAM' | 'IN_PROGRESS' | 'SUBMITTED';

@Component({
  selector: 'app-exam-delivery',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatProgressBarModule,
    ExamTimerComponent,
    QuestionDisplayComponent,
    NavigationPaletteComponent
  ],
  templateUrl: './exam-delivery.component.html',
  styles: [`
    .pre-exam-container {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100vh;
      background: #f5f5f5;
    }
    .pre-exam-card {
      max-width: 500px;
      width: 100%;
      padding: 32px;
    }
    .pre-exam-card h2 {
      margin: 0 0 8px 0;
      font-size: 1.4rem;
    }
    .pre-exam-card p {
      color: #555;
      line-height: 1.6;
    }
    .pre-exam-info {
      margin: 16px 0;
      padding: 12px;
      background: #e3f2fd;
      border-radius: 8px;
      font-size: 0.9rem;
    }
    .exam-container {
      display: flex;
      flex-direction: column;
      height: 100vh;
    }
    .exam-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 20px;
      background: white;
      border-bottom: 1px solid #e0e0e0;
      box-shadow: 0 1px 3px rgba(0,0,0,0.08);
    }
    .exam-name {
      font-size: 1.1rem;
      font-weight: 600;
      margin: 0;
      color: #333;
    }
    .exam-body {
      display: flex;
      flex: 1;
      overflow: hidden;
    }
    .question-panel {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
    }
    .palette-panel {
      width: 260px;
      flex-shrink: 0;
    }
    .exam-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 20px;
      background: white;
      border-top: 1px solid #e0e0e0;
    }
    .footer-left {
      display: flex;
      gap: 8px;
    }
    .footer-right {
      display: flex;
      gap: 8px;
    }
    .submitted-container {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100vh;
      background: #f5f5f5;
    }
    .submitted-card {
      max-width: 450px;
      width: 100%;
      padding: 40px;
      text-align: center;
    }
    .submitted-card mat-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: #2e7d32;
      margin-bottom: 16px;
    }
    .submitted-card h2 {
      margin: 0 0 12px 0;
      color: #2e7d32;
    }
    .summary-info {
      margin-top: 16px;
      padding: 12px;
      background: #f5f5f5;
      border-radius: 8px;
      font-size: 0.9rem;
      color: #555;
    }
    .error-msg {
      color: #d32f2f;
      font-size: 0.85rem;
      margin-top: 8px;
    }
  `]
})
export class ExamDeliveryComponent implements OnDestroy {
  // State machine
  state: ExamState = 'PRE_EXAM';

  // Hardcoded IDs for dev testing
  readonly examId = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';
  readonly shiftId = 'b2c3d4e5-f6a7-8901-bcde-f12345678901';
  readonly candidateId = 'c3d4e5f6-a7b8-9012-cdef-123456789012';

  // Session data
  session: ExamSession | null = null;
  currentQuestion: Question | null = null;
  selectedOptionIds: string[] = [];
  enteredValue = '';
  errorMessage = '';
  isLoading = false;

  // Question tracking
  answeredQuestions = new Set<number>();
  markedQuestions = new Set<number>();
  visitedQuestions = new Set<number>();

  // Timing
  private questionStartTime = Date.now();
  private cumulativeTimeMap = new Map<string, number>();
  private revisionMap = new Map<string, number>();

  constructor(
    private examService: ExamService,
    private dialog: MatDialog
  ) {}

  ngOnDestroy(): void {
    // Timer cleanup handled by ExamTimerComponent
  }

  // --- Pre-Exam ---

  startExam(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.examService.startSession({
      shiftId: this.shiftId,
      examId: this.examId,
      candidateId: this.candidateId
    }).subscribe({
      next: (session) => {
        this.session = session;
        this.state = 'IN_PROGRESS';
        this.isLoading = false;
        this.loadQuestion(1);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Failed to start exam session. Please try again.';
      }
    });
  }

  // --- In-Progress ---

  private loadQuestion(sequenceNumber: number): void {
    if (!this.session) return;

    this.examService.getQuestion(this.session.sessionId, sequenceNumber).subscribe({
      next: (question) => {
        // Save time spent on previous question
        this.currentQuestion = question;
        this.visitedQuestions.add(sequenceNumber);
        this.questionStartTime = Date.now();

        // Restore previous answer if any
        this.selectedOptionIds = [];
        this.enteredValue = '';
      },
      error: () => {
        this.errorMessage = 'Failed to load question. Please try again.';
      }
    });
  }

  onOptionSelected(optionIds: string[]): void {
    this.selectedOptionIds = optionIds;
  }

  onValueEntered(value: string): void {
    this.enteredValue = value;
  }

  saveAndNext(): void {
    this.saveCurrentResponse('SAVE_AND_NEXT');
    if (this.currentQuestion && this.session) {
      const next = this.currentQuestion.sequenceNumber + 1;
      if (next <= this.session.totalQuestions) {
        this.loadQuestion(next);
      }
    }
  }

  previousQuestion(): void {
    if (this.currentQuestion && this.currentQuestion.sequenceNumber > 1) {
      this.loadQuestion(this.currentQuestion.sequenceNumber - 1);
    }
  }

  nextQuestion(): void {
    if (this.currentQuestion && this.session) {
      const next = this.currentQuestion.sequenceNumber + 1;
      if (next <= this.session.totalQuestions) {
        this.loadQuestion(next);
      }
    }
  }

  navigateToQuestion(sequenceNumber: number): void {
    this.loadQuestion(sequenceNumber);
  }

  markForReview(): void {
    if (this.currentQuestion) {
      const seq = this.currentQuestion.sequenceNumber;
      if (this.markedQuestions.has(seq)) {
        this.markedQuestions.delete(seq);
      } else {
        this.markedQuestions.add(seq);
      }
    }
  }

  clearResponse(): void {
    this.selectedOptionIds = [];
    this.enteredValue = '';
    // Remove from answered set
    if (this.currentQuestion) {
      this.answeredQuestions.delete(this.currentQuestion.sequenceNumber);
    }
  }

  openSubmitDialog(): void {
    const dialogRef = this.dialog.open(ConfirmSubmitDialogComponent, {
      width: '400px',
      data: {
        totalQuestions: this.session?.totalQuestions || 0,
        answeredCount: this.answeredQuestions.size
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.submitExam();
      }
    });
  }

  onTimeUp(): void {
    this.saveCurrentResponse('AUTO_SUBMIT');
    this.submitExam();
  }

  private submitExam(): void {
    this.state = 'SUBMITTED';
  }

  private saveCurrentResponse(source: string): void {
    if (!this.currentQuestion || !this.session) return;

    const questionId = this.currentQuestion.id;
    const timeSpent = Date.now() - this.questionStartTime;

    // Accumulate time
    const prevTime = this.cumulativeTimeMap.get(questionId) || 0;
    const cumulativeTime = prevTime + timeSpent;
    this.cumulativeTimeMap.set(questionId, cumulativeTime);

    // Increment revision
    const prevRevision = this.revisionMap.get(questionId) || 0;
    const revision = prevRevision + 1;
    this.revisionMap.set(questionId, revision);

    // Track answered state
    if (this.selectedOptionIds.length > 0 || this.enteredValue.trim()) {
      this.answeredQuestions.add(this.currentQuestion.sequenceNumber);
    }

    const payload: ResponsePayload = {
      sessionId: this.session.sessionId,
      questionId: questionId,
      candidateId: this.candidateId,
      selectedOptionIds: this.selectedOptionIds.length > 0 ? this.selectedOptionIds : undefined,
      enteredValue: this.enteredValue.trim() || undefined,
      timestamp: new Date().toISOString(),
      cumulativeTimeSpentMs: cumulativeTime,
      revisionSequence: revision,
      saveSource: source
    };

    this.examService.saveResponse(payload).subscribe({
      error: () => {
        // Silently handle save failure for now
      }
    });
  }
}
