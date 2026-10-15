import {
  Component,
  ChangeDetectionStrategy,
  OnInit,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  DynamicLearningPath,
  MilestonePhase,
  MilestoneItem,
} from '../../models/learning-path.model';
import { LearningPathService } from '../../services/learning-path.service';

@Component({
  selector: 'app-dlp-roadmap',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatChipsModule,
    MatTooltipModule,
  ],
  templateUrl: './dlp-roadmap.component.html',
  styleUrl: './dlp-roadmap.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DlpRoadmapComponent implements OnInit {
  private readonly learningPathService = inject(LearningPathService);
  private readonly router = inject(Router);

  readonly learningPath = signal<DynamicLearningPath | null>(null);
  readonly loading = signal<boolean>(true);
  readonly actionInProgress = signal<boolean>(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  readonly phases = computed<MilestonePhase[]>(() => {
    return this.learningPath()?.milestoneGraph?.phases || [];
  });

  readonly currentPhaseNumber = computed<number>(() => {
    const cp = this.learningPath()?.milestoneGraph?.currentPhase;
    return typeof cp === 'number' ? cp : Number(cp) || 1;
  });

  readonly gatekeeperPassed = computed<boolean>(() => {
    return !!this.learningPath()?.milestoneGraph?.gatekeeperPassed;
  });

  readonly overallProgressPercent = computed<number>(() => {
    const lp = this.learningPath();
    if (!lp) return 0;
    if (lp.status === 'COMPLETED' || lp.gatekeeperPassed) return 100;

    const phase1 = lp.milestoneGraph?.phases?.find((p) => p.phaseNumber === 1);
    const items = phase1?.items || [];
    const completedItems = items.filter((i) => i.status === 'COMPLETED').length;
    const p1Progress = items.length > 0 ? (completedItems / items.length) * 40 : 40;

    const cp = lp.milestoneGraph?.currentPhase;
    const currentP = typeof cp === 'number' ? cp : Number(cp) || 1;
    if (currentP === 1) {
      return Math.round(p1Progress);
    } else if (currentP === 2) {
      return Math.round(40 + 30);
    } else if (currentP === 3) {
      return Math.round(40 + 30 + 20);
    }
    return 100;
  });

  ngOnInit(): void {
    this.loadLearningPath();
  }

  loadLearningPath(): void {
    this.loading.set(true);
    this.error.set(null);
    this.learningPathService.getLearningPath().subscribe({
      next: (data) => {
        this.learningPath.set(data);
        this.loading.set(false);
      },
      error: () => {
        // If not found or initial state, attempt to generate
        this.generateLearningPath();
      },
    });
  }

  generateLearningPath(): void {
    this.loading.set(true);
    this.learningPathService.generateLearningPath().subscribe({
      next: (data) => {
        this.learningPath.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(
          err?.error?.message ||
            'Unable to initialize Dynamic Learning Path. Please complete a practice test to diagnose focus areas.'
        );
        this.loading.set(false);
      },
    });
  }

  onStudyMicroLesson(item: MilestoneItem): void {
    if (item.subtopicId) {
      this.router.navigate(['/learn/lesson', item.subtopicId], {
        queryParams: { milestoneId: item.milestoneId },
      });
    }
  }

  onStartAdaptiveQuiz(): void {
    this.actionInProgress.set(true);
    this.learningPathService.startAdaptiveQuiz().subscribe({
      next: (res) => {
        this.message.set(
          `Adaptive quiz session ${res.quizSessionId.slice(0, 8)} started! Pass score: ${res.targetAccuracy}%.`
        );
        // Simulate immediate test submission for demo/testing or navigate
        this.actionInProgress.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Could not launch adaptive quiz.');
        this.actionInProgress.set(false);
      },
    });
  }

  onSubmitAdaptiveQuizScore(score: number): void {
    const phase2 = this.phases().find((p) => p.phaseNumber === 2);
    const sessionId = phase2?.quizSessionId || 'quiz-session-auto';

    this.actionInProgress.set(true);
    this.learningPathService.submitAdaptiveQuiz(sessionId, score).subscribe({
      next: (updated) => {
        this.learningPath.set(updated);
        this.actionInProgress.set(false);
        this.message.set(
          score >= 70
            ? 'Congratulations! You passed the Adaptive Quiz and unlocked the Phase 3 Gatekeeper Test.'
            : 'Quiz score submitted. Review Phase 1 micro-lessons and re-attempt to advance.'
        );
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Failed to submit quiz score.');
        this.actionInProgress.set(false);
      },
    });
  }

  onStartGatekeeper(): void {
    this.actionInProgress.set(true);
    this.learningPathService.startGatekeeper().subscribe({
      next: (res) => {
        this.message.set(
          `Gatekeeper test session ${res.gatekeeperSessionId.slice(0, 8)} started! Required score: >= ${res.passThreshold}%.`
        );
        this.actionInProgress.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Could not launch Gatekeeper test.');
        this.actionInProgress.set(false);
      },
    });
  }

  onSubmitGatekeeperScore(score: number): void {
    const phase3 = this.phases().find((p) => p.phaseNumber === 3);
    const sessionId = phase3?.gatekeeperSessionId || 'gatekeeper-session-auto';

    this.actionInProgress.set(true);
    this.learningPathService.submitGatekeeper(sessionId, score).subscribe({
      next: (updated) => {
        this.learningPath.set(updated);
        this.actionInProgress.set(false);
        this.message.set(
          score >= 75
            ? 'Gatekeeper Passed! All full-length mock examinations are now fully unlocked!'
            : 'Gatekeeper score below 75%. Review weak areas and retake test to unlock mock exams.'
        );
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Failed to submit gatekeeper score.');
        this.actionInProgress.set(false);
      },
    });
  }
}
