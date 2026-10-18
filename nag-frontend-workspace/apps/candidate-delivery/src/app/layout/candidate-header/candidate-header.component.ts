import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { NavItem, CANDIDATE_NAV_ITEMS } from '../layout.models';
import { AdaptiveLearningService } from '../../learn/services/adaptive-learning.service';

@Component({
  selector: 'nag-candidate-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule,
  ],
  templateUrl: './candidate-header.component.html',
  styleUrl: './candidate-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateHeaderComponent {
  private readonly router = inject(Router);
  private readonly learningService = inject(AdaptiveLearningService);

  readonly isDrawerOpen = input<boolean>(false);
  readonly userName = input<string>('Guest');
  readonly isAuthenticated = input<boolean>(false);
  readonly navItems = input<NavItem[]>(CANDIDATE_NAV_ITEMS);

  readonly activeEnrollment = this.learningService.activeEnrollment;

  readonly toggleDrawer = output<void>();
  readonly logout = output<void>();

  isGroupActive(item: NavItem): boolean {
    if (!item.children || item.children.length === 0) return false;
    const currentUrl = this.router.url;
    return item.children.some(
      (child) =>
        child.route &&
        (currentUrl === child.route ||
          currentUrl.startsWith(child.route + '/') ||
          currentUrl.startsWith(child.route + '?'))
    );
  }

  getUserInitials(): string {
    const name = this.userName() || 'G';
    const parts = name.trim().split(/[\s_.-]+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  }
}
