import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type StatusVariant =
  | 'success'
  | 'warn'
  | 'error'
  | 'info'
  | 'neutral'
  | 'primary';

@Component({
  selector: 'nag-status-badge',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './status-badge.component.html',
  styleUrl: './status-badge.component.scss',
})
export class StatusBadgeComponent {
  label = input<string>();
  variant = input<StatusVariant>('neutral');
  showDot = input<boolean>(true);

  badgeClass(): string {
    switch (this.variant()) {
      case 'success':
        return 'bg-emerald-50 text-emerald-700 border border-emerald-200';
      case 'warn':
        return 'bg-amber-50 text-amber-700 border border-amber-200';
      case 'error':
        return 'bg-rose-50 text-rose-700 border border-rose-200';
      case 'info':
        return 'bg-sky-50 text-sky-700 border border-sky-200';
      case 'primary':
        return 'bg-indigo-50 text-indigo-700 border border-indigo-200';
      case 'neutral':
      default:
        return 'bg-slate-100 text-slate-700 border border-slate-200';
    }
  }
}
