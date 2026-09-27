import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { ReviewStats } from '../../models';

@Component({
  selector: 'app-review-summary-stats',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './review-summary-stats.component.html',
  styleUrl: './review-summary-stats.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewSummaryStatsComponent {
  readonly stats = input.required<ReviewStats>();
  readonly totalQuestions = input.required<number>();
  readonly avgTime = input<string>('1m 18s');
}
