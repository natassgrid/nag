import {
  Component,
  ChangeDetectionStrategy,
  input,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-readiness-score-meter',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
  templateUrl: './readiness-score-meter.component.html',
  styleUrl: './readiness-score-meter.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReadinessScoreMeterComponent {
  readonly score = input<number>(0);
  readonly targetBenchmark = input<number>(85);
  readonly masteredCount = input<number>(0);
  readonly learningCount = input<number>(0);
  readonly totalSubtopics = input<number>(0);
  readonly examTitle = input<string>('');

  readonly radius = 42;
  readonly circumference = 2 * Math.PI * this.radius;

  readonly strokeDashoffset = computed<number>(() => {
    const s = Math.min(100, Math.max(0, this.score()));
    return this.circumference - (s / 100) * this.circumference;
  });

  readonly statusLabel = computed<string>(() => {
    const s = this.score();
    if (s >= 85) return 'Exam Ready';
    if (s >= 70) return 'On Track';
    if (s >= 50) return 'Progressing';
    return 'Foundation Building';
  });

  readonly statusBadgeClass = computed<string>(() => {
    const s = this.score();
    if (s >= 85) return 'bg-emerald-50 text-emerald-700 border-emerald-200';
    if (s >= 70) return 'bg-indigo-50 text-indigo-700 border-indigo-200';
    if (s >= 50) return 'bg-amber-50 text-amber-700 border-amber-200';
    return 'bg-slate-100 text-slate-700 border-slate-200';
  });
}
