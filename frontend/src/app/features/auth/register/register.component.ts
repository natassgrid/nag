import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
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
    MatCheckboxModule,
    RouterLink
  ],
  template: `
    <main id="main-content" class="auth-container" role="main" aria-labelledby="register-heading">
      <mat-card class="auth-card">
        <mat-card-header>
          <mat-card-title id="register-heading">Register</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" aria-label="Registration form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Full Name</mat-label>
              <input matInput formControlName="name" aria-required="true" autocomplete="name">
              <mat-error *ngIf="registerForm.get('name')?.hasError('required')">Name is required</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" autocomplete="email" aria-required="true">
              <mat-error *ngIf="registerForm.get('email')?.hasError('email')">Enter a valid email</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Mobile Number</mat-label>
              <input matInput formControlName="mobile" type="tel" autocomplete="tel" aria-required="true">
              <mat-error *ngIf="registerForm.get('mobile')?.hasError('pattern')">Enter a valid 10-digit mobile number</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Identity Document Type</mat-label>
              <mat-select formControlName="identityDocType" aria-required="true">
                <mat-option value="Aadhaar">Aadhaar</mat-option>
                <mat-option value="PAN">PAN</mat-option>
                <mat-option value="Passport">Passport</mat-option>
                <mat-option value="VoterID">Voter ID</mat-option>
                <mat-option value="DL">Driving License</mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Identity Document Number</mat-label>
              <input matInput formControlName="identityDocNumber" aria-required="true">
              <mat-error *ngIf="registerForm.get('identityDocNumber')?.hasError('required')">Document number is required</mat-error>
            </mat-form-field>

            <mat-checkbox formControlName="consent" aria-required="true" class="consent-checkbox">
              I consent to the collection and processing of my data as per the privacy policy.
            </mat-checkbox>
            <div *ngIf="registerForm.get('consent')?.hasError('requiredTrue') && registerForm.get('consent')?.touched"
                 class="error-message" role="alert">
              You must provide consent to register.
            </div>

            <div class="form-actions">
              <button mat-raised-button color="primary" type="submit"
                      [disabled]="registerForm.invalid || isLoading"
                      aria-label="Submit registration">
                {{ isLoading ? 'Registering...' : 'Register' }}
              </button>
            </div>

            <div *ngIf="errorMessage" class="error-message" role="alert" aria-live="assertive">
              {{ errorMessage }}
            </div>
          </form>

          <div class="auth-links">
            <a [routerLink]="['/auth/login']" aria-label="Already have an account? Sign in">
              Already have an account? Sign In
            </a>
          </div>

          <div class="accessibility-toggle">
            <mat-checkbox (change)="toggleHighContrast($event.checked)"
                          aria-label="Enable high contrast mode">
              High Contrast Mode
            </mat-checkbox>
          </div>
        </mat-card-content>
      </mat-card>
    </main>
  `,
  styles: [`
    .auth-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: var(--spacing-md);
    }
    .auth-card { max-width: 480px; width: 100%; padding: var(--spacing-lg); }
    .full-width { width: 100%; margin-bottom: var(--spacing-sm); }
    .form-actions { margin-top: var(--spacing-md); }
    .error-message { color: var(--color-error); margin-top: var(--spacing-sm); font-weight: 500; }
    .auth-links { margin-top: var(--spacing-lg); text-align: center; }
    .accessibility-toggle { margin-top: var(--spacing-md); }
    .consent-checkbox { margin-top: var(--spacing-sm); }
  `]
})
export class RegisterComponent {
  registerForm: FormGroup;
  isLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      name: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      mobile: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]],
      identityDocType: ['', [Validators.required]],
      identityDocNumber: ['', [Validators.required]],
      consent: [false, [Validators.requiredTrue]]
    });
  }

  onSubmit(): void {
    if (this.registerForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';

    const formData = this.registerForm.value;
    this.authService.register(formData).subscribe({
      next: (res) => {
        this.router.navigate(['/auth/otp-verify'], {
          queryParams: { registrationId: res.registrationId }
        });
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Registration failed. Please try again.';
      }
    });
  }

  toggleHighContrast(enabled: boolean): void {
    document.body.classList.toggle('high-contrast', enabled);
  }
}
