import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule, MatSidenav } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { AuthService } from './core/services/auth.service';

export interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles: string[];
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  title = 'Exam Platform';
  isMobile = false;

  @ViewChild('sidenav') sidenav!: MatSidenav;

  navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'home', route: '/dashboard', roles: [] },
    { label: 'Profile', icon: 'person', route: '/profile', roles: [] },
    { label: 'Notifications', icon: 'notifications', route: '/notifications', roles: [] },
    { label: 'User Management', icon: 'people', route: '/admin/users', roles: ['SUPER_ADMIN', 'SECURITY_ADMIN'] },
    { label: 'Question Bank', icon: 'quiz', route: '/questions', roles: ['QUESTION_AUTHOR', 'REVIEWER'] },
    { label: 'Subject Management', icon: 'category', route: '/questions/subjects', roles: ['QUESTION_AUTHOR', 'EXAM_CONTROLLER', 'SUPER_ADMIN'] },
    { label: 'Asset Library', icon: 'perm_media', route: '/assets', roles: ['QUESTION_AUTHOR', 'CONTENT_MANAGER', 'SUPER_ADMIN'] },
    { label: 'Review Queue', icon: 'rate_review', route: '/questions/review', roles: ['REVIEWER', 'APPROVER'] },
    { label: 'Exam Management', icon: 'assignment', route: '/exam/manage', roles: ['EXAM_CONTROLLER'] },
    { label: 'Paper Generation', icon: 'description', route: '/papers', roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] },
    { label: 'Scheduling', icon: 'event', route: '/exam/scheduling', roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] },
    { label: 'Exam Centres', icon: 'location_on', route: '/exam/scheduling/centres', roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] },
    { label: 'My Exams', icon: 'school', route: '/exam', roles: ['CANDIDATE'] },
    { label: 'Results', icon: 'grade', route: '/results', roles: ['CANDIDATE'] },
    { label: 'Evaluations', icon: 'rate_review', route: '/evaluations', roles: ['EVALUATOR'] },
    { label: 'Audit Log', icon: 'security', route: '/admin/audit', roles: ['AUDITOR'] },
  ];

  constructor(
    public authService: AuthService,
    private breakpointObserver: BreakpointObserver
  ) {
    this.breakpointObserver
      .observe([Breakpoints.Handset])
      .subscribe(result => {
        this.isMobile = result.matches;
      });
  }

  get visibleNavItems(): NavItem[] {
    const userRoles = this.authService.getUserRoles();
    return this.navItems.filter(item => {
      if (item.roles.length === 0) return true;
      return item.roles.some(role => userRoles.includes(role));
    });
  }

  get userName(): string {
    return this.authService.getUserId() || 'User';
  }

  onLogout(): void {
    this.authService.logout();
  }

  closeSidenavIfMobile(): void {
    if (this.isMobile) {
      this.sidenav.close();
    }
  }
}
