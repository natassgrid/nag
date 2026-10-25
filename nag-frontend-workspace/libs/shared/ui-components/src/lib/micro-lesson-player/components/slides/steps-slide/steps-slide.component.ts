import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MathRendererComponent } from '../../../../math-renderer.component';
import {
  PlayerSubtopicMicroLesson,
  PlayerMicroLessonStep,
  extractStepsFromLesson,
  extractProblemStatementFromLesson,
  extractFinalAnswerFromLesson,
  formatLatex,
} from '../../../micro-lesson-player.model';

@Component({
  selector: 'nag-steps-slide, app-steps-slide',
  standalone: true,
  imports: [CommonModule, MatIconModule, MathRendererComponent],
  templateUrl: './steps-slide.component.html',
  styleUrl: './steps-slide.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StepsSlideComponent {
  readonly lesson = input<PlayerSubtopicMicroLesson | null>(null);
  readonly revealedStepCount = input<number>(0);
  readonly isPlaying = input<boolean>(false);

  readonly startAutoPlay = output<void>();
  readonly revealNextStep = output<void>();
  readonly revealAllSteps = output<void>();
  readonly resetSteps = output<void>();

  readonly formatLatex = formatLatex;

  readonly allSteps = computed<PlayerMicroLessonStep[]>(() => {
    return extractStepsFromLesson(this.lesson());
  });

  readonly visibleSteps = computed<PlayerMicroLessonStep[]>(() => {
    return this.allSteps().slice(0, this.revealedStepCount());
  });

  readonly totalStepsCount = computed<number>(() => {
    return this.allSteps().length;
  });

  readonly problemStatementText = computed<string>(() =>
    extractProblemStatementFromLesson(this.lesson())
  );

  readonly finalAnswerText = computed<string>(() =>
    extractFinalAnswerFromLesson(this.lesson())
  );

  cleanStepExplanation(step: PlayerMicroLessonStep): string {
    if (!step || !step.explanation) return '';
    return (
      step.explanation
        .replace(/^\s*step\s*\d+\s*[:\-.]?\s*/i, '')
        .trim() || step.explanation
    );
  }

  hasDistinctStepTitle(step: PlayerMicroLessonStep): boolean {
    if (!step || !step.title) return false;
    const trimmed = step.title.trim();
    if (/^step\s*\d+\s*[:\-.]?$/i.test(trimmed)) {
      return false;
    }
    return true;
  }
}
