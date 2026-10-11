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

  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  handleRegister(): void {
    if (!this.fullName || !this.email || !this.mobile || !this.password) {
      this.errorMessage.set('Please fill out all required fields.');
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage.set('Passwords do not match.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const payload: CandidateRegistrationPayload = {
      fullName: this.fullName,
      email: this.email,
      mobile: this.mobile,
      password: this.password,
    };

    this.authFlowService.registerCandidate(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.authFlowService.navigateToVerifyOtp({
          email: this.email,
          mobile: this.mobile,
        });
      },
      error: (err) => {
        // Fallback for offline demo if network unavailable, else show error
        this.loading.set(false);
        const detail = err?.error?.detail || err?.error?.message;
        if (detail) {
          this.errorMessage.set(detail);
        } else {
          this.authFlowService.navigateToVerifyOtp({
            email: this.email,
            mobile: this.mobile,
          });
        }
      },
    });
  }
}
