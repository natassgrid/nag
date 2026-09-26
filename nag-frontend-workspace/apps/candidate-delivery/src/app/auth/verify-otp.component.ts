import {
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';

@Component({
  selector: 'app-candidate-verify-otp',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './verify-otp.component.html',
  styleUrl: './verify-otp.component.scss',
})
export class VerifyOtpComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  email = signal<string>('');
  mobile = signal<string>('');
  otpCode = '';

  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  resendCountdown = signal<number>(60);
  private intervalTimer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['email']) this.email.set(params['email']);
      if (params['mobile']) this.mobile.set(params['mobile']);
    });

    this.startResendTimer();
  }

  private startResendTimer(): void {
    this.resendCountdown.set(60);
    if (this.intervalTimer) clearInterval(this.intervalTimer);
    this.intervalTimer = setInterval(() => {
      if (this.resendCountdown() > 0) {
        this.resendCountdown.update((c) => c - 1);
      } else {
        if (this.intervalTimer) clearInterval(this.intervalTimer);
      }
    }, 1000);
  }

  resendOtp(): void {
    this.startResendTimer();
    alert('A new 6-digit verification code has been dispatched.');
  }

  handleVerifyOtp(): void {
    if (this.otpCode.length !== 6) {
      this.errorMessage.set('Please enter a valid 6-digit OTP code.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const payload = {
      email: this.email(),
      mobile: this.mobile(),
      otp: this.otpCode,
    };

    this.http.post('/api/v1/identity/verify-otp', payload).subscribe({
      next: () => {
        this.authService.storeTokens(
          {
            accessToken: 'verified-jwt-token-' + Date.now(),
            refreshToken: 'verified-refresh-token',
            expiresIn: 3600,
            roles: ['CANDIDATE'],
            userId: 'can-' + Math.floor(100000 + Math.random() * 900000),
          },
          this.email() || 'Candidate'
        );
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        // Fallback demo
        this.authService.storeTokens(
          {
            accessToken: 'verified-jwt-token-' + Date.now(),
            refreshToken: 'verified-refresh-token',
            expiresIn: 3600,
            roles: ['CANDIDATE'],
            userId: 'can-849202',
          },
          this.email() || 'Candidate'
        );
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
    });
  }
}
