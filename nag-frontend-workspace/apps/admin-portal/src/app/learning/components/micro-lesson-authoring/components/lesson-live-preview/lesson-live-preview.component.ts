import {
  Component,
  ChangeDetectionStrategy,
  computed,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  FormulaEntry,
  MicroLessonStep,
  QuickCheckItem,
  TrapAlert,
} from '../../../../models/micro-lesson.model';

@Component({
  selector: 'app-lesson-live-preview',
  standalone: true,
  imports: [CommonModule, MatIconModule, MathRendererComponent],
  templateUrl: './lesson-live-preview.component.html',
  styleUrl: './lesson-live-preview.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LessonLivePreviewComponent {
  readonly subjectName = input<string>('General');
  readonly topicName = input<string>('General');
  readonly subtopicName = input<string>('Subtopic');
  readonly targetExam = input<string>('JEE_MAIN');
  readonly lessonType = input<string>('CONCEPT');
  readonly difficultyLevel = input<string>('CORE');
  readonly estimatedMinutes = input<number>(5);
  readonly status = input<string>('APPROVED');
  readonly coreConcept = input<string>('');
  readonly trapAlert = input<TrapAlert>({
    title: '',
    description: '',
    commonMistake: '',
    correctApproach: '',
  });
  readonly formulas = input<FormulaEntry[]>([]);
  readonly problemStatement = input<string>('');
  readonly steps = input<MicroLessonStep[]>([]);
  readonly finalAnswer = input<string>('');
  readonly keyTakeaways = input<string[]>([]);
  readonly quickCheckItems = input<QuickCheckItem[]>([]);

  readonly nonEmptyTakeaways = computed(() =>
    (this.keyTakeaways() || []).filter((t) => t && t.trim().length > 0)
  );

  readonly nonEmptyQuickChecks = computed(() =>
    (this.quickCheckItems() || []).filter(
      (q) => q && q.question && q.question.trim().length > 0
    )
  );

  formatLatex(latex: string): string {
    const trimmed = (latex || '').trim();
    if (!trimmed) return '';
    if (trimmed.includes('$$') || trimmed.includes('$')) return trimmed;
    return `$$${trimmed}$$`;
  }
}
