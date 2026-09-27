import {
  ChangeDetectionStrategy,
  Component,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { SubjectScore } from '../../models';

@Component({
  selector: 'nag-results-section-table',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './results-section-table.component.html',
  styleUrl: './results-section-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResultsSectionTableComponent {
  readonly subjectScores = input.required<SubjectScore[]>();
}
