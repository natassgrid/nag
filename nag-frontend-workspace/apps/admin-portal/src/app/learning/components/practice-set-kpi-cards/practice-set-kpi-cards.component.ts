import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-practice-set-kpi-cards',
  standalone: true,
  imports: [MatCardModule, MatIconModule],
  templateUrl: './practice-set-kpi-cards.component.html',
  styleUrls: ['./practice-set-kpi-cards.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeSetKpiCardsComponent {
  total = input.required<number>();
  published = input.required<number>();
  draft = computed(() => this.total() - this.published());
}
