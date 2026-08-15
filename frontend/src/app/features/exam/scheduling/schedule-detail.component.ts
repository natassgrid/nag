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

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ExamManagementService, ExaminationResponse } from '../exam-manage/exam-management.service';
import {
  SchedulingService,
  ScheduleResponse,
  ShiftResponse,
  SeatAllocationResponse,
  CentreResponse
} from './scheduling.service';
import { ScheduleFormDialogComponent } from './schedule-form-dialog.component';
import { AmendScheduleDialogComponent } from './amend-schedule-dialog.component';
import { ShiftFormDialogComponent } from './shift-form-dialog.component';
import { SeatAllocationDialogComponent } from './seat-allocation-dialog.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-schedule-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatTableModule,
    MatChipsModule,
    MatTooltipModule,
    MatSelectModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
  ],
  templateUrl: './schedule-detail.component.html',
  styleUrls: ['./schedule-detail.component.scss']
})
export class ScheduleDetailComponent implements OnInit {

  examId = '';
  examName = '';
  activeTab = 0;

  // Schedules tab
  schedules: ScheduleResponse[] = [];
  loadingSchedules = false;
  scheduleColumns = ['version', 'scheduleName', 'examDate', 'reserveDate', 'notificationNumber', 'status', 'actions'];

  // Shifts tab
  selectedScheduleId: string | null = null;
  shifts: ShiftResponse[] = [];
  loadingShifts = false;
  shiftColumns = ['shiftNumber', 'shiftName', 'reportingTime', 'gateClosingTime', 'examStartTime', 'examEndTime', 'durationMinutes', 'shiftActions'];

  // Seats tab
  seatsScheduleId: string | null = null;
  seatsShiftId: string | null = null;
  seatsShifts: ShiftResponse[] = [];
  allocations: SeatAllocationResponse[] = [];
  loadingAllocations = false;
  allocColumns = ['centreId', 'totalSeats', 'availableSeats', 'pwdSeats', 'emergencyBufferSeats', 'allocActions'];

  // Workflow transition map
  private nextStatusMap: Record<string, string[]> = {
    'DRAFT': ['SCHEDULER_REVIEW', 'CANCELLED'],
    'SCHEDULER_REVIEW': ['CONTROLLER_APPROVED', 'CANCELLED'],
    'CONTROLLER_APPROVED': ['SECURITY_REVIEW', 'CANCELLED'],
    'SECURITY_REVIEW': ['CHAIRMAN_APPROVED', 'CANCELLED'],
    'CHAIRMAN_APPROVED': ['PUBLISHED', 'CANCELLED'],
    'PUBLISHED': ['CANCELLED'],
  };

  constructor(
    private route: ActivatedRoute,
    private examService: ExamManagementService,
    private schedulingService: SchedulingService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.examId = this.route.snapshot.paramMap.get('examId') || '';
    if (this.examId) {
      this.examService.getExam(this.examId).subscribe({
        next: (e) => { if (e) this.examName = e.name; },
        error: () => {}
      });
      this.loadSchedules();
    }
  }

  // ── Schedules ──────────────────────────────────────────────────────────────

  loadSchedules(): void {
    this.loadingSchedules = true;
    this.schedulingService.listSchedules(this.examId).subscribe({
      next: (data) => { this.schedules = data ?? []; this.loadingSchedules = false; this.cdr.detectChanges(); },
      error: (err) => { console.error('Failed to load schedules:', err); this.schedules = []; this.loadingSchedules = false; this.cdr.detectChanges(); }
    });
  }

  openCreateSchedule(): void {
    const ref = this.dialog.open(ScheduleFormDialogComponent, { width: '560px', data: {} });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.schedulingService.createSchedule(this.examId, result).subscribe({
        next: () => { this.snackBar.open('Schedule created', 'OK', { duration: 3000 }); this.loadSchedules(); },
        error: (e) => this.snackBar.open(e?.error?.message || 'Error', 'Dismiss', { duration: 4000 })
      });
    });
  }

  openEditSchedule(schedule: ScheduleResponse): void {
    const ref = this.dialog.open(ScheduleFormDialogComponent, {
      width: '560px',
      data: { schedule }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      // Use amend for DRAFT schedules too — backend creates a new version
      this.schedulingService.amendSchedule(this.examId, schedule.id, {
        ...result,
        changeReason: 'Schedule updated (draft edit)'
      }).subscribe({
        next: () => { this.snackBar.open('Schedule updated', 'OK', { duration: 3000 }); this.loadSchedules(); },
        error: (e) => {
          // If amend fails (only works on PUBLISHED), fall back to create
          this.schedulingService.createSchedule(this.examId, result).subscribe({
            next: () => { this.snackBar.open('Schedule recreated', 'OK', { duration: 3000 }); this.loadSchedules(); },
            error: (e2) => this.snackBar.open(e2?.error?.message || 'Error', 'Dismiss', { duration: 4000 })
          });
        }
      });
    });
  }

  openTransition(schedule: ScheduleResponse): void {
    const targets = this.nextStatusMap[schedule.status] || [];
    if (targets.length === 0) return;
    const target = targets[0]; // default to forward transition
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Transition Schedule',
        message: `Transition "${schedule.scheduleName}" from ${schedule.status} to ${target}?`,
        confirmText: `Transition to ${target}`,
        color: 'primary',
        icon: 'arrow_forward'
      } as ConfirmDialogData
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.schedulingService.transitionSchedule(this.examId, schedule.id, { targetStatus: target }).subscribe({
        next: () => { this.snackBar.open(`Transitioned to ${target}`, 'OK', { duration: 3000 }); this.loadSchedules(); },
        error: (e) => this.snackBar.open(e?.error?.message || 'Transition failed', 'Dismiss', { duration: 4000 })
      });
    });
  }

  cancelSchedule(schedule: ScheduleResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Cancel Schedule',
        message: `Cancel schedule "${schedule.scheduleName}" v${schedule.scheduleVersion}? This cannot be undone.`,
        confirmText: 'Cancel Schedule',
        color: 'warn',
        icon: 'cancel'
      } as ConfirmDialogData
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.schedulingService.transitionSchedule(this.examId, schedule.id, { targetStatus: 'CANCELLED' }).subscribe({
        next: () => { this.snackBar.open('Schedule cancelled', 'OK', { duration: 3000 }); this.loadSchedules(); },
        error: (e) => this.snackBar.open(e?.error?.message || 'Error', 'Dismiss', { duration: 4000 })
      });
    });
  }

  openAmend(schedule: ScheduleResponse): void {
    const ref = this.dialog.open(AmendScheduleDialogComponent, { width: '600px', data: { schedule } });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.schedulingService.amendSchedule(this.examId, schedule.id, result).subscribe({
        next: () => { this.snackBar.open('Amendment created', 'OK', { duration: 3000 }); this.loadSchedules(); },
        error: (e) => this.snackBar.open(e?.error?.message || 'Error', 'Dismiss', { duration: 4000 })
      });
    });
  }

  selectScheduleForShifts(schedule: ScheduleResponse): void {
    this.selectedScheduleId = schedule.id;
    this.activeTab = 1;
    this.loadShifts();
  }

  // ── Shifts ─────────────────────────────────────────────────────────────────

  loadShifts(): void {
    if (!this.selectedScheduleId) return;
    this.loadingShifts = true;
    this.schedulingService.listShifts(this.examId, this.selectedScheduleId).subscribe({
      next: (data) => { this.shifts = data ?? []; this.loadingShifts = false; this.cdr.detectChanges(); },
      error: (err) => { console.error('Failed to load shifts:', err); this.shifts = []; this.loadingShifts = false; this.cdr.detectChanges(); }
    });
  }

  openAddShift(): void {
    const ref = this.dialog.open(ShiftFormDialogComponent, { width: '640px', data: {} });
    ref.afterClosed().subscribe(result => {
      if (!result || !this.selectedScheduleId) return;
      this.schedulingService.addShift(this.examId, this.selectedScheduleId, result).subscribe({
        next: () => { this.snackBar.open('Shift added', 'OK', { duration: 3000 }); this.loadShifts(); },
        error: (e) => this.snackBar.open(e?.error?.message || 'Error', 'Dismiss', { duration: 4000 })
      });
    });
  }

  openEditShift(shift: ShiftResponse): void {
    const ref = this.dialog.open(ShiftFormDialogComponent, { width: '640px', data: { shift } });
    ref.afterClosed().subscribe(result => {
      if (!result || !this.selectedScheduleId) return;
      this.schedulingService.updateShift(this.examId, this.selectedScheduleId, shift.id, result).subscribe({
        next: () => { this.snackBar.open('Shift updated', 'OK', { duration: 3000 }); this.loadShifts(); },
        error: (e) => this.snackBar.open(e?.error?.message || 'Error', 'Dismiss', { duration: 4000 })
      });
    });
  }

  selectShiftForSeats(shift: ShiftResponse): void {
    this.seatsScheduleId = this.selectedScheduleId;
    this.seatsShiftId = shift.id;
    this.seatsShifts = this.shifts;
    this.activeTab = 2;
    this.loadAllocations();
  }

  // ── Seats ──────────────────────────────────────────────────────────────────

  loadShiftsForSeats(): void {
    if (!this.seatsScheduleId) return;
    this.schedulingService.listShifts(this.examId, this.seatsScheduleId).subscribe({
      next: (data) => { this.seatsShifts = data ?? []; this.seatsShiftId = null; this.allocations = []; this.cdr.detectChanges(); },
      error: () => { this.seatsShifts = []; this.cdr.detectChanges(); }
    });
  }

  loadAllocations(): void {
    if (!this.seatsScheduleId || !this.seatsShiftId) return;
    this.loadingAllocations = true;
    this.schedulingService.listAllocations(this.examId, this.seatsScheduleId, this.seatsShiftId).subscribe({
      next: (data) => { this.allocations = data ?? []; this.loadingAllocations = false; this.cdr.detectChanges(); },
      error: (err) => { console.error('Failed to load allocations:', err); this.allocations = []; this.loadingAllocations = false; this.cdr.detectChanges(); }
    });
  }

  openAddAllocation(): void {
    const ref = this.dialog.open(SeatAllocationDialogComponent, { width: '560px', data: {} });
    ref.afterClosed().subscribe(result => {
      if (!result || !this.seatsScheduleId || !this.seatsShiftId) return;
      this.schedulingService.upsertAllocation(this.examId, this.seatsScheduleId, this.seatsShiftId, result).subscribe({
        next: () => { this.snackBar.open('Allocation saved', 'OK', { duration: 3000 }); this.loadAllocations(); },
        error: (e) => this.snackBar.open(e?.error?.message || 'Error', 'Dismiss', { duration: 4000 })
      });
    });
  }

  openEditAllocation(alloc: SeatAllocationResponse): void {
    const ref = this.dialog.open(SeatAllocationDialogComponent, { width: '560px', data: { allocation: alloc } });
    ref.afterClosed().subscribe(result => {
      if (!result || !this.seatsScheduleId || !this.seatsShiftId) return;
      this.schedulingService.upsertAllocation(this.examId, this.seatsScheduleId, this.seatsShiftId, result).subscribe({
        next: () => { this.snackBar.open('Allocation updated', 'OK', { duration: 3000 }); this.loadAllocations(); },
        error: (e) => this.snackBar.open(e?.error?.message || 'Error', 'Dismiss', { duration: 4000 })
      });
    });
  }
}
