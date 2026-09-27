import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReviewStatusFilter, ReviewStats } from '../../models';

@Component({
  selector: 'app-review-filter-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './review-filter-bar.component.html',
  styleUrl: './review-filter-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewFilterBarComponent {
  readonly statusFilter = input.required<ReviewStatusFilter>();
  readonly totalQuestions = input.required<number>();
  readonly stats = input.required<ReviewStats>();

  readonly filterChange = output<ReviewStatusFilter>();
}
