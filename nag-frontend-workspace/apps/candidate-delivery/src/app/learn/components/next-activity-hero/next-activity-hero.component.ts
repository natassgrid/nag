import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { NextActivity } from '../../models/learner.model';

@Component({
  selector: 'app-next-activity-hero',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule],
  templateUrl: './next-activity-hero.component.html',
  styleUrl: './next-activity-hero.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NextActivityHeroComponent {
  readonly activity = input<NextActivity | null>(null);
  readonly completing = input<boolean>(false);

  readonly start = output<NextActivity>();
  readonly complete = output<NextActivity>();

  readonly activityIcon = computed<string>(() => {
    const type = this.activity()?.type;
    switch (type) {
      case 'LESSON':
        return 'menu_book';
      case 'PRACTICE':
        return 'quiz';
      case 'REVISION':
        return 'replay';
      case 'MOCK_SECTION':
        return 'assignment';
      default:
        return 'auto_awesome';
    }
  });

  readonly actionText = computed<string>(() => {
    const type = this.activity()?.type;
    switch (type) {
      case 'LESSON':
        return 'Start Micro-Lesson';
      case 'PRACTICE':
        return 'Launch Practice Drill';
      case 'REVISION':
        return 'Begin Spaced Revision';
      case 'MOCK_SECTION':
        return 'Start Section Mock';
      default:
        return 'Start Activity';
    }
  });

  readonly badgeClass = computed<string>(() => {
    const type = this.activity()?.type;
    switch (type) {
      case 'LESSON':
        return 'bg-indigo-50 text-indigo-700 border-indigo-200';
      case 'PRACTICE':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'REVISION':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'MOCK_SECTION':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  });

  onStart(): void {
    const act = this.activity();
    if (act) this.start.emit(act);
  }

  onComplete(): void {
    const act = this.activity();
    if (act) this.complete.emit(act);
  }
}
