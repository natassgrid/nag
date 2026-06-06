import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

function generateRequestId(): string {
  return crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).substring(2, 11)}`;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  let headers = req.headers
    .set('X-Request-Id', generateRequestId())
    .set('X-Tenant-Id', 'exam-authority-1')
    .set('Accept-Language', navigator.language || 'en');

  if (token && token !== 'undefined' && token !== 'null') {
    headers = headers.set('Authorization', `Bearer ${token}`);
  }

  const clonedReq = req.clone({ headers });
  return next(clonedReq);
};
