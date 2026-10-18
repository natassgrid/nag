import { inject } from '@angular/core';
import { Router, CanActivateFn, UrlTree } from '@angular/router';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { AdaptiveLearningService } from '../services/adaptive-learning.service';

export const learnerJourneyGuard: CanActivateFn = (): boolean | UrlTree => {
  const authService = inject(AuthService);
  const learningService = inject(AdaptiveLearningService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  if (!learningService.hasCompletedProfile()) {
    return router.createUrlTree(['/learn/onboarding'], {
      queryParams: { step: 'profile' },
    });
  }

  if (!learningService.hasActiveEnrollment()) {
    return router.createUrlTree(['/learn/onboarding'], {
      queryParams: { step: 'exam-picker' },
    });
  }

  return true;
};
