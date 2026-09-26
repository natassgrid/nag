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

  readonly addSection = output<void>();
  readonly removeSection = output<number>();
}
