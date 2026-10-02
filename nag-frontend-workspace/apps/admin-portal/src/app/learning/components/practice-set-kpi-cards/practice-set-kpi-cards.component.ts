import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-practice-set-kpi-cards',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './practice-set-kpi-cards.component.html',
  styleUrls: ['./practice-set-kpi-cards.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeSetKpiCardsComponent {
  total = input.required<number>();
  published = input.required<number>();
  totalQuestions = input<number>(0);
  draft = computed(() => Math.max(0, this.total() - this.published()));
}
