import {
  Component,
  ChangeDetectionStrategy,
  OnInit,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AdaptiveLearningService } from '../../services/adaptive-learning.service';
import { OnboardingProfileComponent } from '../onboarding-profile/onboarding-profile.component';
import { ExamPickerComponent } from '../exam-picker/exam-picker.component';
import { DiagnosticRunnerComponent } from '../diagnostic-runner/diagnostic-runner.component';
import {
  LearnerProfile,
  DiagnosticSession,
  DiagnosticSubmitRequest,
  DiagnosticSubmitResult,
} from '../../models/learner.model';

export type OnboardingStep = 'profile' | 'exam-picker' | 'diagnostic';

@Component({
  selector: 'app-learner-onboarding-flow',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    OnboardingProfileComponent,
    ExamPickerComponent,
    DiagnosticRunnerComponent,
  ],
  templateUrl: './learner-onboarding-flow.component.html',
  styleUrl: './learner-onboarding-flow.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LearnerOnboardingFlowComponent implements OnInit {
  private readonly learningService = inject(AdaptiveLearningService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly currentStep = signal<OnboardingStep>('profile');
  readonly diagnosticSession = signal<DiagnosticSession | null>(null);
  readonly diagnosticResult = signal<DiagnosticSubmitResult | null>(null);
  readonly submittingDiagnostic = signal<boolean>(false);

  readonly profile = this.learningService.profile;
  readonly activeEnrollment = this.learningService.activeEnrollment;
  readonly publishedExams = this.learningService.publishedExams;

  readonly isSwitchMode = computed<boolean>(() => {
    return this.currentStep() === 'exam-picker' && !!this.activeEnrollment();
  });

  ngOnInit(): void {
    const qStep = this.route.snapshot.queryParamMap.get('step') as OnboardingStep;
    if (qStep && ['profile', 'exam-picker', 'diagnostic'].includes(qStep)) {
      this.currentStep.set(qStep);
    } else {
      // Determine step based on learner state
      if (!this.learningService.hasCompletedProfile()) {
        this.currentStep.set('profile');
      } else if (!this.learningService.hasActiveEnrollment()) {
        this.currentStep.set('exam-picker');
      } else {
        this.currentStep.set('exam-picker');
      }
    }

    // Refresh state
    this.learningService.getProfile().subscribe();
    this.learningService.getPublishedExams().subscribe();
    this.learningService.getActiveEnrollment().subscribe();
  }

  setStep(step: OnboardingStep): void {
    this.currentStep.set(step);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { step },
      queryParamsHandling: 'merge',
    });
  }

  onSaveProfile(data: Partial<LearnerProfile>): void {
    this.learningService.updateProfile(data).subscribe({
      next: () => {
        this.setStep('exam-picker');
      },
    });
  }

  onSelectExam(event: { examId: string; targetDate?: string }): void {
    this.learningService
      .createEnrollment({
        examinationId: event.examId,
        targetDate: event.targetDate,
      })
      .subscribe({
        next: (enrollment) => {
          this.learningService.startDiagnostic(enrollment.id).subscribe({
            next: (session) => {
              this.diagnosticSession.set(session);
              this.setStep('diagnostic');
            },
          });
        },
      });
  }

  onSubmitDiagnostic(req: DiagnosticSubmitRequest): void {
    const enrollment = this.activeEnrollment();
    if (!enrollment) return;

    this.submittingDiagnostic.set(true);
    this.learningService.submitDiagnostic(enrollment.id, req).subscribe({
      next: (res) => {
        this.submittingDiagnostic.set(false);
        this.diagnosticResult.set(res);
      },
      error: () => {
        this.submittingDiagnostic.set(false);
      },
    });
  }

  onSkipDiagnostic(): void {
    const enrollment = this.activeEnrollment();
    if (!enrollment) return;

    this.learningService.skipDiagnostic(enrollment.id).subscribe({
      next: (res) => {
        this.diagnosticResult.set(res);
      },
    });
  }

  onFinishFlow(): void {
    this.router.navigate(['/learn/recommendations']);
  }
}
