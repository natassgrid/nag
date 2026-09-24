import {
  Component,
  signal,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-candidate-register',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);

  fullName = '';
  email = '';
  mobile = '';
  identityDocType = 'AADHAAR';
  identityDocNumber = '';
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

    const payload = {
      fullName: this.fullName,
      email: this.email,
      mobile: this.mobile,
      identityDocType: this.identityDocType,
      identityDocNumber: this.identityDocNumber,
      password: this.password,
    };

    this.http.post('/api/v1/identity/register', payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/verify-otp'], {
          queryParams: { email: this.email, mobile: this.mobile },
        });
      },
      error: () => {
        // Fallback for offline demo
        this.loading.set(false);
        this.router.navigate(['/verify-otp'], {
          queryParams: { email: this.email, mobile: this.mobile },
        });
      },
    });
  }
}
