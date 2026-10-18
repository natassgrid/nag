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
    return Array.isArray(sheet) ? (sheet as FormulaEntry[]) : null;
  }

  asString(val: any): string {
    return typeof val === 'string' ? val : '';
  }
}
