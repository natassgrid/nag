import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { PracticeResult } from '../../models';

@Component({
  selector: 'app-practice-result-summary',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatProgressBarModule, MatIconModule],
  templateUrl: './practice-result-summary.component.html',
  styleUrl: './practice-result-summary.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeResultSummaryComponent {
  readonly result = input.required<PracticeResult>();
}
