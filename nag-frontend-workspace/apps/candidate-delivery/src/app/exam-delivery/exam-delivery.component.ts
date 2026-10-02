import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  OnDestroy,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import {
  I18nService,
  SUPPORTED_LANGUAGES,
  SupportedLanguage,
} from '@nag-frontend-workspace/shared-util-i18n';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';
import { ExamDeliveryMode, ExamItem } from './models';
import { ExamDeliveryService } from './services';
import {
  ExamModeBannerComponent,
  ExamRuntimeHeaderComponent,
  ExamQuestionCardComponent,
  ExamQuestionPaletteComponent,
  ExamSubmissionModalComponent,
} from './components';

export * from './models';
export * from './data';
export * from './services';
export * from './components';

@Component({
  selector: 'app-exam-delivery',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
    ExamModeBannerComponent,
    ExamRuntimeHeaderComponent,
    ExamQuestionCardComponent,
    ExamQuestionPaletteComponent,
    ExamSubmissionModalComponent,
  ],
  templateUrl: './exam-delivery.component.html',
  styleUrl: './exam-delivery.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamDeliveryComponent implements OnInit, OnDestroy {
  readonly deliveryService = inject(ExamDeliveryService);
  readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);
  readonly i18nService = inject(I18nService);
  private readonly notificationService = inject(NotificationService);

  readonly supportedLanguages = SUPPORTED_LANGUAGES;

  // Shortcuts proxied from service for template signals
  readonly examId = this.deliveryService.examId;
  readonly deliveryMode = this.deliveryService.deliveryMode;
  readonly sessionMeta = this.deliveryService.sessionMeta;
  readonly remainingSeconds = this.deliveryService.remainingSeconds;
  readonly formattedTime = this.deliveryService.formattedTime;
  readonly questions = this.deliveryService.questions;
  readonly currentIndex = this.deliveryService.currentIndex;
  readonly currentItem = this.deliveryService.currentItem;
  readonly submissionReceipt = this.deliveryService.submissionReceipt;
  readonly totalQuestions = this.deliveryService.totalQuestions;
  readonly countAnswered = this.deliveryService.countAnswered;
  readonly countFlagged = this.deliveryService.countFlagged;
  readonly countUnvisited = this.deliveryService.countUnvisited;

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      const modeParam = (params['mode'] || '').toUpperCase() as ExamDeliveryMode;
      const examId = params['examId'] || null;
      const paperId = params['paperId'] || null;
      const mode: ExamDeliveryMode =
        modeParam === 'PRACTICE' || modeParam === 'PREVIEW' ? modeParam : 'LIVE';

      this.deliveryService.initialize(mode, examId, paperId);
    });

    this.deliveryService.startTimer(() => this.autoSubmit());
  }

  ngOnDestroy(): void {
    this.deliveryService.stopTimer();
  }

  onLanguageChange(lang: SupportedLanguage): void {
    this.i18nService.setLanguage(lang);
  }

  selectOption(item: ExamItem, optionId: string): void {
    this.deliveryService.selectOption(item, optionId);
  }

  clearResponse(item: ExamItem): void {
    this.deliveryService.clearResponse(item);
  }

  toggleFlag(item: ExamItem): void {
    this.deliveryService.toggleFlag(item);
  }

  goToQuestion(index: number): void {
    this.deliveryService.goToQuestion(index);
  }

  nextQuestion(): void {
    this.deliveryService.nextQuestion();
  }

  prevQuestion(): void {
    this.deliveryService.prevQuestion();
  }

  onCloseReceipt(): void {
    this.deliveryService.clearReceipt();
    this.router.navigate(['/dashboard']);
  }

  async handleExitBanner(): Promise<void> {
    const isMock = this.deliveryMode() === 'PRACTICE';
    const confirmed = await this.notificationService.confirm({
      title: isMock ? 'Exit Practice Assessment?' : 'Exit Examination Preview?',
      message: isMock
        ? 'Are you sure you want to exit? Your practice session progress will be discarded.'
        : 'You can return to the dashboard or browse assessments anytime.',
      confirmText: isMock ? 'Exit Assessment' : 'Exit to Dashboard',
      cancelText: 'Stay Here',
      type: isMock ? 'warning' : 'info',
    });
    if (confirmed) {
      this.router.navigate(['/dashboard']);
    }
  }

  async confirmSubmission(): Promise<void> {
    const mode = this.deliveryMode();

    if (mode === 'PREVIEW') {
      await this.handleExitBanner();
      return;
    }

    const answered = this.countAnswered();
    const total = this.totalQuestions();
    const isMock = mode === 'PRACTICE';

    const confirmed = await this.notificationService.confirm({
      title: isMock
        ? 'Complete Practice Mock Session?'
        : 'Finalize and Submit Exam Responses?',
      message: `You have answered ${answered} of ${total} questions.\n${
        isMock
          ? 'Your practice score and instant review breakdown will be generated.'
          : 'Once submitted, your responses will be cryptographically hashed, sealed, and cannot be modified.'
      }`,
      confirmText: isMock ? 'Submit Practice Mock' : 'Submit & Seal Exam',
      cancelText: 'Return to Test',
      type: isMock ? 'info' : 'warning',
    });

    if (confirmed) {
      await this.deliveryService.sealAndSubmit();
    }
  }

  private async autoSubmit(): Promise<void> {
    if (this.deliveryMode() === 'PREVIEW') {
      return;
    }
    this.notificationService.warning(
      'Exam Timer Expired',
      'The allocated examination duration has ended. The system is sealing and submitting your responses.',
      6000
    );
    await this.deliveryService.sealAndSubmit();
  }
}
