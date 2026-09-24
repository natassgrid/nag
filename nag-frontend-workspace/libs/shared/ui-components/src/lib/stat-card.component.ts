import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-stat-card',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './stat-card.component.html',
  styleUrl: './stat-card.component.scss',
})
export class StatCardComponent {
  label = input.required<string>();
  value = input.required<string | number>();
  unit = input<string>('');
  icon = input<string>('analytics');
  variant = input<'primary' | 'accent' | 'success' | 'warn' | 'info'>('primary');
  trendText = input<string>('');
  trendDirection = input<'up' | 'down' | 'neutral'>('neutral');

  iconBgClass(): string {
    switch (this.variant()) {
      case 'success':
        return 'bg-emerald-50 text-emerald-600';
      case 'accent':
        return 'bg-amber-50 text-amber-600';
      case 'warn':
        return 'bg-rose-50 text-rose-600';
      case 'info':
        return 'bg-sky-50 text-sky-600';
      case 'primary':
      default:
        return 'bg-indigo-50 text-indigo-600';
    }
  }
}
