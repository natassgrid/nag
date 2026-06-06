import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { ProfileService, CandidateProfile } from './profile.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDividerModule
  ],
  template: `
    <div class="profile-container" role="main" aria-labelledby="profile-heading">
      <mat-card class="profile-card" appearance="outlined" *ngIf="profile; else loading">
        <mat-card-header class="profile-header">
          <div class="profile-avatar">
            <mat-icon class="avatar-icon" aria-hidden="true">account_circle</mat-icon>
          </div>
          <h1 id="profile-heading" class="profile-name">{{ profile.name }}</h1>
          <mat-chip-set aria-label="Verification status">
            <mat-chip [class.verified]="profile.verificationStatus === 'VERIFIED'"
                      [class.pending]="profile.verificationStatus !== 'VERIFIED'"
                      highlighted>
              <mat-icon matChipAvatar>
                {{ profile.verificationStatus === 'VERIFIED' ? 'verified' : 'pending' }}
              </mat-icon>
              {{ profile.verificationStatus }}
            </mat-chip>
          </mat-chip-set>
        </mat-card-header>

        <mat-divider></mat-divider>

        <mat-card-content class="profile-details">
          <div class="detail-row">
            <mat-icon class="detail-icon" aria-hidden="true">email</mat-icon>
            <div class="detail-content">
              <span class="detail-label">Email</span>
              <span class="detail-value">{{ profile.email }}</span>
            </div>
          </div>

          <div class="detail-row">
            <mat-icon class="detail-icon" aria-hidden="true">phone</mat-icon>
            <div class="detail-content">
              <span class="detail-label">Mobile Number</span>
              <span class="detail-value">{{ profile.mobile }}</span>
            </div>
          </div>

          <div class="detail-row">
            <mat-icon class="detail-icon" aria-hidden="true">badge</mat-icon>
            <div class="detail-content">
              <span class="detail-label">Document Type</span>
              <span class="detail-value">{{ profile.identityDocType }}</span>
            </div>
          </div>

          <div class="detail-row">
            <mat-icon class="detail-icon" aria-hidden="true">article</mat-icon>
            <div class="detail-content">
              <span class="detail-label">Document Number</span>
              <span class="detail-value">{{ maskDocNumber(profile.identityDocNumber) }}</span>
            </div>
          </div>

          <div class="detail-row">
            <mat-icon class="detail-icon" aria-hidden="true">calendar_today</mat-icon>
            <div class="detail-content">
              <span class="detail-label">Registration Date</span>
              <span class="detail-value">{{ profile.registrationDate | date:'mediumDate' }}</span>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <ng-template #loading>
        <div class="loading-container" *ngIf="isLoading" aria-label="Loading profile">
          <mat-spinner diameter="40"></mat-spinner>
          <p>Loading profile...</p>
        </div>
        <mat-card class="profile-card error-card" appearance="outlined" *ngIf="errorMessage">
          <mat-card-content class="error-content">
            <mat-icon class="error-icon">error_outline</mat-icon>
            <p>{{ errorMessage }}</p>
          </mat-card-content>
        </mat-card>
      </ng-template>
    </div>
  `,
  styles: [`
    .profile-container {
      display: flex;
      justify-content: center;
      padding: 32px 16px;
    }

    .profile-card {
      max-width: 560px;
      width: 100%;
      border-radius: 12px;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06), 0 1px 4px rgba(0, 0, 0, 0.03);
    }

    .profile-header {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 32px 24px 24px;
    }

    .profile-avatar {
      margin-bottom: 12px;
    }

    .avatar-icon {
      font-size: 72px;
      width: 72px;
      height: 72px;
      color: #3f51b5;
    }

    .profile-name {
      margin: 0 0 12px;
      font-size: 24px;
      font-weight: 600;
      color: #333;
    }

    .verified {
      background-color: #e8f5e9 !important;
      color: #2e7d32 !important;
    }

    .pending {
      background-color: #fff3e0 !important;
      color: #e65100 !important;
    }

    .profile-details {
      padding: 24px;
    }

    .detail-row {
      display: flex;
      align-items: flex-start;
      gap: 16px;
      padding: 12px 0;
      border-bottom: 1px solid #f0f0f0;
    }

    .detail-row:last-child {
      border-bottom: none;
    }

    .detail-icon {
      color: #666;
      margin-top: 2px;
    }

    .detail-content {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .detail-label {
      font-size: 12px;
      color: #888;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      font-weight: 500;
    }

    .detail-value {
      font-size: 15px;
      color: #333;
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding: 48px;
      color: #666;
    }

    .error-card {
      max-width: 400px;
      width: 100%;
    }

    .error-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 32px;
      text-align: center;
      color: #666;
    }

    .error-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #f44336;
      margin-bottom: 12px;
    }
  `]
})
export class ProfileComponent implements OnInit {
  profile: CandidateProfile | null = null;
  isLoading = true;
  errorMessage = '';

  constructor(
    private profileService: ProfileService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const userId = this.authService.getUserId();
    if (!userId) {
      this.isLoading = false;
      this.errorMessage = 'Unable to determine user ID. Please log in again.';
      return;
    }

    this.profileService.getProfile(userId).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.isLoading = false;
      },
      error: () => {
        // Fallback: show basic info from JWT token when candidate profile doesn't exist
        const token = this.authService.getToken();
        const payload = token ? this.decodePayload(token) : null;
        this.profile = {
          id: userId,
          name: payload?.preferred_username || userId,
          email: '-',
          mobile: '-',
          identityDocType: '-',
          identityDocNumber: '-',
          verificationStatus: 'ACTIVE',
          registrationDate: ''
        };
        this.isLoading = false;
      }
    });
  }

  private decodePayload(token: string): any {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      return JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
    } catch { return null; }
  }

  maskDocNumber(docNumber: string): string {
    if (!docNumber || docNumber.length <= 4) return docNumber;
    const visible = docNumber.slice(-4);
    const masked = '*'.repeat(docNumber.length - 4);
    return masked + visible;
  }
}
