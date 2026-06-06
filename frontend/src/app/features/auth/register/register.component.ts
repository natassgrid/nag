import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatStepperModule,
    RouterLink
  ],
  template: `
    <main class="register-container" role="main" aria-labelledby="register-heading">
      <mat-card class="register-card" appearance="outlined">
        <!-- Registration Step -->
        <ng-container *ngIf="!showOtpStep">
          <mat-card-header class="register-header">
            <div class="app-branding">
              <mat-icon class="app-logo" aria-hidden="true">person_add</mat-icon>
              <h1 id="register-heading" class="app-title">Create Account</h1>
            </div>
            <p class="register-subtitle">Register as a candidate</p>
          </mat-card-header>

          <mat-card-content>
            <form [formGroup]="registerForm" (ngSubmit)="onRegister()" aria-label="Registration form">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Full Name</mat-label>
                <input matInput formControlName="name"
                       autocomplete="name"
                       aria-required="true">
                <mat-icon matPrefix>person</mat-icon>
                <mat-error *ngIf="registerForm.get('name')?.hasError('required')">
                  Full name is required
                </mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Email</mat-label>
                <input matInput formControlName="email"
                       type="email"
                       autocomplete="email"
                       aria-required="true">
                <mat-icon matPrefix>email</mat-icon>
                <mat-error *ngIf="registerForm.get('email')?.hasError('required')">
                  Email is required
                </mat-error>
                <mat-error *ngIf="registerForm.get('email')?.hasError('email')">
                  Enter a valid email address
                </mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Mobile Number</mat-label>
                <input matInput formControlName="mobile"
                       type="tel"
                       autocomplete="tel"
                       placeholder="+91XXXXXXXXXX"
                       aria-required="true">
                <mat-icon matPrefix>phone</mat-icon>
                <mat-error *ngIf="registerForm.get('mobile')?.hasError('required')">
                  Mobile number is required
                </mat-error>
                <mat-error *ngIf="registerForm.get('mobile')?.hasError('pattern')">
                  Enter a valid Indian mobile number (+91XXXXXXXXXX)
                </mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Identity Document Type</mat-label>
                <mat-select formControlName="identityDocType" aria-required="true">
                  <mat-option value="AADHAAR">Aadhaar</mat-option>
                  <mat-option value="PAN">PAN</mat-option>
                  <mat-option value="VOTER_ID">Voter ID</mat-option>
                  <mat-option value="PASSPORT">Passport</mat-option>
                </mat-select>
                <mat-icon matPrefix>badge</mat-icon>
                <mat-error *ngIf="registerForm.get('identityDocType')?.hasError('required')">
                  Document type is required
                </mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Identity Document Number</mat-label>
                <input matInput formControlName="identityDocNumber"
                       aria-required="true">
                <mat-icon matPrefix>article</mat-icon>
                <mat-error *ngIf="registerForm.get('identityDocNumber')?.hasError('required')">
                  Document number is required
                </mat-error>
              </mat-form-field>

              <button mat-raised-button color="primary" type="submit"
                      class="submit-button full-width"
                      [disabled]="registerForm.invalid || isLoading"
                      aria-label="Submit registration">
                <mat-spinner *ngIf="isLoading" diameter="20" class="button-spinner"></mat-spinner>
                <span *ngIf="!isLoading">Register</span>
              </button>
            </form>

            <div class="auth-links">
              <a [routerLink]="['/auth/login']" aria-label="Already have an account? Sign in">
                Already have an account? Sign In
              </a>
            </div>
          </mat-card-content>
        </ng-container>

        <!-- OTP Verification Step -->
        <ng-container *ngIf="showOtpStep">
          <mat-card-header class="register-header">
            <div class="app-branding">
              <mat-icon class="app-logo" aria-hidden="true">verified_user</mat-icon>
              <h1 id="register-heading" class="app-title">Verify OTP</h1>
            </div>
            <p class="register-subtitle">Enter the 6-digit OTP sent to your registered mobile number</p>
          </mat-card-header>

          <mat-card-content>
            <form [formGroup]="otpForm" (ngSubmit)="onVerifyOtp()" aria-label="OTP verification form">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>OTP Code</mat-label>
                <input matInput formControlName="otp"
                       type="text"
                       inputmode="numeric"
                       maxlength="6"
                       autocomplete="one-time-code"
                       aria-required="true"
                       aria-label="6-digit One Time Password">
                <mat-icon matPrefix>lock</mat-icon>
                <mat-error *ngIf="otpForm.get('otp')?.hasError('required')">
                  OTP is required
                </mat-error>
                <mat-error *ngIf="otpForm.get('otp')?.hasError('pattern')">
                  Must be exactly 6 digits
                </mat-error>
              </mat-form-field>

              <button mat-raised-button color="primary" type="submit"
                      class="submit-button full-width"
                      [disabled]="otpForm.invalid || isVerifying"
                      aria-label="Verify OTP code">
                <mat-spinner *ngIf="isVerifying" diameter="20" class="button-spinner"></mat-spinner>
                <span *ngIf="!isVerifying">Verify & Activate</span>
              </button>
            </form>

            <div class="auth-links">
              <a href="javascript:void(0)" (click)="backToRegister()"
                 aria-label="Go back to registration form">
                ← Back to registration
              </a>
            </div>
          </mat-card-content>
        </ng-container>
      </mat-card>
    </main>
  `,
  styles: [`
    .register-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: 16px;
      background: linear-gradient(135deg, #e8eaf6 0%, #fafafa 100%);
    }

    .register-card {
      max-width: 460px;
      width: 100%;
      padding: 32px 24px;
      border-radius: 12px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
    }

    .register-header {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 24px;
    }

    .app-branding {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
    }

    .app-logo {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #3f51b5;
    }

    .app-title {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: #3f51b5;
      letter-spacing: -0.5px;
    }

    .register-subtitle {
      margin: 8px 0 0;
      font-size: 14px;
      color: #666;
      text-align: center;
    }

    .full-width {
      width: 100%;
    }

    mat-form-field.full-width {
      margin-bottom: 8px;
    }

    .submit-button {
      height: 48px;
      font-size: 16px;
      font-weight: 500;
      margin-top: 8px;
      border-radius: 8px;
    }

    .button-spinner {
      display: inline-block;
    }

    ::ng-deep .submit-button .mat-mdc-button-persistent-ripple {
      border-radius: 8px;
    }

    .auth-links {
      margin-top: 24px;
      text-align: center;
    }

    .auth-links a {
      color: #3f51b5;
      text-decoration: none;
      font-size: 14px;
    }

    .auth-links a:hover {
      text-decoration: underline;
    }
  `]
})
export class RegisterComponent {
  registerForm: FormGroup;
  otpForm: FormGroup;
  isLoading = false;
  isVerifying = false;
  showOtpStep = false;
  private registrationId = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.registerForm = this.fb.group({
      name: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      mobile: ['', [Validators.required, Validators.pattern(/^\+91\d{10}$/)]],
      identityDocType: ['', [Validators.required]],
      identityDocNumber: ['', [Validators.required]]
    });

    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
    });
  }

  onRegister(): void {
    if (this.registerForm.invalid) return;

    this.isLoading = true;

    const formData = this.registerForm.value;
    this.authService.register(formData).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.registrationId = res.registrationId;
        this.showOtpStep = true;
        this.snackBar.open('Registration successful! Please verify OTP.', 'OK', {
          duration: 4000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        });
      },
      error: (err) => {
        this.isLoading = false;
        const message = err.error?.message || 'Registration failed. Please try again.';
        this.snackBar.open(message, 'Dismiss', {
          duration: 5000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom',
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  onVerifyOtp(): void {
    if (this.otpForm.invalid) return;

    this.isVerifying = true;

    this.authService.verifyOtp({
      registrationId: this.registrationId,
      otp: this.otpForm.value.otp
    }).subscribe({
      next: () => {
        this.isVerifying = false;
        this.snackBar.open('Account verified successfully!', 'OK', {
          duration: 3000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        });
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isVerifying = false;
        const message = err.error?.message || 'OTP verification failed. Please try again.';
        this.snackBar.open(message, 'Dismiss', {
          duration: 5000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom',
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  backToRegister(): void {
    this.showOtpStep = false;
    this.otpForm.reset();
  }
}
