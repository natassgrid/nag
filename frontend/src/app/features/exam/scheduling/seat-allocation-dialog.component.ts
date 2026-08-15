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

import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { SchedulingService, CentreResponse, SeatAllocationResponse } from './scheduling.service';

export interface SeatAllocationDialogData {
  allocation?: SeatAllocationResponse;
}

@Component({
  selector: 'app-seat-allocation-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule,
  ],
  templateUrl: './seat-allocation-dialog.component.html',
  styleUrls: ['./seat-allocation-dialog.component.scss']
})
export class SeatAllocationDialogComponent implements OnInit {
  form!: FormGroup;
  centres: CentreResponse[] = [];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<SeatAllocationDialogComponent>,
    private schedulingService: SchedulingService,
    @Inject(MAT_DIALOG_DATA) public data: SeatAllocationDialogData
  ) {}

  ngOnInit(): void {
    const a = this.data.allocation;
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

    // Load centres for dropdown (only needed when creating)
    if (!a) {
      this.schedulingService.listCentres().subscribe(list => this.centres = list);
    }
  }

  save(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    this.dialogRef.close({
      centreId: this.data.allocation?.centreId || v.centreId,
      totalSeats: v.totalSeats,
      availableSeats: v.availableSeats,
      reservedSeats: v.reservedSeats,
      pwdSeats: v.pwdSeats,
      emergencyBufferSeats: v.emergencyBufferSeats,
      femaleReservedSeats: v.femaleReservedSeats,
      specialCategorySeats: v.specialCategorySeats,
    });
  }
}
