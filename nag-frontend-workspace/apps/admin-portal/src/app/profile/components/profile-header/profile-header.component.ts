import { Component, ChangeDetectionStrategy, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { StatusBadgeComponent } from '@nag-frontend-workspace/shared-ui-components';
import { AdminUserProfile } from '../../profile.model';

@Component({
  selector: 'nag-profile-header',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatTooltipModule, StatusBadgeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './profile-header.component.html',
  styleUrl: './profile-header.component.scss',
})
export class ProfileHeaderComponent {
  readonly profile = input.required<AdminUserProfile>();
  readonly refresh = output<void>();

  readonly initials = computed(() => {
    const name = this.profile().fullName || this.profile().username || 'AD';
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  });

  getRoleBadgeClass(role: string): string {
    const r = role.toUpperCase();
    if (r.includes('SUPER')) return 'bg-purple-100 text-purple-800 border-purple-200';
    if (r.includes('EXAM')) return 'bg-indigo-100 text-indigo-800 border-indigo-200';
    if (r.includes('QUESTION') || r.includes('AUTHOR')) return 'bg-blue-100 text-blue-800 border-blue-200';
    if (r.includes('SECURITY')) return 'bg-amber-100 text-amber-800 border-amber-200';
    if (r.includes('REVIEW')) return 'bg-emerald-100 text-emerald-800 border-emerald-200';
    return 'bg-slate-100 text-slate-800 border-slate-200';
  }
}
