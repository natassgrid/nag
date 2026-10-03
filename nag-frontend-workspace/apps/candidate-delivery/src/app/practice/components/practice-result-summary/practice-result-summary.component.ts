import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { PracticeResult } from '../../models';

export interface TopicStat {
  topic: string;
  correct: number;
  total: number;
  percentage: number;
}

@Component({
  selector: 'app-practice-result-summary',
  standalone: true,
  imports: [CommonModule, MatProgressBarModule, MatIconModule],
  templateUrl: './practice-result-summary.component.html',
  styleUrl: './practice-result-summary.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeResultSummaryComponent {
  readonly result = input.required<PracticeResult>();

  readonly topicStats = computed<TopicStat[]>(() => {
    const raw = this.result().topicWiseBreakdown;
    if (!raw) return [];

    let map: Record<string, any> = {};
    if (typeof raw === 'string') {
      try {
        map = JSON.parse(raw);
      } catch (e) {
        return [];
      }
    } else {
      map = raw;
    }

    return Object.entries(map).map(([topic, data]) => {
      const correct = data.correct || 0;
      const total = data.total || 1;
      const percentage = Math.round((correct / total) * 100);
      return { topic, correct, total, percentage };
    });
  });
}
