/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Component, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
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
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./exam-delivery.component.scss']
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

  // Timing & periodic sync
  private questionStartTime = Date.now();
  private cumulativeTimeMap = new Map<string, number>();
  private revisionMap = new Map<string, number>();
  private autosaveTimer: any = null;

  constructor(
    private examService: ExamService,
    private dialog: MatDialog
  ) {}

  ngOnDestroy(): void {
    this.stopAutosave();
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

        // Apply kiosk mode if enforced
        if (session.kioskModeEnforced) {
          this.requestFullScreen();
        }

        // Setup dynamic autosave interval
        const intervalSec = session.autosaveIntervalSeconds || 15;
        this.startAutosave(intervalSec);

        this.loadQuestion(1);
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Failed to start exam session. Please try again.';
      }
    });
  }

  private requestFullScreen(): void {
    if (document.documentElement.requestFullscreen) {
      document.documentElement.requestFullscreen().catch(err => {
        console.warn('Fullscreen request failed:', err);
      });
    }
  }

  private startAutosave(seconds: number): void {
    this.stopAutosave();
    this.autosaveTimer = setInterval(() => {
      if (this.state === 'IN_PROGRESS' && this.currentQuestion) {
        this.saveCurrentResponse('AUTOSAVE');
      }
    }, seconds * 1000);
  }

  private stopAutosave(): void {
    if (this.autosaveTimer) {
      clearInterval(this.autosaveTimer);
      this.autosaveTimer = null;
    }
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
    this.stopAutosave();
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
    if (this.selectedOptionIds.length > 0 || (this.enteredValue && this.enteredValue.trim())) {
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
