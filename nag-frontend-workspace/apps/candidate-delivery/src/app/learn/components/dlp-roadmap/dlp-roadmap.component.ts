import {
  Component,
  ChangeDetectionStrategy,
  OnInit,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import {
  DynamicLearningPath,
  MilestonePhase,
  MilestoneItem,
} from '../../models/learning-path.model';
import { SubtopicMastery } from '../../models/learner.model';
import { LearningPathService } from '../../services/learning-path.service';
import { AdaptiveLearningService } from '../../services/adaptive-learning.service';
import { ReadinessScoreMeterComponent } from '../readiness-score-meter/readiness-score-meter.component';
import { SyllabusMasteryTreeComponent } from '../syllabus-mastery-tree/syllabus-mastery-tree.component';
import { PhasedMilestonesListComponent } from '../phased-milestones-list/phased-milestones-list.component';

@Component({
  selector: 'app-dlp-roadmap',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    ReadinessScoreMeterComponent,
    SyllabusMasteryTreeComponent,
    PhasedMilestonesListComponent,
  ],
  templateUrl: './dlp-roadmap.component.html',
  styleUrl: './dlp-roadmap.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DlpRoadmapComponent implements OnInit {
  private readonly learningPathService = inject(LearningPathService);
  private readonly learningService = inject(AdaptiveLearningService);
  private readonly router = inject(Router);

  readonly learningPath = signal<DynamicLearningPath | null>(null);
  readonly loading = signal<boolean>(true);
  readonly actionInProgress = signal<boolean>(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  readonly activeEnrollment = this.learningService.activeEnrollment;
  readonly readinessScore = this.learningService.readinessScore;
  readonly progress = this.learningService.progress;

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
    const num = Number(cp);
    return isNaN(num) ? 1 : num;
  });

  readonly isDiagnosticRequired = computed<boolean>(() => {
    return this.currentPhaseNumber() === 0;
  });

  readonly overallProgressPercent = computed<number>(() => {
    const lp = this.learningPath();
    if (!lp || this.isDiagnosticRequired()) return 0;
    if (lp.status === 'COMPLETED' || lp.gatekeeperPassed) return 100;
    const phase1 = lp.milestoneGraph?.phases?.find((p) => p.phaseNumber === 1);
    const items = phase1?.items || [];
    const completed = items.filter((i) => i.completed || i.status === 'COMPLETED').length;
    const p1Progress = items.length > 0 ? (completed / items.length) * 40 : 40;
    if (this.currentPhaseNumber() === 1) return Math.round(p1Progress);
    if (this.currentPhaseNumber() === 2) return 70;
    if (this.currentPhaseNumber() === 3) return 90;
    return 100;
  });

  readonly gatekeeperPassed = computed<boolean>(() => !!this.learningPath()?.gatekeeperPassed);

  ngOnInit(): void {
    this.loadLearningPath();
  }

  loadLearningPath(): void {
    this.loading.set(true);
    this.learningPathService.getLearningPath().subscribe({
      next: (data) => {
        this.learningPath.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.learningPathService.generateLearningPath().subscribe({
          next: (data) => {
            this.learningPath.set(data);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        });
      },
    });
  }

  onStudyLesson(item: MilestoneItem): void {
    this.onStudyMicroLesson(item);
  }

  onStudyMicroLesson(item: MilestoneItem): void {
    const subtopicId = item.subtopicId || 301;
    this.router.navigate(['/learn/lesson', subtopicId], {
      queryParams: { milestoneId: item.milestoneId || item.id },
    });
  }

  onSelectSubtopicFromTree(subtopic: SubtopicMastery): void {
    this.router.navigate(['/learn/lesson', subtopic.subtopicId]);
  }

  onPracticeFromTree(subtopic: SubtopicMastery): void {
    this.router.navigate(['/practice']);
  }

  onStartQuiz(phase?: MilestonePhase): void {
    this.actionInProgress.set(true);
    this.learningPathService.startAdaptiveQuiz().subscribe({
      next: (res) => {
        this.actionInProgress.set(false);
        this.router.navigate(['/practice'], { queryParams: { setId: res.practiceSetId || res.quizSessionId } });
      },
      error: () => this.actionInProgress.set(false),
    });
  }

  onStartAdaptiveQuiz(phase?: MilestonePhase): void {
    this.onStartQuiz(phase);
  }

  onSubmitAdaptiveQuizScore(score: number, sessionId = 'quiz-session'): void {
    this.learningPathService.submitAdaptiveQuiz(sessionId, score).subscribe({
      next: (data) => this.learningPath.set(data),
    });
  }

  onStartGatekeeper(phase?: MilestonePhase): void {
    this.actionInProgress.set(true);
    this.learningPathService.startGatekeeper().subscribe({
      next: (res) => {
        this.actionInProgress.set(false);
        this.router.navigate(['/practice'], { queryParams: { setId: res.practiceSetId || res.gatekeeperSessionId } });
      },
      error: () => this.actionInProgress.set(false),
    });
  }

  onSubmitGatekeeperScore(score: number, sessionId = 'gatekeeper-session'): void {
    this.learningPathService.submitGatekeeper(sessionId, score).subscribe({
      next: (data) => this.learningPath.set(data),
    });
  }

  onStartDiagnosticTest(): void {
    this.router.navigate(['/learn/onboarding'], { queryParams: { step: 'diagnostic' } });
  }
}
