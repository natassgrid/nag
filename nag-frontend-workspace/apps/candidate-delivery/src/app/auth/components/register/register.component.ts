import {
  Component,
  signal,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { parseHttpError } from '@nag-frontend-workspace/shared-ui-components';
import { AuthBrandHeaderComponent } from '../auth-brand-header/auth-brand-header.component';
import { AuthFlowService } from '../../services/auth-flow.service';
import { CandidateRegistrationPayload } from '../../models/auth-flow.model';

@Component({
  selector: 'app-candidate-register',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    AuthBrandHeaderComponent,
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private readonly authFlowService = inject(AuthFlowService);

  fullName = '';
  email = '';
  mobile = '';
  password = '';
  confirmPassword = '';

  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly fieldErrors = signal<Record<string, string>>({});

  clearFieldError(field: string): void {
    const current = { ...this.fieldErrors() };
    if (current[field]) {
      delete current[field];
      this.fieldErrors.set(current);
    }
    this.errorMessage.set(null);
  }

  validateSingleField(field: string): void {
    const error = this.getFieldValidationError(field);
    const current = { ...this.fieldErrors() };
    if (error) {
      current[field] = error;
    } else {
      delete current[field];
    }
    this.fieldErrors.set(current);
  }

  validateField(field: string): void {
    this.validateSingleField(field);
  }

  private getFieldValidationError(field: string): string | null {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const mobileRegex = /^[6-9]\d{9}$/;

    switch (field) {
      case 'fullName': {
        const trimmed = (this.fullName || '').trim();
        if (!trimmed || trimmed.length < 3 || trimmed.length > 100) {
          return 'Full legal name must be between 3 and 100 characters.';
        }
        return null;
      }
      case 'email': {
        const trimmed = (this.email || '').trim();
        if (!trimmed || !emailRegex.test(trimmed)) {
          return 'Please enter a valid email address.';
        }
        return null;
      }
      case 'mobile': {
        const trimmed = (this.mobile || '').trim();
        if (!trimmed || !mobileRegex.test(trimmed)) {
          return 'Mobile number must be a valid 10-digit Indian number starting with 6, 7, 8, or 9.';
        }
        return null;
      }
      case 'password': {
        if (!this.password || this.password.length < 8) {
          return 'Password must be at least 8 characters long.';
        }
        return null;
      }
      case 'confirmPassword': {
        if (!this.confirmPassword) {
          return 'Please confirm your password.';
        }
        if (this.password !== this.confirmPassword) {
          return 'Passwords do not match.';
        }
        return null;
      }
      default:
        return null;
    }
  }

  handleRegister(): void {
    const fields = ['fullName', 'email', 'mobile', 'password', 'confirmPassword'];
    const errors: Record<string, string> = {};

    for (const field of fields) {
      const err = this.getFieldValidationError(field);
      if (err) {
        errors[field] = err;
      }
    }

    if (Object.keys(errors).length > 0) {
      this.fieldErrors.set(errors);
      const firstError = Object.values(errors)[0];
      this.errorMessage.set(firstError);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.fieldErrors.set({});

    const payload: CandidateRegistrationPayload = {
      fullName: this.fullName.trim(),
      email: this.email.trim(),
      mobile: this.mobile.trim(),
      password: this.password,
    };

    this.authFlowService.registerCandidate(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.authFlowService.navigateToVerifyOtp({
          email: this.email.trim(),
          mobile: this.mobile.trim(),
        });
      },
      error: (err) => {
        this.loading.set(false);
        const parsed = parseHttpError(err, 'Registration failed. Please verify your details and try again.');
        this.fieldErrors.set(parsed.fieldErrors);
        this.errorMessage.set(parsed.message);
      },
    });
  }
}
