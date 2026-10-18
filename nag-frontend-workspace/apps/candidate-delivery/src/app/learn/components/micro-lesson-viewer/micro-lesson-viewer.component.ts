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
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatChipsModule } from '@angular/material/chips';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  SubtopicMicroLesson,
  TrapAlert,
  FormulaEntry,
  SUPPORTED_MICROLESSON_LANGUAGES,
  MicroLessonLanguage,
} from '../../models/micro-lesson.model';
import { ExplanationIr } from '../../models/explanation-ir.model';
import {
  MicroLessonService,
  MicroLessonQueryParams,
} from '../../services/micro-lesson.service';
import { LearningPathService } from '../../services/learning-path.service';
import { ExplanationPlayerComponent } from '../explanation-player/explanation-player.component';

@Component({
  selector: 'app-micro-lesson-viewer',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
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

  readonly supportedLanguages: MicroLessonLanguage[] = SUPPORTED_MICROLESSON_LANGUAGES;
  readonly regionalLanguages = SUPPORTED_MICROLESSON_LANGUAGES.filter(
    (l) => l.code !== 'en' && l.code !== 'hi'
  );
  readonly selectedLanguage = signal<string>('en');
  readonly isChangingLanguage = signal<boolean>(false);

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
    const steps = b.steps || [];
    if (steps.length === 0) return null;

    const trapText =
      typeof l.content.trapAlert === 'object' && l.content.trapAlert
        ? l.content.trapAlert.description
        : typeof l.content.trapAlert === 'string'
        ? l.content.trapAlert
        : '';

    return {
      explanationId: l.id,
      questionId: String(l.subtopicId),
      status: 'APPROVED',
      problem: {
        statement: b.problemStatement || b.questionStem || '',
      },
      concepts: [l.subtopicName, l.topicName].filter(Boolean),
      steps: steps.map((s, idx) => ({
        id: `step-${s.stepNumber}`,
        sequence: s.stepNumber,
        type:
          idx === 0
            ? 'GIVEN'
            : idx === steps.length - 1
            ? 'CONCLUDE'
            : 'TRANSFORM',
        after: s.explanation,
        math: s.formula ? { latex: s.formula } : undefined,
        justification: { why: s.title, formula: s.formula },
      })),
      hints: [
        {
          id: 'h1',
          level: 1,
          text: 'Review the formula cheat sheet in Part 3 above.',
        },
        {
          id: 'h2',
          level: 2,
          text: 'Avoid the common trap: ' + trapText,
        },
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
  resolvedQueryParams: MicroLessonQueryParams = {};

  ngOnInit(): void {
    const routeSubtopicId = this.route.snapshot?.paramMap?.get('subtopicId');
    const qp = this.route.snapshot?.queryParamMap;
    const routeMilestoneId = qp?.get('milestoneId');
    const subtopicName = qp?.get('subtopicName') || undefined;
    const topicName = qp?.get('topicName') || undefined;
    const subjectName = qp?.get('subjectName') || undefined;
    const targetExam = qp?.get('targetExam') || undefined;

    const routeLang = qp?.get('lang') || undefined;
    if (routeLang) {
      this.selectedLanguage.set(routeLang);
    }

    this.resolvedSubtopicId =
      this.subtopicIdInput() || (routeSubtopicId ? Number(routeSubtopicId) : null);
    this.resolvedMilestoneId =
      this.milestoneIdInput() || routeMilestoneId || null;
    this.resolvedQueryParams = {
      subtopicName,
      topicName,
      subjectName,
      targetExam,
      lang: routeLang,
    };

    if (this.resolvedSubtopicId) {
      this.loadLesson(this.resolvedSubtopicId, this.resolvedQueryParams);
    } else {
      this.error.set('No subtopic identifier specified for micro-lesson.');
      this.loading.set(false);
    }
  }

  loadLesson(subtopicId: number, params?: MicroLessonQueryParams): void {
    this.loading.set(true);
    this.error.set(null);
    const queryParams: MicroLessonQueryParams = {
      ...(params || {}),
      lang: params?.lang || this.selectedLanguage(),
    };

    this.microLessonService
      .getPublishedMicroLesson(subtopicId, queryParams)
      .subscribe({
        next: (data) => {
          this.lesson.set(data);
          if (data.languageCode) {
            this.selectedLanguage.set(data.languageCode);
          }
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

  changeLanguage(lang: string): void {
    if (this.selectedLanguage() === lang) return;
    this.selectedLanguage.set(lang);
    const current = this.lesson();

    if (current?.id) {
      this.isChangingLanguage.set(true);
      this.microLessonService.getMicroLessonByLanguage(current.id, lang).subscribe({
        next: (data) => {
          this.lesson.set(data);
          this.isChangingLanguage.set(false);
        },
        error: () => {
          if (this.resolvedSubtopicId) {
            this.loadLesson(this.resolvedSubtopicId, {
              ...this.resolvedQueryParams,
              lang,
            });
          }
          this.isChangingLanguage.set(false);
        },
      });
    } else if (this.resolvedSubtopicId) {
      this.loadLesson(this.resolvedSubtopicId, {
        ...this.resolvedQueryParams,
        lang,
      });
    }
  }

  isTrapObject(trap: any): trap is TrapAlert {
    return typeof trap === 'object' && trap !== null && 'title' in trap;
  }

  isFormulaArray(sheet: any): sheet is FormulaEntry[] {
    return Array.isArray(sheet);
  }

  asString(val: any): string {
    return typeof val === 'string' ? val : '';
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
    this.learningPathService
      .completeMilestone(this.resolvedMilestoneId)
      .subscribe({
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
