import { Component, ChangeDetectionStrategy, OnInit, inject, signal, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { SubtopicMicroLesson, LessonVariantRef } from '../../models/micro-lesson.model';
import { LessonType, LessonDifficultyLevel } from '../../models/learner.model';
import { MicroLessonService, MicroLessonQueryParams } from '../../services/micro-lesson.service';
import { AdaptiveLearningService } from '../../services/adaptive-learning.service';
import { LearningPathService } from '../../services/learning-path.service';
import { MicroLessonVariantSwitcherComponent } from '../micro-lesson-variant-switcher/micro-lesson-variant-switcher.component';
import { MicroLessonContentViewComponent } from '../micro-lesson-content-view/micro-lesson-content-view.component';
import { MicroLessonFeedbackBarComponent } from '../micro-lesson-feedback-bar/micro-lesson-feedback-bar.component';

@Component({
  selector: 'app-micro-lesson-viewer',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatButtonModule, MatIconModule, MatProgressBarModule,
    MicroLessonVariantSwitcherComponent, MicroLessonContentViewComponent, MicroLessonFeedbackBarComponent,
  ],
  templateUrl: './micro-lesson-viewer.component.html',
  styleUrl: './micro-lesson-viewer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonViewerComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly microLessonService = inject(MicroLessonService);
  private readonly learningService = inject(AdaptiveLearningService);
  private readonly learningPathService = inject(LearningPathService);

  subtopicIdInput = input<number | null>(null, { alias: 'subtopicId' });
  milestoneIdInput = input<string | null>(null, { alias: 'milestoneId' });
  completed = output<string>();

  readonly lesson = signal<SubtopicMicroLesson | null>(null);
  readonly allVariants = signal<SubtopicMicroLesson[]>([]);
  readonly loading = signal<boolean>(true);
  readonly completing = signal<boolean>(false);
  readonly isCompleted = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly currentType = signal<LessonType>('CONCEPT');
  readonly currentDifficulty = signal<LessonDifficultyLevel>('CORE');
  readonly userHelpfulRating = signal<number | null>(null);
  readonly selectedLanguage = signal<string>('en');

  resolvedSubtopicId: number | null = null;
  resolvedMilestoneId: string | null = null;
  resolvedQueryParams: MicroLessonQueryParams = {};

  ngOnInit(): void {
    const routeSubId = this.route.snapshot.paramMap.get('subtopicId');
    const qp = this.route.snapshot.queryParamMap;
    this.resolvedSubtopicId = this.subtopicIdInput() || (routeSubId ? Number(routeSubId) : 301);
    this.resolvedMilestoneId = this.milestoneIdInput() || qp.get('milestoneId') || null;
    const routeLang = qp.get('lang');
    if (routeLang) this.selectedLanguage.set(routeLang);
    this.resolvedQueryParams = {
      subtopicName: qp.get('subtopicName') || undefined,
      topicName: qp.get('topicName') || undefined,
      targetExam: qp.get('targetExam') || undefined,
      lang: routeLang || 'en',
    };

    this.currentType.set((qp.get('lessonType') as LessonType) || 'CONCEPT');
    this.currentDifficulty.set((qp.get('difficulty') as LessonDifficultyLevel) || 'CORE');

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
    const params: MicroLessonQueryParams = {
      ...this.resolvedQueryParams,
      lessonType: this.currentType(),
      difficulty: this.currentDifficulty(),
      lang: this.selectedLanguage(),
    };

    if (typeof this.microLessonService.getMicroLessonsBySubtopic === 'function') {
      this.microLessonService.getMicroLessonsBySubtopic(subtopicId, params).subscribe({
        next: (lessons) => {
          this.allVariants.set(lessons || []);
          if (lessons && lessons.length > 0) {
            const matched = lessons.find((l) => l.lessonType === this.currentType() && l.difficultyLevel === this.currentDifficulty()) || lessons[0];
            this.lesson.set(matched);
            if (matched.languageCode) this.selectedLanguage.set(matched.languageCode);
          }
          this.loading.set(false);
        },
        error: () => this.fallbackPublishedLesson(subtopicId, params),
      });
    } else {
      this.fallbackPublishedLesson(subtopicId, params);
    }
  }

  private fallbackPublishedLesson(subtopicId: number, params: MicroLessonQueryParams): void {
    this.microLessonService.getPublishedMicroLesson(subtopicId, params).subscribe({
      next: (data) => {
        this.lesson.set(data);
        this.allVariants.set([data]);
        if (data?.languageCode) this.selectedLanguage.set(data.languageCode);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Micro-lesson content is being generated for this subtopic.');
        this.loading.set(false);
      },
    });
  }

  changeLanguage(lang: string): void {
    if (this.selectedLanguage() === lang) return;
    this.selectedLanguage.set(lang);
    const current = this.lesson();
    if (current?.id && typeof this.microLessonService.getMicroLessonByLanguage === 'function') {
      this.microLessonService.getMicroLessonByLanguage(current.id, lang).subscribe({
        next: (data) => this.lesson.set(data),
        error: () => this.resolvedSubtopicId && this.loadLesson(this.resolvedSubtopicId),
      });
    } else if (this.resolvedSubtopicId) {
      this.loadLesson(this.resolvedSubtopicId);
    }
  }

  onTypeChange(type: LessonType): void {
    this.currentType.set(type);
    if (this.resolvedSubtopicId) this.loadLesson(this.resolvedSubtopicId);
  }

  onDifficultyChange(level: LessonDifficultyLevel): void {
    this.currentDifficulty.set(level);
    if (this.resolvedSubtopicId) this.loadLesson(this.resolvedSubtopicId);
  }

  onTryAnotherExplanation(): void {
    const types: LessonType[] = ['CONCEPT', 'WORKED_EXAMPLES', 'TRAPS_AND_PITFALLS', 'CHEAT_SHEET', 'VISUAL', 'QUICK_RECAP'];
    this.onTypeChange(types[(types.indexOf(this.currentType()) + 1) % types.length]);
  }

  onRateHelpful(rating: number): void {
    this.userHelpfulRating.set(rating);
  }

  onMarkCompleted(): void {
    this.completing.set(true);
    const enrollment = this.learningService.activeEnrollment();
    const aid = this.resolvedMilestoneId || `act-sub-${this.resolvedSubtopicId || 301}`;

    if (enrollment) {
      this.learningService.completeActivity(enrollment.id, aid, { status: 'COMPLETED', helpfulRating: this.userHelpfulRating() || 1 }).subscribe();
    }

    if (this.resolvedMilestoneId) {
      this.learningPathService.completeMilestone(this.resolvedMilestoneId).subscribe({
        next: () => this.finishCompletion(this.resolvedMilestoneId!),
        error: () => this.finishCompletion(this.resolvedMilestoneId!),
      });
    } else {
      this.finishCompletion('');
    }
  }

  private finishCompletion(id: string): void {
    this.completing.set(false);
    this.isCompleted.set(true);
    this.completed.emit(id);
  }

  onBackToRoadmap(): void {
    this.router.navigate(['/learn/roadmap']);
  }
}
