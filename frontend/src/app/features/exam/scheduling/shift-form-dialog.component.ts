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
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { SchedulingService, ShiftResponse, CreateShiftRequest } from './scheduling.service';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-shift-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    RightDrawerComponent
  ],
  templateUrl: './shift-form-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./shift-form-dialog.component.scss']
})
export class ShiftFormDialogComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() shift?: ShiftResponse | null = null;
  @Input() examId = '';
  @Input() scheduleId = '';
  @Input() width = '560px';
  @Output() close = new EventEmitter<void>();
  @Output() saved = new EventEmitter<ShiftResponse>();

  form!: FormGroup;
  timingErrors: string[] = [];
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
    if (changes['shift']) {
      this.initForm();
    }
  }

  initForm(): void {
    const s = this.shift;
    this.form = this.fb.group({
      shiftNumber: [s?.shiftNumber || 1, [Validators.required, Validators.min(1)]],
      shiftName: [s?.shiftName || ''],
      reportingTime: [s?.reportingTime || '', Validators.required],
      gateClosingTime: [s?.gateClosingTime || '', Validators.required],
      loginStartTime: [s?.loginStartTime || '', Validators.required],
      examStartTime: [s?.examStartTime || '', Validators.required],
      examEndTime: [s?.examEndTime || '', Validators.required],
      exitTime: [s?.exitTime || ''],
      durationMinutes: [s?.durationMinutes || 180, [Validators.required, Validators.min(1)]],
      bufferMinutes: [s?.bufferMinutes || 0, Validators.min(0)],
    });

    this.form.get('examStartTime')!.valueChanges.subscribe(() => this.autoComputeDuration());
    this.form.get('examEndTime')!.valueChanges.subscribe(() => this.autoComputeDuration());

    this.form.get('reportingTime')!.valueChanges.subscribe((reporting) => {
      if (!reporting || this.shift) return;
      const gate = this.form.get('gateClosingTime')!.value;
      const login = this.form.get('loginStartTime')!.value;
      const start = this.form.get('examStartTime')!.value;
      if (!gate) this.form.get('gateClosingTime')!.setValue(this.addMinutes(reporting, 60), { emitEvent: false });
      if (!login) this.form.get('loginStartTime')!.setValue(this.addMinutes(reporting, 75), { emitEvent: false });
      if (!start) this.form.get('examStartTime')!.setValue(this.addMinutes(reporting, 90), { emitEvent: false });
      if (!this.form.get('examEndTime')!.value && this.form.get('durationMinutes')!.value > 0) {
        const examStart = this.form.get('examStartTime')!.value || this.addMinutes(reporting, 90);
        this.form.get('examEndTime')!.setValue(
          this.addMinutes(examStart, this.form.get('durationMinutes')!.value), { emitEvent: false }
        );
      }
      this.validateTimings();
    });

    this.form.valueChanges.subscribe(() => this.validateTimings());
  }

  private addMinutes(time: string, minutes: number): string {
    const [h, m] = time.split(':').map(Number);
    const total = h * 60 + m + minutes;
    const nh = Math.floor(total / 60) % 24;
    const nm = total % 60;
    return `${nh.toString().padStart(2, '0')}:${nm.toString().padStart(2, '0')}`;
  }

  private autoComputeDuration(): void {
    const start = this.form.get('examStartTime')!.value;
    const end = this.form.get('examEndTime')!.value;
    if (start && end) {
      const mins = this.diffMinutes(start, end);
      if (mins > 0) {
        this.form.get('durationMinutes')!.setValue(mins, { emitEvent: false });
      }
    }
  }

  private validateTimings(): void {
    const errors: string[] = [];
    const v = this.form.value;

    if (v.reportingTime && v.gateClosingTime && v.reportingTime >= v.gateClosingTime) {
      errors.push('Reporting Time must be before Gate Closing Time');
    }
    if (v.gateClosingTime && v.loginStartTime && v.gateClosingTime >= v.loginStartTime) {
      errors.push('Gate Closing Time must be before Login Start Time');
    }
    if (v.loginStartTime && v.examStartTime && v.loginStartTime >= v.examStartTime) {
      errors.push('Login Start Time must be before Exam Start Time');
    }
    if (v.examStartTime && v.examEndTime && v.examStartTime >= v.examEndTime) {
      errors.push('Exam Start Time must be before Exam End Time');
    }
    if (v.examStartTime && v.examEndTime && v.durationMinutes) {
      const computed = this.diffMinutes(v.examStartTime, v.examEndTime);
      if (computed > 0 && computed !== v.durationMinutes) {
        errors.push(`Duration must equal End - Start (${computed} min), got ${v.durationMinutes}`);
      }
    }

    this.timingErrors = errors;
  }

  private diffMinutes(start: string, end: string): number {
    const [sh, sm] = start.split(':').map(Number);
    const [eh, em] = end.split(':').map(Number);
    return (eh * 60 + em) - (sh * 60 + sm);
  }

  cancel(): void {
    this.saveError = '';
    this.close.emit();
  }

  save(): void {
    if (this.form.invalid || this.timingErrors.length > 0) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.examId || !this.scheduleId) {
      this.saveError = 'Cannot save shift: Examination or Schedule ID is missing.';
      return;
    }

    const v = this.form.value;
    const req: CreateShiftRequest = {
      shiftNumber: v.shiftNumber,
      shiftName: v.shiftName?.trim() || undefined,
      reportingTime: v.reportingTime,
      gateClosingTime: v.gateClosingTime,
      loginStartTime: v.loginStartTime,
      examStartTime: v.examStartTime,
      examEndTime: v.examEndTime,
      exitTime: v.exitTime?.trim() || undefined,
      durationMinutes: v.durationMinutes,
      bufferMinutes: v.bufferMinutes || 0,
    };

    this.saving = true;
    this.saveError = '';

    const call$ = this.shift
      ? this.schedulingService.updateShift(this.examId, this.scheduleId, this.shift.id, req)
      : this.schedulingService.addShift(this.examId, this.scheduleId, req);

    call$.subscribe({
      next: (res) => {
        this.saving = false;
        this.saveError = '';
        this.saved.emit(res);
      },
      error: (err) => {
        this.saving = false;
        this.saveError = err?.error?.message || err?.error?.error || 'Failed to save shift. Please check the details and try again.';
      }
    });
  }
}
