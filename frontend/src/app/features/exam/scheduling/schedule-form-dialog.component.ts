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

import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SchedulingService, ScheduleResponse, CreateScheduleRequest } from './scheduling.service';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-schedule-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    RightDrawerComponent
  ],
  templateUrl: './schedule-form-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./schedule-form-dialog.component.scss']
})
export class ScheduleFormDialogComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() schedule?: ScheduleResponse | null = null;
  @Input() examId = '';
  @Input() width = '540px';
  @Output() close = new EventEmitter<void>();
  @Output() saved = new EventEmitter<ScheduleResponse>();

  form!: FormGroup;
  minDate = new Date();
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
    }
    if (changes['schedule']) {
      this.initForm();
    }
  }

  initForm(): void {
    const s = this.schedule;
    this.form = this.fb.group({
      scheduleName: [s?.scheduleName || '', [Validators.required, Validators.maxLength(100)]],
      notificationNumber: [s?.notificationNumber || '', [Validators.maxLength(100)]],
      examDate: [s?.examDate ? this.parseDate(s.examDate) : null, Validators.required],
      reserveDate: [s?.reserveDate ? this.parseDate(s.reserveDate) : null],
      timeZone: [s?.timeZone || 'Asia/Kolkata', Validators.required],
    });
  }

  private parseDate(val: string): Date | null {
    if (!val) return null;
    const parts = val.split('-');
    if (parts.length === 3) {
      return new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
    }
    return new Date(val);
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
    if (!this.examId) {
      this.saveError = 'Cannot save schedule: Examination ID is missing.';
      return;
    }

    const v = this.form.value;
    const req: CreateScheduleRequest = {
      scheduleName: v.scheduleName.trim(),
      notificationNumber: v.notificationNumber?.trim() || undefined,
      examDate: this.toIso(v.examDate),
      reserveDate: v.reserveDate ? this.toIso(v.reserveDate) : undefined,
      timeZone: v.timeZone,
    };

    this.saving = true;
    this.saveError = '';

    const call$ = this.schedule
      ? this.schedulingService.amendSchedule(this.examId, this.schedule.id, {
          ...req,
          changeReason: 'Schedule updated (draft edit)'
        })
      : this.schedulingService.createSchedule(this.examId, req);

    call$.subscribe({
      next: (res) => {
        this.saving = false;
        this.saveError = '';
        this.saved.emit(res);
      },
      error: (err) => {
        this.saving = false;
        this.saveError = err?.error?.message || err?.error?.error || 'Failed to save schedule. Please check the details and try again.';
      }
    });
  }

  private toIso(d: any): string {
    if (!d) return '';
    if (d instanceof Date) {
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }
    if (typeof d === 'string') {
      return d.includes('T') ? d.split('T')[0] : d;
    }
    return String(d);
  }
}
