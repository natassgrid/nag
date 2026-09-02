/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
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
      let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const pad = base64.length % 4;
      if (pad === 2) base64 += '==';
      else if (pad === 3) base64 += '=';
      else if (pad === 1) return null;
      const jsonStr = decodeURIComponent(
        atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonStr);
    } catch {
      return null;
    }
  }
}
