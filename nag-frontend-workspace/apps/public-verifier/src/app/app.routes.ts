import { Route } from '@angular/router';

export const appRoutes: Route[] = [
  {
    path: '',
    loadComponent: () =>
      import('./verification/verification.component').then(
        (m) => m.VerificationComponent
      ),
  },
];
