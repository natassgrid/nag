import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'nag-exam-form-general',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './exam-form-general.component.html',
  styleUrl: './exam-form-general.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamFormGeneralComponent {
  readonly name = input<string>('');
  readonly code = input<string>('');
  readonly authority = input<string>('');
  readonly category = input<string>('');
  readonly academicYear = input<string>('2025-2026');

  readonly nameChange = output<string>();
  readonly codeChange = output<string>();
  readonly authorityChange = output<string>();
  readonly categoryChange = output<string>();
  readonly academicYearChange = output<string>();
}
