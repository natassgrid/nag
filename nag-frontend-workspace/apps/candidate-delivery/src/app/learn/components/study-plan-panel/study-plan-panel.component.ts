import {
  Component,
  ChangeDetectionStrategy,
  input,
  signal,
  inject,
  effect,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { StudyPlanItem } from '../../models';
import { RecommendationService } from '../../services';

@Component({
  selector: 'app-study-plan-panel',
  standalone: true,
  imports: [CommonModule, MatListModule, MatIconModule, MatButtonModule],
  templateUrl: './study-plan-panel.component.html',
  styleUrl: './study-plan-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudyPlanPanelComponent {
  private readonly recommendationService = inject(RecommendationService);

  plan = input.required<StudyPlanItem[]>();
  readonly completedDays = signal<Set<number>>(new Set());

  constructor() {
    effect(() => {
      const items = this.plan() || [];
      const done = new Set<number>(
        items.filter((d) => d.completed).map((d) => d.day)
      );
      this.completedDays.set(done);
    });
  }

  isDayCompleted(day: number): boolean {
    return this.completedDays().has(day);
  }

  toggleDay(day: number): void {
    const current = new Set(this.completedDays());
    const nextCompleted = !current.has(day);
    if (nextCompleted) {
      current.add(day);
    } else {
      current.delete(day);
    }
    this.completedDays.set(current);

    this.recommendationService.toggleStudyPlanDay(day, nextCompleted).subscribe({
      error: () => {
        // Rollback on network failure
        const rollback = new Set(this.completedDays());
        if (nextCompleted) {
          rollback.delete(day);
        } else {
          rollback.add(day);
        }
        this.completedDays.set(rollback);
      },
    });
  }
}
