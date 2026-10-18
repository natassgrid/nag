import { Component, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AdaptiveLearningService } from './services/adaptive-learning.service';

@Component({
  selector: 'app-learn',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatButtonModule],
  templateUrl: './learn.component.html',
  styleUrl: './learn.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LearnComponent {
  private readonly router = inject(Router);
  readonly learningService = inject(AdaptiveLearningService);

  readonly activeEnrollment = this.learningService.activeEnrollment;
  readonly readinessScore = this.learningService.readinessScore;

  readonly isOnboarding = computed<boolean>(() => {
    return this.router.url.includes('/learn/onboarding');
  });

  onSwitchTargetExam(): void {
    this.router.navigate(['/learn/onboarding'], {
      queryParams: { step: 'exam-picker' },
    });
  }
}
