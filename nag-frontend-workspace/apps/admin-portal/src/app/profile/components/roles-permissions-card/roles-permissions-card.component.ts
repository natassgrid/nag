import {
  Component,
  ChangeDetectionStrategy,
  input,
  signal,
  computed,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdminUserProfile, PermissionItem, RoleDetail } from '../../profile.model';
import { ProfileService } from '../../profile.service';

@Component({
  selector: 'nag-roles-permissions-card',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule, MatTooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './roles-permissions-card.component.html',
  styleUrl: './roles-permissions-card.component.scss',
})
export class RolesPermissionsCardComponent {
  private readonly profileService = inject(ProfileService);

  readonly profile = input.required<AdminUserProfile>();
  readonly permissions = input.required<PermissionItem[]>();

  // Filter signals
  readonly selectedCategory = signal<string>('ALL');
  readonly searchQuery = signal<string>('');

  readonly categories = [
    { key: 'ALL', label: 'All Modules', icon: 'apps' },
    { key: 'QUESTIONS', label: 'Questions & Taxonomy', icon: 'quiz' },
    { key: 'EXAMINATIONS', label: 'Examinations & Papers', icon: 'assignment' },
    { key: 'DELIVERY', label: 'Live Delivery', icon: 'sensors' },
    { key: 'EVALUATION', label: 'Grading & Normalization', icon: 'grading' },
    { key: 'IDENTITY', label: 'Identity & Access', icon: 'admin_panel_settings' },
    { key: 'AUDIT', label: 'Audit Trail', icon: 'history' },
    { key: 'SECURITY', label: 'Security & Keyrings', icon: 'security' },
  ];

  readonly roleDetails = computed<RoleDetail[]>(() => {
    const roles = this.profile().roles || [];
    return roles.map((r) => this.profileService.getSystemRoleDetails(r));
  });

  readonly filteredPermissions = computed(() => {
    const all = this.permissions() || [];
    const cat = this.selectedCategory();
    const query = this.searchQuery().toLowerCase().trim();

    return all.filter((p) => {
      const matchCat = cat === 'ALL' || p.category === cat;
      const matchQuery =
        !query ||
        p.name.toLowerCase().includes(query) ||
        p.code.toLowerCase().includes(query) ||
        p.description.toLowerCase().includes(query);
      return matchCat && matchQuery;
    });
  });

  readonly grantedCount = computed(() => {
    return (this.permissions() || []).filter((p) => p.granted).length;
  });

  getCategoryColor(category: string): string {
    switch (category) {
      case 'QUESTIONS':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'EXAMINATIONS':
        return 'bg-indigo-50 text-indigo-700 border-indigo-200';
      case 'DELIVERY':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'EVALUATION':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'IDENTITY':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'AUDIT':
        return 'bg-slate-100 text-slate-700 border-slate-200';
      case 'SECURITY':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  }
}
