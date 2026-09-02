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
import { ActivatedRoute, Router } from '@angular/router';
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
  SeatAllocationResponse
} from './scheduling.service';
import { ScheduleFormDialogComponent } from './schedule-form-dialog.component';
import { ShiftFormDialogComponent } from './shift-form-dialog.component';
import { SeatAllocationDialogComponent } from './seat-allocation-dialog.component';
import { AmendScheduleDialogComponent } from './amend-schedule-dialog.component';
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
    ScheduleFormDialogComponent,
    ShiftFormDialogComponent,
    SeatAllocationDialogComponent,
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

  // Schedule Drawer State
  scheduleDrawerOpen = false;
  editingSchedule: ScheduleResponse | null = null;

  // Shifts tab
  selectedScheduleId: string | null = null;
  shifts: ShiftResponse[] = [];
  loadingShifts = false;
  shiftColumns = ['shiftNumber', 'shiftName', 'reportingTime', 'gateClosingTime', 'examStartTime', 'examEndTime', 'durationMinutes', 'shiftActions'];

  // Shift Drawer State
  shiftDrawerOpen = false;
  editingShift: ShiftResponse | null = null;

  // Seats tab
  seatsScheduleId: string | null = null;
  seatsShiftId: string | null = null;
  seatsShifts: ShiftResponse[] = [];
  allocations: SeatAllocationResponse[] = [];
  loadingAllocations = false;
  allocColumns = ['centreId', 'totalSeats', 'availableSeats', 'pwdSeats', 'emergencyBufferSeats', 'allocActions'];

  // Seat Allocation Drawer State
  allocDrawerOpen = false;
  editingAllocation: SeatAllocationResponse | null = null;

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
    private router: Router,
    private examService: ExamManagementService,
    private schedulingService: SchedulingService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const rawId = this.route.snapshot.paramMap.get('examId') || '';
    const uuidPattern = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
    if (!rawId || !uuidPattern.test(rawId)) {
      this.router.navigate(['/exam/scheduling']);
      return;
    }
    this.examId = rawId;
    this.examService.getExam(this.examId).subscribe({
      next: (e) => { if (e) this.examName = e.name; },
      error: () => {}
    });
    this.loadSchedules();
  }

  // ── Schedules ─────────────────────────────────────────────────────────────

  loadSchedules(): void {
    this.loadingSchedules = true;
    this.schedulingService.listSchedules(this.examId).subscribe({
      next: (data) => {
        this.schedules = data ?? [];
        this.loadingSchedules = false;
        // Keep selected schedule id valid
        if (this.selectedScheduleId && !this.schedules.some(s => s.id === this.selectedScheduleId)) {
          this.selectedScheduleId = this.schedules[0]?.id || null;
        } else if (!this.selectedScheduleId && this.schedules.length > 0) {
          this.selectedScheduleId = this.schedules[0].id;
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load schedules:', err);
        this.schedules = [];
        this.loadingSchedules = false;
        this.cdr.detectChanges();
      }
    });
  }

  openCreateSchedule(): void {
    this.editingSchedule = null;
    this.scheduleDrawerOpen = true;
  }

  openEditSchedule(schedule: ScheduleResponse): void {
    this.editingSchedule = schedule;
    this.scheduleDrawerOpen = true;
  }

  onScheduleSaved(savedSchedule: ScheduleResponse): void {
    this.scheduleDrawerOpen = false;
    this.editingSchedule = null;
    this.snackBar.open('Schedule saved successfully', 'OK', { duration: 3000 });
    this.loadSchedules();
  }

  onScheduleDrawerClose(): void {
    this.scheduleDrawerOpen = false;
    this.editingSchedule = null;
  }

  openTransition(schedule: ScheduleResponse): void {
    const targets = this.nextStatusMap[schedule.status] || [];
    if (targets.length === 0) return;
    const target = targets[0];
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
        next: () => {
          this.snackBar.open(`Transitioned to ${target}`, 'OK', { duration: 3000 });
          this.loadSchedules();
        },
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
        next: () => {
          this.snackBar.open('Schedule cancelled', 'OK', { duration: 3000 });
          this.loadSchedules();
        },
        error: (e) => this.snackBar.open(e?.error?.message || 'Error cancelling schedule', 'Dismiss', { duration: 4000 })
      });
    });
  }

  openAmend(schedule: ScheduleResponse): void {
    const ref = this.dialog.open(AmendScheduleDialogComponent, { width: '600px', data: { schedule } });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.schedulingService.amendSchedule(this.examId, schedule.id, result).subscribe({
        next: () => {
          this.snackBar.open('Amendment created', 'OK', { duration: 3000 });
          this.loadSchedules();
        },
        error: (e) => this.snackBar.open(e?.error?.message || 'Error creating amendment', 'Dismiss', { duration: 4000 })
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
      next: (data) => {
        this.shifts = data ?? [];
        this.loadingShifts = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load shifts:', err);
        this.shifts = [];
        this.loadingShifts = false;
        this.cdr.detectChanges();
      }
    });
  }

  openAddShift(): void {
    this.editingShift = null;
    this.shiftDrawerOpen = true;
  }

  openEditShift(shift: ShiftResponse): void {
    this.editingShift = shift;
    this.shiftDrawerOpen = true;
  }

  onShiftSaved(savedShift: ShiftResponse): void {
    this.shiftDrawerOpen = false;
    this.editingShift = null;
    this.snackBar.open(this.editingShift ? 'Shift updated successfully' : 'Shift added successfully', 'OK', { duration: 3000 });
    this.loadShifts();
  }

  onShiftDrawerClose(): void {
    this.shiftDrawerOpen = false;
    this.editingShift = null;
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
      next: (data) => {
        this.seatsShifts = data ?? [];
        this.seatsShiftId = null;
        this.allocations = [];
        this.cdr.detectChanges();
      },
      error: () => {
        this.seatsShifts = [];
        this.cdr.detectChanges();
      }
    });
  }

  loadAllocations(): void {
    if (!this.seatsScheduleId || !this.seatsShiftId) return;
    this.loadingAllocations = true;
    this.schedulingService.listAllocations(this.examId, this.seatsScheduleId, this.seatsShiftId).subscribe({
      next: (data) => {
        this.allocations = data ?? [];
        this.loadingAllocations = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load allocations:', err);
        this.allocations = [];
        this.loadingAllocations = false;
        this.cdr.detectChanges();
      }
    });
  }

  openAddAllocation(): void {
    this.editingAllocation = null;
    this.allocDrawerOpen = true;
  }

  openEditAllocation(alloc: SeatAllocationResponse): void {
    this.editingAllocation = alloc;
    this.allocDrawerOpen = true;
  }

  onAllocationSaved(savedAlloc: SeatAllocationResponse): void {
    this.allocDrawerOpen = false;
    this.editingAllocation = null;
    this.snackBar.open('Seat allocation saved successfully', 'OK', { duration: 3000 });
    this.loadAllocations();
  }

  onAllocationDrawerClose(): void {
    this.allocDrawerOpen = false;
    this.editingAllocation = null;
  }
}
