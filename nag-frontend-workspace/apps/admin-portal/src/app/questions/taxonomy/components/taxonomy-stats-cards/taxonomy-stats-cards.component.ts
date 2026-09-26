import {
  Component,
  ChangeDetectionStrategy,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-taxonomy-stats-cards',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './taxonomy-stats-cards.component.html',
  styleUrl: './taxonomy-stats-cards.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaxonomyStatsCardsComponent {
  totalSubjects = input<number>(0);
  totalTopics = input<number>(0);
  totalSubtopics = input<number>(0);
}
