import {
  Component,
  ChangeDetectionStrategy,
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
    const trimmed = (latex || '').trim();
    if (!trimmed) return '';
    if (trimmed.includes('$$') || trimmed.includes('$')) return trimmed;
    return `$$${trimmed}$$`;
  }

  asString(val: any): string {
    return typeof val === 'string' ? val : '';
  }
}
