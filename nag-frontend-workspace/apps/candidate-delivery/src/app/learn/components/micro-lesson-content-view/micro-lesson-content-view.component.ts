import {
  Component,
  ChangeDetectionStrategy,
  computed,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  SubtopicMicroLesson,
  TrapAlert,
  FormulaEntry,
  StepItem,
  extractStepsFromLesson,
  extractProblemStatementFromLesson,
  extractFinalAnswerFromLesson,
} from '../../models/micro-lesson.model';

@Component({
  selector: 'app-micro-lesson-content-view',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatCardModule, MathRendererComponent],
  templateUrl: './micro-lesson-content-view.component.html',
  styleUrl: './micro-lesson-content-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonContentViewComponent {
  readonly lesson = input.required<SubtopicMicroLesson>();

  readonly steps = computed<StepItem[]>(() => extractStepsFromLesson(this.lesson()));
  readonly problemStatement = computed<string>(() => extractProblemStatementFromLesson(this.lesson()));
  readonly finalAnswer = computed<string>(() => extractFinalAnswerFromLesson(this.lesson()));

  asTrapAlert(trap: any): TrapAlert | null {
    return typeof trap === 'object' && trap !== null && 'title' in trap ? (trap as TrapAlert) : null;
  }

  asFormulaArray(sheet: any): FormulaEntry[] | null {
    if (!Array.isArray(sheet)) return null;
    const valid = (sheet as FormulaEntry[]).filter(
      (f) => f && ((f.latex && f.latex.trim().length > 0) || (f.name && f.name.trim().length > 0))
    );
    return valid.length > 0 ? valid : null;
  }

  hasFormulas(sheet: any): boolean {
    if (!sheet) return false;
    if (Array.isArray(sheet)) {
      return sheet.some(
        (f) =>
          (typeof f === 'string' && f.trim().length > 0) ||
          (f && typeof f === 'object' && ((f.latex && f.latex.trim().length > 0) || (f.name && f.name.trim().length > 0)))
      );
    }
    if (typeof sheet === 'string') {
      return sheet.trim().length > 0;
    }
    return false;
  }

  formatLatex(latex: string): string {
    let trimmed = (latex || '').trim();
    if (!trimmed) return '';
    const colonIdx = trimmed.indexOf(':');
    const dollarIdx = trimmed.indexOf('$');
    const slashIdx = trimmed.indexOf('\\');
    if (
      colonIdx > 0 &&
      (dollarIdx > colonIdx || (dollarIdx === -1 && slashIdx > colonIdx))
    ) {
      trimmed = trimmed.substring(colonIdx + 1).trim();
    }
    if (trimmed.includes('$$') || trimmed.includes('$')) return trimmed;
    return `$$${trimmed}$$`;
  }

  asString(val: any): string {
    return typeof val === 'string' ? val : '';
  }
}
