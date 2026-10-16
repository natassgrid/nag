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
    if (cp === 'DIAGNOSTIC_REQUIRED') return 0;
    if (cp === 'PHASE_1_REMEDIATION') return 1;
    if (cp === 'PHASE_2_ADAPTIVE_QUIZ') return 2;
    if (cp === 'PHASE_3_GATEKEEPER') return 3;
    if (cp === 'COMPLETED') return 4;
    if (typeof cp === 'number') return cp;
    const num = Number(cp);
    return isNaN(num) ? 1 : num;
  });

  readonly isDiagnosticRequired = computed<boolean>(() => {
    const cp = this.learningPath()?.milestoneGraph?.currentPhase;
    return cp === 'DIAGNOSTIC_REQUIRED' || this.currentPhaseNumber() === 0;
  });

  readonly gatekeeperPassed = computed<boolean>(() => {
    const lp = this.learningPath();
    return !!(lp?.gatekeeperPassed || lp?.milestoneGraph?.gatekeeperPassed);
  });

  readonly overallProgressPercent = computed<number>(() => {
    const lp = this.learningPath();
    if (!lp) return 0;
    const cp = lp.milestoneGraph?.currentPhase;
    if (cp === 'DIAGNOSTIC_REQUIRED' || this.currentPhaseNumber() === 0) {
      return 0;
    }
    if (
      lp.status === 'COMPLETED' ||
      lp.gatekeeperPassed ||
      lp.milestoneGraph?.gatekeeperPassed ||
      cp === 'COMPLETED'
    ) {
      return 100;
    }

    const phase1 = lp.milestoneGraph?.phases?.find((p) => p.phaseNumber === 1);
    const items = phase1?.items || [];
    const completedItems = items.filter((i) => this.isItemCompleted(i)).length;
    const p1Progress =
      items.length > 0 ? (completedItems / items.length) * 40 : 40;

    const currentP = this.currentPhaseNumber();
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

  isItemCompleted(item: MilestoneItem): boolean {
    return item.completed === true || item.status === 'COMPLETED';
  }

  getItemId(item: MilestoneItem): string {
    return (
      item.milestoneId ||
      item.id ||
      String(item.subtopicId || item.subtopicName || '')
    );
  }

  private hashString(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      hash = (hash << 5) - hash + str.charCodeAt(i);
      hash |= 0;
    }
    return hash;
  }

  onStudyMicroLesson(item: MilestoneItem): void {
    const milestoneId = item.milestoneId || item.id;
    const subtopicId =
      item.subtopicId ||
      Math.abs(this.hashString(item.subtopicName || 'general'));
    this.router.navigate(['/learn/lesson', subtopicId], {
      queryParams: {
        milestoneId,
        subtopicName: item.subtopicName,
        topicName: item.topicName,
        subjectName: item.subjectName,
        targetExam:
          this.learningPath()?.targetExam ||
          this.learningPath()?.examTarget ||
          'JEE_MAIN',
      },
    });
  }

  onStartAdaptiveQuiz(): void {
    this.actionInProgress.set(true);
    this.learningPathService.startAdaptiveQuiz().subscribe({
      next: (res) => {
        this.message.set(
          `Adaptive quiz session ${res.quizSessionId.slice(
            0,
            8
          )} started! Pass score: ${res.targetAccuracy}%.`
        );
        this.actionInProgress.set(false);
        const setId = res.practiceSetId || res.quizSessionId;
        this.router.navigate(['/practice'], { queryParams: { setId } });
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Could not launch adaptive quiz.');
        this.actionInProgress.set(false);
      },
    });
  }

  onStartGatekeeper(): void {
    this.actionInProgress.set(true);
    this.learningPathService.startGatekeeper().subscribe({
      next: (res) => {
        this.message.set(
          `Gatekeeper test session ${res.gatekeeperSessionId.slice(
            0,
            8
          )} started! Required score: >= ${res.passThreshold}%.`
        );
        this.actionInProgress.set(false);
        const setId = res.practiceSetId || res.gatekeeperSessionId;
        this.router.navigate(['/practice'], { queryParams: { setId } });
      },
      error: (err) => {
        this.error.set(
          err?.error?.message || 'Could not launch Gatekeeper test.'
        );
        this.actionInProgress.set(false);
      },
    });
  }
}
