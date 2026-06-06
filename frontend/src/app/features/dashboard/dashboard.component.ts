import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/services/auth.service';

interface DashboardCard {
  icon: string;
  title: string;
  description: string;
  route: string;
  roles: string[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatButtonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  username = '';
  cards: DashboardCard[] = [];

  private readonly allCards: DashboardCard[] = [
    {
      icon: 'people',
      title: 'Users',
      description: 'Manage user accounts and roles',
      route: '/admin/users',
      roles: ['SUPER_ADMIN', 'SECURITY_ADMIN']
    },
    {
      icon: 'security',
      title: 'Audit Log',
      description: 'View system audit trail',
      route: '/admin/audit',
      roles: ['SUPER_ADMIN', 'SECURITY_ADMIN']
    },
    {
      icon: 'quiz',
      title: 'Question Bank',
      description: 'Create and review questions',
      route: '/questions',
      roles: ['QUESTION_AUTHOR', 'REVIEWER']
    },
    {
      icon: 'assignment',
      title: 'Examinations',
      description: 'Create and manage exams',
      route: '/exam/manage',
      roles: ['EXAM_CONTROLLER']
    },
    {
      icon: 'analytics',
      title: 'Analytics',
      description: 'View exam analytics',
      route: '/analytics',
      roles: ['EXAM_CONTROLLER']
    },
    {
      icon: 'school',
      title: 'My Exams',
      description: 'Start or continue exams',
      route: '/exam/delivery',
      roles: ['CANDIDATE']
    },
    {
      icon: 'grade',
      title: 'Results',
      description: 'View your results',
      route: '/results',
      roles: ['CANDIDATE']
    },
    {
      icon: 'rate_review',
      title: 'Evaluations',
      description: 'Evaluate exam responses',
      route: '/evaluations',
      roles: ['EVALUATOR']
    },
    {
      icon: 'notifications',
      title: 'Notifications',
      description: 'View notifications',
      route: '/notifications',
      roles: [] // Available to all roles
    }
  ];

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.username = this.authService.getUserId() || 'User';
    this.cards = this.getVisibleCards();
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  private getVisibleCards(): DashboardCard[] {
    const userRoles = this.authService.getUserRoles();
    return this.allCards.filter(card =>
      card.roles.length === 0 || card.roles.some(role => userRoles.includes(role))
    );
  }
}
