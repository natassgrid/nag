import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ScorecardRecord } from '../../models';

@Component({
  selector: 'nag-results-exam-selector',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './results-exam-selector.component.html',
  styleUrl: './results-exam-selector.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResultsExamSelectorComponent {
  readonly scorecards = input.required<ScorecardRecord[]>();
  readonly selected = input<ScorecardRecord | null>(null);

  readonly selectScorecard = output<ScorecardRecord>();
}
