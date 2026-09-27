import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { CandidateRegistrationPayload } from '../models/auth-flow.model';

@Injectable({
  providedIn: 'root',
})
export class AuthFlowService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /**
   * Register a new candidate account.
   */
  registerCandidate(payload: CandidateRegistrationPayload): Observable<unknown> {
    return this.http.post('/api/v1/identity/register', payload);
  }

  /**
   * Navigate candidate to OTP verification screen with query parameters.
   */
  navigateToVerifyOtp(params: {
    userId?: string;
    email?: string;
    mobile?: string;
    pending?: boolean;
  }): void {
    this.router.navigate(['/verify-otp'], {
      queryParams: {
        userId: params.userId || undefined,
        email: params.email || undefined,
        mobile: params.mobile || undefined,
        pending: params.pending ? 'true' : undefined,
      },
    });
  }

  /**
   * Navigate candidate to dashboard.
   */
  navigateToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  /**
   * Checks whether an HTTP error from login indicates an unverified account.
   */
  isUnverifiedAccountError(err: any): boolean {
    const errData = err?.error;
    return Boolean(
      errData?.pendingVerification ||
      errData?.title === 'Account Not Verified' ||
      (errData?.detail && errData.detail.toLowerCase().includes('not yet verified'))
    );
  }
}
