import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ProfileService, CandidateProfile, ProfileCreateUpdateRequest } from './profile.service';
import { AuthService } from '../../core/services/auth.service';

const DOC_TYPES = ['AADHAAR', 'PAN', 'PASSPORT', 'VOTER_ID', 'DRIVING_LICENSE'];

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSnackBarModule
  ],
  template: `
    <div class="profile-container" role="main" aria-labelledby="profile-heading">

      <!-- Profile View Mode -->
      <mat-card class="profile-card" appearance="outlined" *ngIf="profile && !isEditing">
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

        <mat-card-actions class="profile-actions">
          <button mat-raised-button color="primary" (click)="startEdit()"
                  aria-label="Edit profile">
            <mat-icon>edit</mat-icon>
            Edit Profile
          </button>
        </mat-card-actions>
      </mat-card>

      <!-- Create / Edit Form -->
      <mat-card class="profile-card" appearance="outlined" *ngIf="isEditing || profileNotFound">
        <mat-card-header class="profile-header">
          <div class="profile-avatar">
            <mat-icon class="avatar-icon" aria-hidden="true">{{ profileNotFound ? 'person_add' : 'edit' }}</mat-icon>
          </div>
          <h1 id="profile-heading" class="profile-name">
            {{ profileNotFound ? 'Create Your Profile' : 'Edit Profile' }}
          </h1>
          <p class="form-subtitle" *ngIf="profileNotFound">
            Complete your profile to get started with the platform.
          </p>
        </mat-card-header>

        <mat-divider></mat-divider>

        <mat-card-content class="form-content">
          <form [formGroup]="profileForm" (ngSubmit)="saveProfile()" class="profile-form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Full Name</mat-label>
              <input matInput formControlName="name" placeholder="Your full name" />
              <mat-icon matPrefix>person</mat-icon>
              <mat-error *ngIf="profileForm.get('name')?.hasError('required')">Name is required</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" placeholder="your@email.com" />
              <mat-icon matPrefix>email</mat-icon>
              <mat-error *ngIf="profileForm.get('email')?.hasError('required')">Email is required</mat-error>
              <mat-error *ngIf="profileForm.get('email')?.hasError('email')">Enter a valid email</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Mobile Number</mat-label>
              <input matInput formControlName="mobile" placeholder="10-digit mobile number" />
              <mat-icon matPrefix>phone</mat-icon>
              <mat-error *ngIf="profileForm.get('mobile')?.hasError('required')">Mobile is required</mat-error>
              <mat-error *ngIf="profileForm.get('mobile')?.hasError('pattern')">Enter a valid 10-digit number</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Identity Document Type</mat-label>
              <mat-select formControlName="identityDocType">
                <mat-option *ngFor="let doc of docTypes" [value]="doc">{{ doc }}</mat-option>
              </mat-select>
              <mat-icon matPrefix>badge</mat-icon>
              <mat-error *ngIf="profileForm.get('identityDocType')?.hasError('required')">Document type is required</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Identity Document Number</mat-label>
              <input matInput formControlName="identityDocNumber" placeholder="Document number" />
              <mat-icon matPrefix>article</mat-icon>
              <mat-error *ngIf="profileForm.get('identityDocNumber')?.hasError('required')">Document number is required</mat-error>
            </mat-form-field>

            <div class="form-actions">
              <button mat-button type="button" (click)="cancelEdit()" *ngIf="isEditing && !profileNotFound">
                Cancel
              </button>
              <button mat-raised-button color="primary" type="submit"
                      [disabled]="profileForm.invalid || isSaving">
                <mat-icon *ngIf="!isSaving">{{ profileNotFound ? 'person_add' : 'save' }}</mat-icon>
                <mat-spinner *ngIf="isSaving" diameter="20" class="button-spinner"></mat-spinner>
                <span *ngIf="!isSaving">{{ profileNotFound ? 'Create Profile' : 'Save Changes' }}</span>
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
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

    .form-subtitle {
      margin: 0;
      font-size: 14px;
      color: #666;
      text-align: center;
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

    .profile-actions {
      display: flex;
      justify-content: center;
      padding: 16px 24px 24px;
    }

    .form-content {
      padding: 24px;
    }

    .profile-form {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .full-width {
      width: 100%;
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 16px;
    }

    .button-spinner {
      display: inline-block;
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding: 48px;
      color: #666;
    }
  `]
})
export class ProfileComponent implements OnInit {
  profile: CandidateProfile | null = null;
  isEditing = false;
  isSaving = false;
  profileNotFound = true;
  profileForm!: FormGroup;
  docTypes = DOC_TYPES;

  private userId: string | null = null;

  constructor(
    private profileService: ProfileService,
    private authService: AuthService,
    private fb: FormBuilder,
    private snackBar: MatSnackBar
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.userId = this.authService.getUserId();
    this.prefillFromToken();

    if (this.userId) {
      this.profileNotFound = false;

      this.profileService.getProfile(this.userId).subscribe({
        next: (profile) => {
          if (profile) {
            this.profile = profile;
            this.profileNotFound = false;
          } else {
            this.profileNotFound = true;
          }
        },
        error: () => {
          this.profileNotFound = true;
        }
      });
    }
  }

  startEdit(): void {
    if (this.profile) {
      this.profileForm.patchValue({
        name: this.profile.name,
        email: this.profile.email,
        mobile: this.profile.mobile,
        identityDocType: this.profile.identityDocType,
        identityDocNumber: this.profile.identityDocNumber
      });
    }
    this.isEditing = true;
  }

  cancelEdit(): void {
    this.isEditing = false;
  }

  saveProfile(): void {
    if (this.profileForm.invalid || !this.userId) return;

    this.isSaving = true;
    const data: ProfileCreateUpdateRequest = this.profileForm.value;

    const operation = this.profileNotFound
      ? this.profileService.createProfile(data)
      : this.profileService.updateProfile(this.userId, data);

    operation.subscribe({
      next: (profile) => {
        this.profile = profile;
        this.profileNotFound = false;
        this.isEditing = false;
        this.isSaving = false;
        this.snackBar.open(
          this.profileNotFound ? 'Profile created successfully' : 'Profile updated successfully',
          'OK', { duration: 3000 }
        );
      },
      error: (err) => {
        this.isSaving = false;
        const msg = err.error?.detail || err.error?.message || 'Failed to save profile';
        this.snackBar.open(msg, 'Dismiss', { duration: 5000 });
      }
    });
  }

  maskDocNumber(docNumber: string): string {
    if (!docNumber || docNumber.length <= 4) return docNumber;
    const visible = docNumber.slice(-4);
    const masked = '*'.repeat(docNumber.length - 4);
    return masked + visible;
  }

  private initForm(): void {
    this.profileForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      mobile: ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
      identityDocType: ['', Validators.required],
      identityDocNumber: ['', Validators.required]
    });
  }

  private prefillFromToken(): void {
    const token = this.authService.getToken();
    const payload = token ? this.decodePayload(token) : null;
    if (payload) {
      this.profileForm.patchValue({
        name: payload.preferred_username || payload.name || '',
        email: payload.email || ''
      });
    }
  }

  private decodePayload(token: string): any {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      return JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
    } catch { return null; }
  }
}
