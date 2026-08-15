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
  template: `
    <div class="page-container">
      <!-- Header -->
      <div class="page-header">
        <div>
          <h1 class="page-title">
            <mat-icon class="title-icon">event</mat-icon>
            {{ examName || 'Examination Schedules' }}
          </h1>
          <p class="page-subtitle" *ngIf="examName">Manage schedules, shifts, and seat allocations.</p>
        </div>
      </div>

      <mat-card>
        <mat-card-content>
          <mat-tab-group [(selectedIndex)]="activeTab" animationDuration="150ms">

            <!-- ═══ TAB 1: Schedules ═══ -->
            <mat-tab label="Schedules">
              <div class="tab-content">
                <div class="tab-toolbar">
                  <span class="tab-toolbar-title">Schedule Versions</span>
                  <button mat-raised-button color="primary" (click)="openCreateSchedule()">
                    <mat-icon>add</mat-icon> New Schedule
                  </button>
                </div>

                <div class="loading" *ngIf="loadingSchedules"><mat-spinner diameter="32"></mat-spinner></div>
                <div class="empty" *ngIf="!loadingSchedules && schedules.length === 0">
                  <mat-icon>event_busy</mat-icon><p>No schedules created yet.</p>
                </div>

                <table mat-table [dataSource]="schedules" *ngIf="!loadingSchedules && schedules.length > 0" class="full-width">
                  <ng-container matColumnDef="version">
                    <th mat-header-cell *matHeaderCellDef>Ver</th>
                    <td mat-cell *matCellDef="let row">v{{ row.scheduleVersion }}</td>
                  </ng-container>
                  <ng-container matColumnDef="scheduleName">
                    <th mat-header-cell *matHeaderCellDef>Name</th>
                    <td mat-cell *matCellDef="let row">{{ row.scheduleName }}</td>
                  </ng-container>
                  <ng-container matColumnDef="examDate">
                    <th mat-header-cell *matHeaderCellDef>Exam Date</th>
                    <td mat-cell *matCellDef="let row">{{ row.examDate }}</td>
                  </ng-container>
                  <ng-container matColumnDef="reserveDate">
                    <th mat-header-cell *matHeaderCellDef>Reserve Date</th>
                    <td mat-cell *matCellDef="let row">{{ row.reserveDate || '—' }}</td>
                  </ng-container>
                  <ng-container matColumnDef="notificationNumber">
                    <th mat-header-cell *matHeaderCellDef>Notification #</th>
                    <td mat-cell *matCellDef="let row">{{ row.notificationNumber || '—' }}</td>
                  </ng-container>
                  <ng-container matColumnDef="status">
                    <th mat-header-cell *matHeaderCellDef>Status</th>
                    <td mat-cell *matCellDef="let row">
                      <mat-chip-set><mat-chip [class]="'status-' + (row.status||'').toLowerCase()">{{ row.status }}</mat-chip></mat-chip-set>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="actions">
                    <th mat-header-cell *matHeaderCellDef>Actions</th>
                    <td mat-cell *matCellDef="let row">
                      <button mat-icon-button matTooltip="Edit" (click)="openEditSchedule(row)"
                              *ngIf="row.status === 'DRAFT'"
                              aria-label="Edit schedule">
                        <mat-icon>edit</mat-icon>
                      </button>
                      <button mat-icon-button matTooltip="Transition" (click)="openTransition(row)"
                              *ngIf="row.status !== 'CANCELLED' && row.status !== 'PUBLISHED'"
                              aria-label="Transition schedule">
                        <mat-icon>arrow_forward</mat-icon>
                      </button>
                      <button mat-icon-button matTooltip="Amend" color="accent" (click)="openAmend(row)"
                              *ngIf="row.status === 'PUBLISHED'" aria-label="Amend schedule">
                        <mat-icon>edit_note</mat-icon>
                      </button>
                      <button mat-icon-button matTooltip="Cancel" color="warn" (click)="cancelSchedule(row)"
                              *ngIf="row.status !== 'CANCELLED'" aria-label="Cancel schedule">
                        <mat-icon>cancel</mat-icon>
                      </button>
                      <button mat-icon-button matTooltip="View Shifts" (click)="selectScheduleForShifts(row)"
                              aria-label="View shifts">
                        <mat-icon>schedule</mat-icon>
                      </button>
                    </td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="scheduleColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: scheduleColumns;"></tr>
                </table>
              </div>
            </mat-tab>

            <!-- ═══ TAB 2: Shifts ═══ -->
            <mat-tab label="Shifts">
              <div class="tab-content">
                <div class="tab-toolbar">
                  <mat-form-field appearance="outline" class="schedule-select">
                    <mat-label>Select Schedule</mat-label>
                    <mat-select [(ngModel)]="selectedScheduleId" (ngModelChange)="loadShifts()">
                      <mat-option *ngFor="let s of schedules" [value]="s.id">
                        v{{ s.scheduleVersion }} — {{ s.scheduleName }} ({{ s.status }})
                      </mat-option>
                    </mat-select>
                  </mat-form-field>
                  <button mat-raised-button color="primary" [disabled]="!selectedScheduleId" (click)="openAddShift()">
                    <mat-icon>add</mat-icon> Add Shift
                  </button>
                </div>

                <div class="empty" *ngIf="!selectedScheduleId">
                  <mat-icon>info</mat-icon><p>Select a schedule above to view its shifts.</p>
                </div>

                <div class="loading" *ngIf="loadingShifts"><mat-spinner diameter="32"></mat-spinner></div>

                <table mat-table [dataSource]="shifts" *ngIf="selectedScheduleId && !loadingShifts && shifts.length > 0" class="full-width">
                  <ng-container matColumnDef="shiftNumber">
                    <th mat-header-cell *matHeaderCellDef>#</th>
                    <td mat-cell *matCellDef="let row">{{ row.shiftNumber }}</td>
                  </ng-container>
                  <ng-container matColumnDef="shiftName">
                    <th mat-header-cell *matHeaderCellDef>Name</th>
                    <td mat-cell *matCellDef="let row">{{ row.shiftName || '—' }}</td>
                  </ng-container>
                  <ng-container matColumnDef="reportingTime">
                    <th mat-header-cell *matHeaderCellDef>Reporting</th>
                    <td mat-cell *matCellDef="let row">{{ row.reportingTime }}</td>
                  </ng-container>
                  <ng-container matColumnDef="gateClosingTime">
                    <th mat-header-cell *matHeaderCellDef>Gate Close</th>
                    <td mat-cell *matCellDef="let row">{{ row.gateClosingTime }}</td>
                  </ng-container>
                  <ng-container matColumnDef="examStartTime">
                    <th mat-header-cell *matHeaderCellDef>Start</th>
                    <td mat-cell *matCellDef="let row">{{ row.examStartTime }}</td>
                  </ng-container>
                  <ng-container matColumnDef="examEndTime">
                    <th mat-header-cell *matHeaderCellDef>End</th>
                    <td mat-cell *matCellDef="let row">{{ row.examEndTime }}</td>
                  </ng-container>
                  <ng-container matColumnDef="durationMinutes">
                    <th mat-header-cell *matHeaderCellDef>Duration</th>
                    <td mat-cell *matCellDef="let row">{{ row.durationMinutes }} min</td>
                  </ng-container>
                  <ng-container matColumnDef="shiftActions">
                    <th mat-header-cell *matHeaderCellDef>Actions</th>
                    <td mat-cell *matCellDef="let row">
                      <button mat-icon-button matTooltip="Edit" (click)="openEditShift(row)" aria-label="Edit shift">
                        <mat-icon>edit</mat-icon>
                      </button>
                      <button mat-icon-button matTooltip="Seat Allocations" (click)="selectShiftForSeats(row)" aria-label="View seats">
                        <mat-icon>event_seat</mat-icon>
                      </button>
                    </td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="shiftColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: shiftColumns;"></tr>
                </table>

                <div class="empty" *ngIf="selectedScheduleId && !loadingShifts && shifts.length === 0">
                  <mat-icon>schedule</mat-icon><p>No shifts configured for this schedule.</p>
                </div>
              </div>
            </mat-tab>

            <!-- ═══ TAB 3: Centres & Seats ═══ -->
            <mat-tab label="Centres & Seats">
              <div class="tab-content">
                <div class="tab-toolbar">
                  <mat-form-field appearance="outline" class="schedule-select">
                    <mat-label>Schedule</mat-label>
                    <mat-select [(ngModel)]="seatsScheduleId" (ngModelChange)="loadShiftsForSeats()">
                      <mat-option *ngFor="let s of schedules" [value]="s.id">
                        v{{ s.scheduleVersion }} — {{ s.scheduleName }}
                      </mat-option>
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field appearance="outline" class="schedule-select">
                    <mat-label>Shift</mat-label>
                    <mat-select [(ngModel)]="seatsShiftId" [disabled]="!seatsScheduleId" (ngModelChange)="loadAllocations()">
                      <mat-option *ngFor="let sh of seatsShifts" [value]="sh.id">
                        #{{ sh.shiftNumber }} {{ sh.shiftName || '' }} ({{ sh.examStartTime }}–{{ sh.examEndTime }})
                      </mat-option>
                    </mat-select>
                  </mat-form-field>
                  <button mat-raised-button color="primary" [disabled]="!seatsShiftId" (click)="openAddAllocation()">
                    <mat-icon>add</mat-icon> Add Allocation
                  </button>
                </div>

                <div class="empty" *ngIf="!seatsShiftId">
                  <mat-icon>info</mat-icon><p>Select a schedule and shift to view seat allocations.</p>
                </div>

                <div class="loading" *ngIf="loadingAllocations"><mat-spinner diameter="32"></mat-spinner></div>

                <table mat-table [dataSource]="allocations" *ngIf="seatsShiftId && !loadingAllocations && allocations.length > 0" class="full-width">
                  <ng-container matColumnDef="centreId">
                    <th mat-header-cell *matHeaderCellDef>Centre ID</th>
                    <td mat-cell *matCellDef="let row">{{ row.centreId | slice:0:8 }}…</td>
                  </ng-container>
                  <ng-container matColumnDef="totalSeats">
                    <th mat-header-cell *matHeaderCellDef>Total</th>
                    <td mat-cell *matCellDef="let row">{{ row.totalSeats }}</td>
                  </ng-container>
                  <ng-container matColumnDef="availableSeats">
                    <th mat-header-cell *matHeaderCellDef>Available</th>
                    <td mat-cell *matCellDef="let row">{{ row.availableSeats }}</td>
                  </ng-container>
                  <ng-container matColumnDef="pwdSeats">
                    <th mat-header-cell *matHeaderCellDef>PwD</th>
                    <td mat-cell *matCellDef="let row">{{ row.pwdSeats }}</td>
                  </ng-container>
                  <ng-container matColumnDef="emergencyBufferSeats">
                    <th mat-header-cell *matHeaderCellDef>Buffer</th>
                    <td mat-cell *matCellDef="let row">{{ row.emergencyBufferSeats }}</td>
                  </ng-container>
                  <ng-container matColumnDef="allocActions">
                    <th mat-header-cell *matHeaderCellDef>Actions</th>
                    <td mat-cell *matCellDef="let row">
                      <button mat-icon-button matTooltip="Edit Allocation" (click)="openEditAllocation(row)" aria-label="Edit allocation">
                        <mat-icon>edit</mat-icon>
                      </button>
                    </td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="allocColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: allocColumns;"></tr>
                </table>

                <div class="empty" *ngIf="seatsShiftId && !loadingAllocations && allocations.length === 0">
                  <mat-icon>event_seat</mat-icon><p>No seat allocations for this shift yet.</p>
                </div>
              </div>
            </mat-tab>

          </mat-tab-group>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; max-width: 1400px; margin: 0 auto; }
    .page-header { margin-bottom: 20px; }
    .page-title { margin: 0; font-size: 24px; font-weight: 600; display: flex; align-items: center; gap: 10px; }
    .title-icon { font-size: 28px; height: 28px; width: 28px; color: #1976d2; }
    .page-subtitle { margin: 4px 0 0; font-size: 14px; color: #757575; }
    .tab-content { padding: 16px 0; }
    .tab-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
    .tab-toolbar-title { font-size: 16px; font-weight: 600; flex: 1; }
    .schedule-select { min-width: 240px; margin-bottom: -1.25em; }
    .full-width { width: 100%; }
    .loading, .empty { display: flex; flex-direction: column; align-items: center; padding: 32px; color: #9e9e9e; }
    .empty mat-icon { font-size: 40px; height: 40px; width: 40px; margin-bottom: 8px; }
    ::ng-deep .status-draft { background: #fff3e0 !important; color: #e65100 !important; }
    ::ng-deep .status-scheduler_review { background: #e3f2fd !important; color: #1565c0 !important; }
    ::ng-deep .status-controller_approved { background: #e0f7fa !important; color: #00695c !important; }
    ::ng-deep .status-security_review { background: #f3e5f5 !important; color: #6a1b9a !important; }
    ::ng-deep .status-chairman_approved { background: #e8f5e9 !important; color: #2e7d32 !important; }
    ::ng-deep .status-published { background: #e8f5e9 !important; color: #1b5e20 !important; }
    ::ng-deep .status-cancelled { background: #ffebee !important; color: #b71c1c !important; }
  `]
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
