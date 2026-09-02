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

import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { SchedulingService, CentreResponse, SeatAllocationResponse, SeatAllocationRequest } from './scheduling.service';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-seat-allocation-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    RightDrawerComponent
  ],
  templateUrl: './seat-allocation-dialog.component.html',
  styleUrls: ['./seat-allocation-dialog.component.scss']
})
export class SeatAllocationDialogComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() allocation?: SeatAllocationResponse | null = null;
  @Input() examId = '';
  @Input() scheduleId = '';
  @Input() shiftId = '';
  @Input() width = '560px';
  @Output() close = new EventEmitter<void>();
  @Output() saved = new EventEmitter<SeatAllocationResponse>();

  form!: FormGroup;
  centres: CentreResponse[] = [];
  loadingCentres = false;
  saving = false;
  saveError = '';

  constructor(
    private fb: FormBuilder,
    private schedulingService: SchedulingService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.initForm();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.initForm();
      this.saveError = '';
      this.loadCentres();
    }
    if (changes['allocation']) {
      this.initForm();
    }
  }

  initForm(): void {
    const a = this.allocation;
    this.form = this.fb.group({
      centreId: [a?.centreId || '', a ? [] : [Validators.required]],
      totalSeats: [a?.totalSeats || 0, [Validators.required, Validators.min(0)]],
      availableSeats: [a?.availableSeats || 0, [Validators.required, Validators.min(0)]],
      reservedSeats: [a?.reservedSeats || 0, Validators.min(0)],
      pwdSeats: [a?.pwdSeats || 0, Validators.min(0)],
      emergencyBufferSeats: [a?.emergencyBufferSeats || 0, Validators.min(0)],
      femaleReservedSeats: [a?.femaleReservedSeats || 0, Validators.min(0)],
      specialCategorySeats: [a?.specialCategorySeats || 0, Validators.min(0)],
    });
  }

  loadCentres(): void {
    if (this.allocation) return;
    this.loadingCentres = true;
    this.schedulingService.listCentres().subscribe({
      next: (list) => {
        this.centres = list;
        this.loadingCentres = false;
      },
      error: () => {
        this.loadingCentres = false;
      }
    });
  }

  cancel(): void {
    this.saveError = '';
    this.close.emit();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.examId || !this.scheduleId || !this.shiftId) {
      this.saveError = 'Cannot save allocation: Missing exam, schedule, or shift context.';
      return;
    }

    const v = this.form.value;
    const req: SeatAllocationRequest = {
      centreId: this.allocation?.centreId || v.centreId,
      totalSeats: v.totalSeats,
      availableSeats: v.availableSeats,
      reservedSeats: v.reservedSeats || 0,
      pwdSeats: v.pwdSeats || 0,
      emergencyBufferSeats: v.emergencyBufferSeats || 0,
      femaleReservedSeats: v.femaleReservedSeats || 0,
      specialCategorySeats: v.specialCategorySeats || 0,
    };

    this.saving = true;
    this.saveError = '';

    this.schedulingService.upsertAllocation(this.examId, this.scheduleId, this.shiftId, req).subscribe({
      next: (res) => {
        this.saving = false;
        this.saveError = '';
        this.saved.emit(res);
      },
      error: (err) => {
        this.saving = false;
        this.saveError = err?.error?.message || err?.error?.error || 'Failed to save seat allocation. Please try again.';
      }
    });
  }
}
