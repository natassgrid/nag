import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { ExamSection } from '@nag-frontend-workspace/examinations-data-access';
import { Subject, TaxonomySubjectNode } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-exam-form-sections',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './exam-form-sections.component.html',
  styleUrl: './exam-form-sections.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamFormSectionsComponent {
  readonly sections = input<ExamSection[]>([]);
  readonly subjects = input<Array<Subject | TaxonomySubjectNode>>([]);

  readonly addSection = output<void>();
  readonly removeSection = output<number>();

  onSubjectSelect(sec: ExamSection, rawId: any): void {
    if (rawId == null || rawId === '' || rawId === 'undefined') {
      sec.subjectId = undefined;
      return;
    }
    const numId = Number(rawId);
    sec.subjectId = isNaN(numId) ? undefined : numId;

    const matchedSubject = this.subjects().find((s) => Number(s.id) === numId);
    if (matchedSubject && (!sec.name || sec.name.startsWith('Section '))) {
      sec.name = matchedSubject.name;
    }
  }
}
