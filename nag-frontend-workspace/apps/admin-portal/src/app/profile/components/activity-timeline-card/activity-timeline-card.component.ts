import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdminActivityLog, PageResponse } from '../../profile.model';

@Component({
  selector: 'nag-activity-timeline-card',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule, MatTooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './activity-timeline-card.component.html',
  styleUrl: './activity-timeline-card.component.scss',
})
export class ActivityTimelineCardComponent {
  readonly activityPage = input<PageResponse<AdminActivityLog> | null>(null);
  readonly loading = input<boolean>(false);
  readonly selectedCategory = input<string>('ALL');

  readonly changeFilter = output<string>();
  readonly changePage = output<number>();
  readonly exportLogs = output<'csv' | 'json'>();

  readonly categories = [
    { code: 'ALL', label: 'All Activities' },
    { code: 'AUTHENTICATION', label: 'Authentication & MFA' },
    { code: 'PROFILE', label: 'Profile & Credentials' },
    { code: 'AUTHORIZATION', label: 'Roles & Tokens' },
    { code: 'EXAMINATION', label: 'Exams & Questions' },
  ];

  onSelectCategory(cat: string): void {
    this.changeFilter.emit(cat);
  }

  onPrev(): void {
    const page = this.activityPage();
    if (page && page.number > 0) {
      this.changePage.emit(page.number - 1);
    }
  }

  onNext(): void {
    const page = this.activityPage();
    if (page && page.number < page.totalPages - 1) {
      this.changePage.emit(page.number + 1);
    }
  }
}
