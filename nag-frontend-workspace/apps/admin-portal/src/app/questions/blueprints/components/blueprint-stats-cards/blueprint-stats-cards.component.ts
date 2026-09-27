import {
  Component,
  ChangeDetectionStrategy,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-blueprint-stats-cards',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './blueprint-stats-cards.component.html',
  styleUrl: './blueprint-stats-cards.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BlueprintStatsCardsComponent {
  activeBlueprintsCount = input<number>(0);
  totalQuestionsCount = input<number>(0);
  taxonomySubjectsCount = input<number>(0);
}
