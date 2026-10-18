import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { SubtopicMastery } from '../../models/learner.model';

@Component({
  selector: 'app-spaced-reviews-panel',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule],
  templateUrl: './spaced-reviews-panel.component.html',
  styleUrl: './spaced-reviews-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SpacedReviewsPanelComponent {
  readonly reviews = input<SubtopicMastery[]>([]);
  readonly reviewNow = output<SubtopicMastery>();

  onReview(item: SubtopicMastery): void {
    this.reviewNow.emit(item);
  }
}
