import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCardModule } from '@angular/material/card';

import { ExamManagementService, ExaminationResponse } from '../exam-manage/exam-management.service';
import {
  SchedulingService,
  ScheduleResponse,
  ShiftResponse,
  CentreResponse,
  SeatAllocationResponse
} from './scheduling.service';
import { ScheduleFormDialogComponent } from './schedule-form-dialog.component';
import { AmendScheduleDialogComponent } from './amend-schedule-dialog.component';
import { TransitionDialogComponent } from './transition-dialog.component';
import { ShiftFormDialogComponent } from './shift-form-dialog.component';
import { SeatAllocationDialogComponent } from './seat-allocation-dialog.component';

@Component({
  selector: 'app-schedule-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatSelectModule,
    MatFormFieldModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatCardModule
  ],
  template: `
    <div class="page-container">
      <!-- Breadcrumb / Header -->
      <div class="header-section">
        <div class="breadcrumb">
          <a routerLink="/exam/scheduling" class="back-link">
            <mat-icon>arrow_back</mat-icon> Examinations
          </a>
          <span class="separator">/</span>
          <span class="current-exam">{{ exam?.name || 'Loading exam...' }}</span>
        </div>
        <h2>{{ exam?.name }} — Schedule Management</h2>
      </div>

      <!-- Main Tabs -->
      <mat-tab-group
        [(selectedIndex)]="selectedTabIndex"
        (selectedIndexChange)="onTabChange($event)"
        class="custom-tabs"
      >

        <!-- TAB 1: SCHEDULES -->
        <mat-tab label="Schedules">
          <ng-template matTabContent>
            <div class="tab-content">
              <div class="tab-toolbar">
                <h3>Schedule Versions</h3>
                <button mat-raised-button color="primary" (click)="openCreateScheduleDialog()">
                  <mat-icon>add</mat-icon> New Schedule
                </button>
              </div>

              <table mat-table [dataSource]="schedules" class="mat-elevation-z1 custom-table">
                <ng-container matColumnDef="version">
                  <th mat-header-cell *matHeaderCellDef>Ver</th>
                  <td mat-cell *matCellDef="let s">v{{ s.scheduleVersion }}</td>
                </ng-container>

                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef>Schedule Name</th>
                  <td mat-cell *matCellDef="let s">
                    <strong>{{ s.scheduleName }}</strong>
                  </td>
                </ng-container>

                <ng-container matColumnDef="examDate">
                  <th mat-header-cell *matHeaderCellDef>Exam Date</th>
                  <td mat-cell *matCellDef="let s">{{ s.examDate | date:'mediumDate' }}</td>
                </ng-container>

                <ng-container matColumnDef="reserveDate">
                  <th mat-header-cell *matHeaderCellDef>Reserve Date</th>
                  <td mat-cell *matCellDef="let s">{{ (s.reserveDate | date:'mediumDate') || '-' }}</td>
                </ng-container>

                <ng-container matColumnDef="notification">
                  <th mat-header-cell *matHeaderCellDef>Notification #</th>
                  <td mat-cell *matCellDef="let s">{{ s.notificationNumber || '-' }}</td>
                </ng-container>

                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef>Status</th>
                  <td mat-cell *matCellDef="let s">
                    <span class="status-chip" [ngClass]="getStatusChipClass(s.status)">
                      {{ s.status }}
                    </span>
                  </td>
                </ng-container>

                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef>Actions</th>
                  <td mat-cell *matCellDef="let s">
                    <div class="action-buttons">
                      <button
                        mat-stroked-button
                        color="primary"
                        (click)="openTransitionDialog(s)"
                        [disabled]="s.status === 'CANCELLED'"
                        matTooltip="Transition schedule status"
                      >
                        Transition
                      </button>

                      <button
                        *ngIf="s.status === 'PUBLISHED'"
                        mat-stroked-button
                        color="warn"
                        (click)="openAmendDialog(s)"
                        matTooltip="Amend published schedule"
                      >
                        Amend
                      </button>

                      <button
                        mat-icon-button
                        color="accent"
                        (click)="selectScheduleForShifts(s)"
                        matTooltip="View shifts for this schedule"
                      >
                        <mat-icon>schedule</mat-icon>
                      </button>
                    </div>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="scheduleColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: scheduleColumns;"></tr>

                <tr class="mat-row" *matNoDataRow>
                  <td class="mat-cell empty-cell" [attr.colspan]="scheduleColumns.length">
                    No schedules created yet for this examination.
                  </td>
                </tr>
              </table>
            </div>
          </ng-template>
        </mat-tab>

        <!-- TAB 2: SHIFTS -->
        <mat-tab label="Shifts">
          <ng-template matTabContent>
            <div class="tab-content">
              <div class="selector-bar">
                <mat-form-field appearance="outline" class="selector-field">
                  <mat-label>Select Schedule Version</mat-label>
                  <mat-select [(ngModel)]="selectedScheduleId" (ngModelChange)="onScheduleSelectChange()">
                    <mat-option *ngFor="let s of schedules" [value]="s.id">
                      v{{ s.scheduleVersion }} - {{ s.scheduleName }} ({{ s.status }})
                    </mat-option>
                  </mat-select>
                </mat-form-field>

                <button
                  mat-raised-button
                  color="primary"
                  [disabled]="!selectedScheduleId"
                  (click)="openAddShiftDialog()"
                >
                  <mat-icon>add</mat-icon> Add Shift
                </button>
              </div>

              <div *ngIf="!selectedScheduleId" class="prompt-box">
                <mat-icon>info</mat-icon>
                <p>Please select a schedule version above to view and manage shifts.</p>
              </div>

              <table
                *ngIf="selectedScheduleId"
                mat-table
                [dataSource]="shifts"
                class="mat-elevation-z1 custom-table"
              >
                <ng-container matColumnDef="shiftNumber">
                  <th mat-header-cell *matHeaderCellDef>Shift #</th>
                  <td mat-cell *matCellDef="let sh">Shift {{ sh.shiftNumber }}</td>
                </ng-container>

                <ng-container matColumnDef="shiftName">
                  <th mat-header-cell *matHeaderCellDef>Shift Name</th>
                  <td mat-cell *matCellDef="let sh">{{ sh.shiftName || '-' }}</td>
                </ng-container>

                <ng-container matColumnDef="reportingTime">
                  <th mat-header-cell *matHeaderCellDef>Reporting</th>
                  <td mat-cell *matCellDef="let sh">{{ sh.reportingTime }}</td>
                </ng-container>

                <ng-container matColumnDef="gateClosingTime">
                  <th mat-header-cell *matHeaderCellDef>Gate Close</th>
                  <td mat-cell *matCellDef="let sh">{{ sh.gateClosingTime }}</td>
                </ng-container>

                <ng-container matColumnDef="loginStartTime">
                  <th mat-header-cell *matHeaderCellDef>Login</th>
                  <td mat-cell *matCellDef="let sh">{{ sh.loginStartTime }}</td>
                </ng-container>

                <ng-container matColumnDef="examStartTime">
                  <th mat-header-cell *matHeaderCellDef>Exam Start</th>
                  <td mat-cell *matCellDef="let sh">{{ sh.examStartTime }}</td>
                </ng-container>

                <ng-container matColumnDef="examEndTime">
                  <th mat-header-cell *matHeaderCellDef>Exam End</th>
                  <td mat-cell *matCellDef="let sh">{{ sh.examEndTime }}</td>
                </ng-container>

                <ng-container matColumnDef="durationMinutes">
                  <th mat-header-cell *matHeaderCellDef>Duration</th>
                  <td mat-cell *matCellDef="let sh">{{ sh.durationMinutes }} mins</td>
                </ng-container>

                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef>Actions</th>
                  <td mat-cell *matCellDef="let sh">
                    <div class="action-buttons">
                      <button
                        mat-icon-button
                        color="primary"
                        (click)="openEditShiftDialog(sh)"
                        matTooltip="Edit shift"
                      >
                        <mat-icon>edit</mat-icon>
                      </button>
                      <button
                        mat-stroked-button
                        color="accent"
                        (click)="selectShiftForSeats(sh)"
                        matTooltip="View seat allocations"
                      >
                        View Seats
                      </button>
                    </div>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="shiftColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: shiftColumns;"></tr>

                <tr class="mat-row" *matNoDataRow>
                  <td class="mat-cell empty-cell" [attr.colspan]="shiftColumns.length">
                    No shifts defined for this schedule. Click "+ Add Shift" to add one.
                  </td>
                </tr>
              </table>
            </div>
          </ng-template>
        </mat-tab>

        <!-- TAB 3: CENTRES & SEATS -->
        <mat-tab label="Centres & Seats">
          <ng-template matTabContent>
            <div class="tab-content">
              <div class="selector-bar">
                <mat-form-field appearance="outline" class="selector-field">
                  <mat-label>Schedule Version</mat-label>
                  <mat-select [(ngModel)]="selectedScheduleId" (ngModelChange)="onScheduleSelectChange()">
                    <mat-option *ngFor="let s of schedules" [value]="s.id">
                      v{{ s.scheduleVersion }} - {{ s.scheduleName }}
                    </mat-option>
                  </mat-select>
                </mat-form-field>

                <mat-form-field appearance="outline" class="selector-field">
                  <mat-label>Shift</mat-label>
                  <mat-select [(ngModel)]="selectedShiftId" (ngModelChange)="onShiftSelectChange()">
                    <mat-option *ngFor="let sh of shifts" [value]="sh.id">
                      Shift {{ sh.shiftNumber }} ({{ sh.shiftName || sh.examStartTime }})
                    </mat-option>
                  </mat-select>
                </mat-form-field>

                <button
                  mat-raised-button
                  color="primary"
                  [disabled]="!selectedScheduleId || !selectedShiftId"
                  (click)="openAddAllocationDialog()"
                >
                  <mat-icon>add</mat-icon> Add Allocation
                </button>
              </div>

              <div *ngIf="!selectedScheduleId || !selectedShiftId" class="prompt-box">
                <mat-icon>info</mat-icon>
                <p>Please select a schedule version and a shift above to view and manage seat allocations.</p>
              </div>

              <table
                *ngIf="selectedScheduleId && selectedShiftId"
                mat-table
                [dataSource]="allocations"
                class="mat-elevation-z1 custom-table"
              >
                <ng-container matColumnDef="centreName">
                  <th mat-header-cell *matHeaderCellDef>Centre Name</th>
                  <td mat-cell *matCellDef="let a">
                    <strong>{{ getCentreName(a.centreId) }}</strong>
                  </td>
                </ng-container>

                <ng-container matColumnDef="city">
                  <th mat-header-cell *matHeaderCellDef>City</th>
                  <td mat-cell *matCellDef="let a">{{ getCentreCity(a.centreId) }}</td>
                </ng-container>

                <ng-container matColumnDef="state">
                  <th mat-header-cell *matHeaderCellDef>State</th>
                  <td mat-cell *matCellDef="let a">{{ getCentreState(a.centreId) }}</td>
                </ng-container>

                <ng-container matColumnDef="totalSeats">
                  <th mat-header-cell *matHeaderCellDef>Total Seats</th>
                  <td mat-cell *matCellDef="let a">{{ a.totalSeats }}</td>
                </ng-container>

                <ng-container matColumnDef="availableSeats">
                  <th mat-header-cell *matHeaderCellDef>Available</th>
                  <td mat-cell *matCellDef="let a">
                    <span [class.text-warn]="a.availableSeats <= 0">{{ a.availableSeats }}</span>
                  </td>
                </ng-container>

                <ng-container matColumnDef="pwdSeats">
                  <th mat-header-cell *matHeaderCellDef>PwD</th>
                  <td mat-cell *matCellDef="let a">{{ a.pwdSeats }}</td>
                </ng-container>

                <ng-container matColumnDef="bufferSeats">
                  <th mat-header-cell *matHeaderCellDef>Buffer</th>
                  <td mat-cell *matCellDef="let a">{{ a.emergencyBufferSeats }}</td>
                </ng-container>

                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef>Actions</th>
                  <td mat-cell *matCellDef="let a">
                    <button
                      mat-icon-button
                      color="primary"
                      (click)="openEditAllocationDialog(a)"
                      matTooltip="Edit seat allocation"
                    >
                      <mat-icon>edit</mat-icon>
                    </button>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="allocationColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: allocationColumns;"></tr>

                <tr class="mat-row" *matNoDataRow>
                  <td class="mat-cell empty-cell" [attr.colspan]="allocationColumns.length">
                    No seat allocations set for this shift. Click "+ Add Allocation" to allocate seats.
                  </td>
                </tr>
              </table>
            </div>
          </ng-template>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .page-container {
      padding: 24px;
      max-width: 1400px;
      margin: 0 auto;
    }
    .header-section {
      margin-bottom: 16px;
    }
    .breadcrumb {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      color: #666;
      margin-bottom: 8px;
    }
    .back-link {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: #1976d2;
      text-decoration: none;
      font-weight: 500;
    }
    .back-link:hover {
      text-decoration: underline;
    }
    .separator {
      color: #999;
    }
    .current-exam {
      font-weight: 600;
      color: #333;
    }
    .header-section h2 {
      margin: 0;
      font-size: 22px;
      font-weight: 600;
    }

    .tab-content {
      padding: 20px 0;
    }
    .tab-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    .tab-toolbar h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
    }

    .selector-bar {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 16px;
      flex-wrap: wrap;
    }
    .selector-field {
      min-width: 280px;
      margin-bottom: -1.25em;
    }

    .prompt-box {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      background: #e3f2fd;
      border-radius: 8px;
      color: #0d47a1;
      margin-top: 16px;
    }
    .prompt-box p {
      margin: 0;
    }

    .custom-table {
      width: 100%;
      background: white;
      border-radius: 8px;
      overflow: hidden;
    }
    .action-buttons {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .empty-cell {
      padding: 32px;
      text-align: center;
      color: #888;
    }
    .text-warn {
      color: #d32f2f;
      font-weight: 600;
    }

    .status-chip {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
      text-transform: uppercase;
    }

    ::ng-deep .status-draft              { background: #fff3e0 !important; color: #e65100 !important; }
    ::ng-deep .status-scheduler_review   { background: #e3f2fd !important; color: #1565c0 !important; }
    ::ng-deep .status-controller_approved{ background: #e0f7fa !important; color: #00695c !important; }
    ::ng-deep .status-security_review    { background: #f3e5f5 !important; color: #6a1b9a !important; }
    ::ng-deep .status-chairman_approved  { background: #e8f5e9 !important; color: #2e7d32 !important; }
    ::ng-deep .status-published          { background: #e8f5e9 !important; color: #1b5e20 !important; }
    ::ng-deep .status-cancelled          { background: #ffebee !important; color: #b71c1c !important; }
  `]
})
export class ScheduleDetailComponent implements OnInit {
  examId!: string;
  exam?: ExaminationResponse;

  selectedTabIndex = 0;
  schedules: ScheduleResponse[] = [];
  shifts: ShiftResponse[] = [];
  centres: CentreResponse[] = [];
  allocations: SeatAllocationResponse[] = [];

  selectedScheduleId: string | null = null;
  selectedShiftId: string | null = null;

  scheduleColumns = ['version', 'name', 'examDate', 'reserveDate', 'notification', 'status', 'actions'];
  shiftColumns = ['shiftNumber', 'shiftName', 'reportingTime', 'gateClosingTime', 'loginStartTime', 'examStartTime', 'examEndTime', 'durationMinutes', 'actions'];
  allocationColumns = ['centreName', 'city', 'state', 'totalSeats', 'availableSeats', 'pwdSeats', 'bufferSeats', 'actions'];

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
    this.examId = this.route.snapshot.paramMap.get('examId') || '';
    if (this.examId) {
      this.loadExam();
      this.loadSchedules();
      this.loadCentres();
    }
  }

  onTabChange(index: number): void {
    this.selectedTabIndex = index;
    if (index === 1 && this.selectedScheduleId) {
      this.loadShifts();
    } else if (index === 2) {
      if (this.selectedScheduleId && !this.shifts.length) {
        this.loadShifts();
      }
      if (this.selectedScheduleId && this.selectedShiftId) {
        this.loadAllocations();
      }
    }
    this.cdr.detectChanges();
  }

  loadExam(): void {
    this.examService.getExam(this.examId).subscribe({
      next: res => {
        this.exam = res;
        this.cdr.detectChanges();
      },
      error: err => this.showError('Failed to load examination details')
    });
  }

  loadSchedules(): void {
    this.schedulingService.listSchedules(this.examId).subscribe({
      next: res => {
        this.schedules = res || [];
        if (!this.selectedScheduleId && this.schedules.length > 0) {
          this.selectedScheduleId = this.schedules[0].id;
          this.loadShifts();
        }
        this.cdr.detectChanges();
      },
      error: err => this.showError('Failed to load schedule versions')
    });
  }

  loadCentres(): void {
    this.schedulingService.listCentres().subscribe({
      next: res => {
        this.centres = res || [];
        this.cdr.detectChanges();
      },
      error: err => console.error('Failed to load centres', err)
    });
  }

  loadShifts(): void {
    if (!this.selectedScheduleId) {
      this.shifts = [];
      return;
    }
    this.schedulingService.listShifts(this.examId, this.selectedScheduleId).subscribe({
      next: res => {
        this.shifts = res || [];
        if (!this.selectedShiftId && this.shifts.length > 0) {
          this.selectedShiftId = this.shifts[0].id;
          this.loadAllocations();
        }
        this.cdr.detectChanges();
      },
      error: err => this.showError('Failed to load shifts')
    });
  }

  loadAllocations(): void {
    if (!this.selectedScheduleId || !this.selectedShiftId) {
      this.allocations = [];
      return;
    }
    this.schedulingService.listAllocations(this.examId, this.selectedScheduleId, this.selectedShiftId).subscribe({
      next: res => {
        this.allocations = res || [];
        this.cdr.detectChanges();
      },
      error: err => this.showError('Failed to load seat allocations')
    });
  }

  onScheduleSelectChange(): void {
    this.selectedShiftId = null;
    this.loadShifts();
  }

  onShiftSelectChange(): void {
    this.loadAllocations();
  }

  getStatusChipClass(status: string): string {
    return `status-${(status || '').toLowerCase()}`;
  }

  // ── Schedules Actions ──────────────────────────────────
  openCreateScheduleDialog(): void {
    const dialogRef = this.dialog.open(ScheduleFormDialogComponent, { width: '500px' });
    dialogRef.afterClosed().subscribe(req => {
      if (req) {
        this.schedulingService.createSchedule(this.examId, req).subscribe({
          next: () => {
            this.snackBar.open('Schedule created successfully', 'OK', { duration: 3000 });
            this.loadSchedules();
          },
          error: err => this.showError(err?.error?.message || 'Failed to create schedule')
        });
      }
    });
  }

  openTransitionDialog(schedule: ScheduleResponse): void {
    const dialogRef = this.dialog.open(TransitionDialogComponent, {
      width: '450px',
      data: schedule
    });
    dialogRef.afterClosed().subscribe(req => {
      if (req) {
        this.schedulingService.transitionSchedule(this.examId, schedule.id, req).subscribe({
          next: () => {
            this.snackBar.open(`Status updated to ${req.targetStatus}`, 'OK', { duration: 3000 });
            this.loadSchedules();
          },
          error: err => this.showError(err?.error?.message || 'Failed to transition schedule status')
        });
      }
    });
  }

  openAmendDialog(schedule: ScheduleResponse): void {
    const dialogRef = this.dialog.open(AmendScheduleDialogComponent, {
      width: '520px',
      data: schedule
    });
    dialogRef.afterClosed().subscribe(req => {
      if (req) {
        this.schedulingService.amendSchedule(this.examId, schedule.id, req).subscribe({
          next: () => {
            this.snackBar.open('Schedule amended successfully', 'OK', { duration: 3000 });
            this.loadSchedules();
          },
          error: err => this.showError(err?.error?.message || 'Failed to amend schedule')
        });
      }
    });
  }

  selectScheduleForShifts(schedule: ScheduleResponse): void {
    this.selectedScheduleId = schedule.id;
    this.selectedShiftId = null;
    this.loadShifts();
    this.selectedTabIndex = 1; // Switch to Shifts tab
  }

  // ── Shifts Actions ──────────────────────────────────────
  openAddShiftDialog(): void {
    if (!this.selectedScheduleId) return;
    const dialogRef = this.dialog.open(ShiftFormDialogComponent, { width: '520px' });
    dialogRef.afterClosed().subscribe(req => {
      if (req) {
        this.schedulingService.addShift(this.examId, this.selectedScheduleId!, req).subscribe({
          next: () => {
            this.snackBar.open('Shift added successfully', 'OK', { duration: 3000 });
            this.loadShifts();
          },
          error: err => this.showError(err?.error?.message || 'Failed to add shift')
        });
      }
    });
  }

  openEditShiftDialog(shift: ShiftResponse): void {
    if (!this.selectedScheduleId) return;
    const dialogRef = this.dialog.open(ShiftFormDialogComponent, {
      width: '520px',
      data: shift
    });
    dialogRef.afterClosed().subscribe(req => {
      if (req) {
        this.schedulingService.updateShift(this.examId, this.selectedScheduleId!, shift.id, req).subscribe({
          next: () => {
            this.snackBar.open('Shift updated successfully', 'OK', { duration: 3000 });
            this.loadShifts();
          },
          error: err => this.showError(err?.error?.message || 'Failed to update shift')
        });
      }
    });
  }

  selectShiftForSeats(shift: ShiftResponse): void {
    this.selectedShiftId = shift.id;
    this.loadAllocations();
    this.selectedTabIndex = 2; // Switch to Centres & Seats tab
  }

  // ── Allocations Actions ─────────────────────────────────
  openAddAllocationDialog(): void {
    if (!this.selectedScheduleId || !this.selectedShiftId) return;
    const dialogRef = this.dialog.open(SeatAllocationDialogComponent, {
      width: '500px',
      data: { centres: this.centres }
    });
    dialogRef.afterClosed().subscribe(req => {
      if (req) {
        this.schedulingService.upsertAllocation(this.examId, this.selectedScheduleId!, this.selectedShiftId!, req).subscribe({
          next: () => {
            this.snackBar.open('Seat allocation saved successfully', 'OK', { duration: 3000 });
            this.loadAllocations();
          },
          error: err => this.showError(err?.error?.message || 'Failed to save seat allocation')
        });
      }
    });
  }

  openEditAllocationDialog(allocation: SeatAllocationResponse): void {
    if (!this.selectedScheduleId || !this.selectedShiftId) return;
    const dialogRef = this.dialog.open(SeatAllocationDialogComponent, {
      width: '500px',
      data: { allocation, centres: this.centres }
    });
    dialogRef.afterClosed().subscribe(req => {
      if (req) {
        this.schedulingService.upsertAllocation(this.examId, this.selectedScheduleId!, this.selectedShiftId!, req).subscribe({
          next: () => {
            this.snackBar.open('Seat allocation updated successfully', 'OK', { duration: 3000 });
            this.loadAllocations();
          },
          error: err => this.showError(err?.error?.message || 'Failed to update seat allocation')
        });
      }
    });
  }

  // ── Helpers ─────────────────────────────────────────────
  getCentreName(centreId: string): string {
    const c = this.centres.find(item => item.id === centreId);
    return c ? c.centreName : centreId;
  }

  getCentreCity(centreId: string): string {
    const c = this.centres.find(item => item.id === centreId);
    return c ? c.city : '-';
  }

  getCentreState(centreId: string): string {
    const c = this.centres.find(item => item.id === centreId);
    return c ? c.state : '-';
  }

  private showError(msg: string): void {
    this.snackBar.open(msg, 'Dismiss', { duration: 4000 });
  }
}
