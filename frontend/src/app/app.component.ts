/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Component, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, Router, NavigationEnd } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule, MatSidenav } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { filter } from 'rxjs';
import { AuthService } from './core/services/auth.service';
import { NotificationPanelComponent } from './shared/components/notification-panel/notification-panel.component';
import { UserMenuComponent } from './shared/components/user-menu/user-menu.component';

export interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles: string[];
}

export interface NavGroup {
  label: string;
  icon: string;
  items: NavItem[];
  roles: string[];
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    NotificationPanelComponent,
    UserMenuComponent,
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  title = 'Exam Platform';
  isMobile = false;
  isAuthRoute = false;
  currentUrl = '';

  @ViewChild('sidenav') sidenav!: MatSidenav;

  /** Tracks which nav groups are expanded (all expanded by default) */
  expandedGroups = new Set<string>();

  /** Top-level nav item (no group) */
  dashboardItem: NavItem = { label: 'Dashboard', icon: 'home', route: '/dashboard', roles: [] };

  /** Grouped navigation sections */
  navGroups: NavGroup[] = [
    {
      label: 'Question Management',
      icon: 'quiz',
      roles: ['QUESTION_AUTHOR', 'REVIEWER', 'APPROVER', 'CONTENT_MANAGER', 'SUPER_ADMIN', 'EXAM_CONTROLLER'],
      items: [
        { label: 'Question Bank', icon: 'quiz', route: '/questions', roles: ['QUESTION_AUTHOR', 'REVIEWER'] },
        { label: 'Subjects', icon: 'category', route: '/questions/subjects', roles: ['QUESTION_AUTHOR', 'EXAM_CONTROLLER', 'SUPER_ADMIN'] },
        { label: 'Review Queue', icon: 'rate_review', route: '/questions/review', roles: ['REVIEWER', 'APPROVER'] },
        { label: 'Asset Library', icon: 'perm_media', route: '/assets', roles: ['QUESTION_AUTHOR', 'CONTENT_MANAGER', 'SUPER_ADMIN'] },
      ]
    },
    {
      label: 'Examination',
      icon: 'assignment',
      roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'],
      items: [
        { label: 'Exams', icon: 'assignment', route: '/exam/manage', roles: ['EXAM_CONTROLLER'] },
        { label: 'Papers', icon: 'description', route: '/papers', roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] },
        { label: 'Blueprint Rules', icon: 'rule', route: '/papers/blueprints', roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] },
        { label: 'Schedules', icon: 'event', route: '/exam/scheduling', roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] },
        { label: 'Exam Centres', icon: 'location_on', route: '/exam/scheduling/centres', roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] },
      ]
    },
    {
      label: 'Exam Delivery',
      icon: 'school',
      roles: ['CANDIDATE'],
      items: [
        { label: 'My Exams', icon: 'school', route: '/exam', roles: ['CANDIDATE'] },
        { label: 'Live Exams', icon: 'cast_connected', route: '/exam/delivery', roles: ['CANDIDATE'] },
      ]
    },
    {
      label: 'Results & Reports',
      icon: 'assessment',
      roles: ['CANDIDATE', 'EXAM_CONTROLLER', 'EVALUATOR', 'SUPER_ADMIN'],
      items: [
        { label: 'Results', icon: 'grade', route: '/results', roles: ['CANDIDATE'] },
        { label: 'Evaluations', icon: 'rate_review', route: '/evaluations', roles: ['EVALUATOR'] },
        { label: 'Reports', icon: 'bar_chart', route: '/admin/reports', roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] },
        { label: 'Analytics', icon: 'insights', route: '/analytics', roles: ['EXAM_CONTROLLER'] },
      ]
    },
    {
      label: 'Administration',
      icon: 'admin_panel_settings',
      roles: ['SUPER_ADMIN', 'SECURITY_ADMIN', 'AUDITOR'],
      items: [
        { label: 'Users', icon: 'people', route: '/admin/users', roles: ['SUPER_ADMIN', 'SECURITY_ADMIN'] },
        { label: 'Roles & Permissions', icon: 'shield', route: '/admin/roles', roles: ['SUPER_ADMIN', 'SECURITY_ADMIN'] },
        { label: 'Audit Log', icon: 'security', route: '/admin/audit', roles: ['AUDITOR', 'SUPER_ADMIN'] },
        { label: 'Settings', icon: 'settings', route: '/admin/settings', roles: ['SUPER_ADMIN'] },
      ]
    },
  ];

  constructor(
    public authService: AuthService,
    private breakpointObserver: BreakpointObserver,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    // All groups expanded by default
    this.navGroups.forEach(g => this.expandedGroups.add(g.label));

    this.breakpointObserver
      .observe([Breakpoints.Handset])
      .subscribe(result => {
        this.isMobile = result.matches;
      });

    this.currentUrl = this.router.url;
    this.isAuthRoute = this.router.url.startsWith('/auth');

    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.currentUrl = event.urlAfterRedirects;
      this.isAuthRoute = this.currentUrl.startsWith('/auth');
      this.cdr.markForCheck();
    });
  }

  toggleGroup(label: string): void {
    if (this.expandedGroups.has(label)) {
      this.expandedGroups.delete(label);
    } else {
      this.expandedGroups.add(label);
    }
  }

  isGroupExpanded(label: string): boolean {
    return this.expandedGroups.has(label);
  }

  isRouteActive(itemRoute: string): boolean {
    const url = (this.currentUrl || this.router.url || '').split('?')[0].split('#')[0];

    // 1. Exact match always wins
    if (url === itemRoute) {
      return true;
    }

    // 2. Dashboard is strictly exact
    if (itemRoute === '/dashboard') {
      return url === '/dashboard';
    }

    // 3. Question Bank (/questions) - active for /questions and /questions/:id, but NOT /questions/subjects or /questions/review
    if (itemRoute === '/questions') {
      return url.startsWith('/questions') &&
        !url.startsWith('/questions/subjects') &&
        !url.startsWith('/questions/review');
    }

    // 4. Papers (/papers) - active for /papers and /papers/:id, but NOT /papers/blueprints
    if (itemRoute === '/papers') {
      return url.startsWith('/papers') &&
        !url.startsWith('/papers/blueprints');
    }

    // 5. Schedules (/exam/scheduling) - active for /exam/scheduling and /exam/scheduling/:examId, but NOT /exam/scheduling/centres
    if (itemRoute === '/exam/scheduling') {
      return url.startsWith('/exam/scheduling') &&
        !url.startsWith('/exam/scheduling/centres');
    }

    // 6. My Exams (/exam) - candidate view, should not match manage, scheduling, or delivery
    if (itemRoute === '/exam') {
      return url === '/exam' || (
        url.startsWith('/exam/') &&
        !url.startsWith('/exam/manage') &&
        !url.startsWith('/exam/scheduling') &&
        !url.startsWith('/exam/delivery')
      );
    }

    // 7. General sub-path match for other routes (e.g. /exam/manage/:id, /admin/users/:id, /exam/scheduling/centres)
    return url.startsWith(itemRoute + '/');
  }

  get visibleNavGroups(): NavGroup[] {
    const userRoles = this.authService.getUserRoles();
    return this.navGroups
      .filter(group => group.roles.some(role => userRoles.includes(role)) || group.roles.length === 0)
      .map(group => ({
        ...group,
        items: group.items.filter(item =>
          item.roles.length === 0 || item.roles.some(role => userRoles.includes(role))
        )
      }))
      .filter(group => group.items.length > 0);
  }

  get userName(): string {
    return this.authService.getUserName() || 'User';
  }

  get primaryRole(): string {
    const roles = this.authService.getUserRoles();
    if (roles.length === 0) return '';
    // Show a friendly display for the first meaningful role
    const roleDisplay: Record<string, string> = {
      'SUPER_ADMIN': 'Super Admin',
      'SECURITY_ADMIN': 'Security Admin',
      'EXAM_CONTROLLER': 'Exam Controller',
      'QUESTION_AUTHOR': 'Question Author',
      'REVIEWER': 'Reviewer',
      'APPROVER': 'Approver',
      'EVALUATOR': 'Evaluator',
      'CANDIDATE': 'Candidate',
      'AUDITOR': 'Auditor',
      'CONTENT_MANAGER': 'Content Manager',
    };
    for (const role of roles) {
      if (roleDisplay[role]) return roleDisplay[role];
    }
    return roles[0]?.replace(/_/g, ' ').toLowerCase() || '';
  }

  onLogout(): void {
    this.authService.logout();
    // Force full page navigation to clear all state
    window.location.href = '/auth/login';
  }

  closeSidenavIfMobile(): void {
    if (this.isMobile) {
      this.sidenav.close();
    }
  }
}
