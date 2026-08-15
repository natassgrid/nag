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
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { ScheduleResponse } from './scheduling.service';

export interface AmendScheduleDialogData {
  schedule: ScheduleResponse;
}

@Component({
  selector: 'app-amend-schedule-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatDatepickerModule, MatNativeDateModule,
  ],
  templateUrl: './amend-schedule-dialog.component.html',
  styleUrls: ['./amend-schedule-dialog.component.scss']
})
export class AmendScheduleDialogComponent implements OnInit {
  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<AmendScheduleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AmendScheduleDialogData
  ) {}

  ngOnInit(): void {
    const s = this.data.schedule;
    this.form = this.fb.group({
      changeReason: ['', Validators.required],
      scheduleName: [s.scheduleName, Validators.required],
      notificationNumber: [s.notificationNumber || ''],
      examDate: [s.examDate ? new Date(s.examDate) : null, Validators.required],
      reserveDate: [s.reserveDate ? new Date(s.reserveDate) : null],
      effectiveFrom: [null],
      timeZone: [s.timeZone || 'Asia/Kolkata', Validators.required],
    });
  }

  save(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    this.dialogRef.close({
      changeReason: v.changeReason,
      scheduleName: v.scheduleName,
      notificationNumber: v.notificationNumber || undefined,
      examDate: this.toIso(v.examDate),
      reserveDate: v.reserveDate ? this.toIso(v.reserveDate) : undefined,
      effectiveFrom: v.effectiveFrom ? this.toIso(v.effectiveFrom) : undefined,
      timeZone: v.timeZone,
    });
  }

  private toIso(d: any): string {
    return d instanceof Date ? d.toISOString().split('T')[0] : d;
  }
}
