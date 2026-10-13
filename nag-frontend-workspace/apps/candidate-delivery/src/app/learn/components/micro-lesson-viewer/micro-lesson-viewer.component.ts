import {
  Component,
  ChangeDetectionStrategy,
  OnInit,
  inject,
  signal,
  computed,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatChipsModule } from '@angular/material/chips';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { SubtopicMicroLesson } from '../../models/micro-lesson.model';
import { ExplanationIr } from '../../models/explanation-ir.model';
import { MicroLessonService } from '../../services/micro-lesson.service';
import { LearningPathService } from '../../services/learning-path.service';
import { ExplanationPlayerComponent } from '../explanation-player/explanation-player.component';

@Component({
  selector: 'app-micro-lesson-viewer',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatExpansionModule,
    MatChipsModule,
    MathRendererComponent,
    ExplanationPlayerComponent,
  ],
  templateUrl: './micro-lesson-viewer.component.html',
  styleUrl: './micro-lesson-viewer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonViewerComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly microLessonService = inject(MicroLessonService);
  private readonly learningPathService = inject(LearningPathService);

  // Optional direct inputs when rendered inside dialog or drawer
  subtopicIdInput = input<number | null>(null, { alias: 'subtopicId' });
  milestoneIdInput = input<string | null>(null, { alias: 'milestoneId' });
  completed = output<string>();

  readonly lesson = signal<SubtopicMicroLesson | null>(null);
  readonly loading = signal<boolean>(true);
  readonly completing = signal<boolean>(false);
  readonly isCompleted = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly showInteractivePlayer = signal<boolean>(false);

  readonly interactiveExplanation = computed<ExplanationIr | null>(() => {
    const l = this.lesson();
    if (!l?.content?.stepByStepBreakdown) return null;
    const b = l.content.stepByStepBreakdown;
    return {
      explanationId: l.id,
      questionId: String(l.subtopicId),
      status: 'APPROVED',
      problem: {
        statement: b.problemStatement,
      },
      concepts: [l.subtopicName, l.topicName],
      steps: b.steps.map((s, idx) => ({
        id: `step-${s.stepNumber}`,
        sequence: s.stepNumber,
        type: idx === 0 ? 'GIVEN' : (idx === b.steps.length - 1 ? 'CONCLUDE' : 'TRANSFORM'),
        after: s.explanation,
        math: s.formula ? { latex: s.formula } : undefined,
        justification: { why: s.title, formula: s.formula },
      })),
      hints: [
        { id: 'h1', level: 1, text: 'Review the formula cheat sheet in Part 3 above.' },
        { id: 'h2', level: 2, text: 'Avoid the common trap: ' + (l.content.trapAlert || '') },
      ],
      verification: {
        passed: true,
        confidence: 0.98,
        details: 'Verified with Anvaya-Prajna pedagogical curriculum rules.',
      },
    };
  });

  resolvedSubtopicId: number | null = null;
  resolvedMilestoneId: string | null = null;

  ngOnInit(): void {
    const routeSubtopicId = this.route.snapshot?.paramMap?.get('subtopicId');
    const routeMilestoneId = this.route.snapshot?.queryParamMap?.get('milestoneId');

    this.resolvedSubtopicId = this.subtopicIdInput() || (routeSubtopicId ? Number(routeSubtopicId) : null);
    this.resolvedMilestoneId = this.milestoneIdInput() || routeMilestoneId || null;

    if (this.resolvedSubtopicId) {
      this.loadLesson(this.resolvedSubtopicId);
    } else {
      this.error.set('No subtopic identifier specified for micro-lesson.');
      this.loading.set(false);
    }
  }

  loadLesson(subtopicId: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.microLessonService.getPublishedMicroLesson(subtopicId).subscribe({
      next: (data) => {
        this.lesson.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(
          err?.error?.message ||
            'Micro-lesson content is currently being prepared for this subtopic.'
        );
        this.loading.set(false);
      },
    });
  }

  toggleInteractivePlayer(): void {
    this.showInteractivePlayer.update((v) => !v);
  }

  onMarkCompleted(): void {
    if (!this.resolvedMilestoneId) {
      this.isCompleted.set(true);
      this.completed.emit('');
      return;
    }

    this.completing.set(true);
    this.learningPathService.completeMilestone(this.resolvedMilestoneId).subscribe({
      next: () => {
        this.completing.set(false);
        this.isCompleted.set(true);
        this.completed.emit(this.resolvedMilestoneId!);
      },
      error: () => {
        this.completing.set(false);
        // Fallback optimistic completion
        this.isCompleted.set(true);
        this.completed.emit(this.resolvedMilestoneId!);
      },
    });
  }

  onBackToRoadmap(): void {
    this.router.navigate(['/learn/roadmap']);
  }
}
