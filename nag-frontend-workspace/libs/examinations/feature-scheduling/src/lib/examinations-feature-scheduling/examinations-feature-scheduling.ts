import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  ExaminationService,
  SchedulingService,
  CentreManagementService,
  ExaminationResponse,
  ScheduleResponse,
  ShiftResponse,
  SeatAllocationResponse,
  CreateScheduleRequest,
  ScheduleTransitionRequest,
  AmendScheduleRequest,
  CreateShiftRequest,
  SeatAllocationRequest,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-examinations-feature-scheduling',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './examinations-feature-scheduling.component.html',
  styleUrl: './examinations-feature-scheduling.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExaminationsFeatureScheduling implements OnInit {
  private readonly examService = inject(ExaminationService);
  private readonly scheduleService = inject(SchedulingService);
  private readonly centreService = inject(CentreManagementService);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);

  // Active Navigation Mode: 'SCHEDULES' | 'SHIFTS' | 'ALLOCATIONS'
  readonly currentTab = signal<'SCHEDULES' | 'SHIFTS' | 'ALLOCATIONS'>('SCHEDULES');

  // Exam Selection
  readonly exams = this.examService.exams;
  readonly selectedExamId = signal<string>('');
  readonly selectedExam = computed(() =>
    this.exams().find((e) => e.id === this.selectedExamId()) || null
  );

  // Schedules State
  readonly schedules = this.scheduleService.schedules;
  readonly loading = this.scheduleService.loading;
  readonly selectedSchedule = signal<ScheduleResponse | null>(null);

  // Shifts State
  readonly shifts = this.scheduleService.shifts;
  readonly selectedShift = signal<ShiftResponse | null>(null);

  // Centres & Allocations State
  readonly centres = this.centreService.centres;
  readonly allocations = signal<SeatAllocationResponse[]>([]);
  readonly loadingAllocations = signal<boolean>(false);

  // Search Filter
  readonly searchQuery = signal<string>('');

  // Modals & Drawers
  readonly showCreateScheduleModal = signal<boolean>(false);
  readonly showTransitionModal = signal<boolean>(false);
  readonly showAmendModal = signal<boolean>(false);
  readonly showShiftModal = signal<boolean>(false);
  readonly showAllocationModal = signal<boolean>(false);

  // Create Schedule Form Fields
  newScheduleName = '';
  newNotificationNumber = '';
  newExamDate = '';
  newReserveDate = '';
  newTimeZone = 'Asia/Kolkata';

  // Transition Form Fields
  targetStatus = '';
  transitionComment = '';

  // Amend Form Fields
  amendReason = '';
  amendScheduleName = '';
  amendNotificationNumber = '';
  amendExamDate = '';
  amendReserveDate = '';
  amendEffectiveFrom = '';
  amendTimeZone = 'Asia/Kolkata';

  // Shift Form Fields
  editingShiftId: string | null = null;
  shiftNumber = 1;
  shiftName = 'Morning Session (Shift 1)';
  reportingTime = '07:30:00';
  gateClosingTime = '08:30:00';
  loginStartTime = '08:45:00';
  examStartTime = '09:00:00';
  examEndTime = '12:00:00';
  exitTime = '12:15:00';
  durationMinutes = 180;
  bufferMinutes = 30;

  // Seat Allocation Form Fields
  allocCentreId = '';
  allocTotalSeats = 200;
  allocAvailableSeats = 180;
  allocReservedSeats = 20;
  allocPwdSeats = 10;
  allocEmergencyBufferSeats = 10;
  allocFemaleReservedSeats = 0;
  allocSpecialCategorySeats = 0;

  // Workflow Map
  private readonly nextStatusMap: Record<string, string[]> = {
    DRAFT: ['SCHEDULER_REVIEW', 'CANCELLED'],
    SCHEDULER_REVIEW: ['CONTROLLER_APPROVED', 'CANCELLED'],
    CONTROLLER_APPROVED: ['SECURITY_REVIEW', 'CANCELLED'],
    SECURITY_REVIEW: ['CHAIRMAN_APPROVED', 'CANCELLED'],
    CHAIRMAN_APPROVED: ['PUBLISHED', 'CANCELLED'],
    PUBLISHED: ['CANCELLED'],
  };

  // Computed KPIs
  readonly totalSchedulesCount = computed(() => this.schedules().length);
  readonly publishedSchedulesCount = computed(
    () => this.schedules().filter((s) => s.status === 'PUBLISHED').length
  );
  readonly totalShiftsCount = computed(() => this.shifts().length);

  readonly filteredSchedules = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) return this.schedules();
    return this.schedules().filter(
      (s) =>
        s.scheduleName.toLowerCase().includes(q) ||
        (s.notificationNumber && s.notificationNumber.toLowerCase().includes(q)) ||
        s.examDate.includes(q)
    );
  });

  ngOnInit(): void {
    // Load exams list
    this.examService.getExams(0, 50).subscribe({
      next: (exams) => {
        this.route.queryParams.subscribe((params) => {
          const paramExamId = params['examId'];
          if (paramExamId && exams.some((e) => e.id === paramExamId)) {
            this.selectExam(paramExamId);
          } else if (exams.length > 0 && !this.selectedExamId()) {
            this.selectExam(exams[0].id);
          }
        });
      },
    });

    // Load centres for seat allocation
    this.centreService.listCentres(undefined, undefined, 0, 100).subscribe();
  }

  selectExam(examId: string): void {
    this.selectedExamId.set(examId);
    this.selectedSchedule.set(null);
    this.selectedShift.set(null);
    this.currentTab.set('SCHEDULES');
    this.loadSchedules();
  }

  loadSchedules(): void {
    const examId = this.selectedExamId();
    if (!examId) return;

    this.scheduleService.listSchedules(examId, 0, 50).subscribe({
      next: (list) => {
        if (list.length > 0 && !this.selectedSchedule()) {
          this.selectSchedule(list[0]);
        }
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to load schedules for examination',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  selectSchedule(schedule: ScheduleResponse): void {
    this.selectedSchedule.set(schedule);
    this.loadShifts(schedule.id);
  }

  loadShifts(scheduleId: string): void {
    const examId = this.selectedExamId();
    if (!examId || !scheduleId) return;

    this.scheduleService.listShifts(examId, scheduleId).subscribe({
      next: (shifts) => {
        if (shifts.length > 0 && !this.selectedShift()) {
          this.selectedShift.set(shifts[0]);
        }
      },
      error: () => {},
    });
  }

  openCreateSchedule(): void {
    const today = new Date();
    const nextMonth = new Date(today.getFullYear(), today.getMonth() + 1, 15);
    const reserve = new Date(today.getFullYear(), today.getMonth() + 1, 16);

    this.newScheduleName = `Session 1 - ${this.selectedExam()?.name || 'Examination'} 2026`;
    this.newNotificationNumber = `NAG-NOTIF-${Date.now().toString().slice(-6)}`;
    this.newExamDate = nextMonth.toISOString().split('T')[0];
    this.newReserveDate = reserve.toISOString().split('T')[0];
    this.newTimeZone = 'Asia/Kolkata';
    this.showCreateScheduleModal.set(true);
  }

  saveSchedule(): void {
    const examId = this.selectedExamId();
    if (!examId || !this.newScheduleName.trim() || !this.newExamDate) {
      this.snackBar.open('Schedule Name and Exam Date are required', 'Dismiss', {
        duration: 3000,
      });
      return;
    }

    const payload: CreateScheduleRequest = {
      scheduleName: this.newScheduleName.trim(),
      notificationNumber: this.newNotificationNumber.trim() || undefined,
      examDate: this.newExamDate,
      reserveDate: this.newReserveDate || undefined,
      timeZone: this.newTimeZone,
    };

    this.scheduleService.createSchedule(examId, payload).subscribe({
      next: (created) => {
        this.showCreateScheduleModal.set(false);
        this.selectSchedule(created);
        this.snackBar.open('Examination Schedule created successfully', 'OK', {
          duration: 3000,
        });
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to create schedule',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  // --- Approval Transition Workflow ---
  getNextStatuses(status: string): string[] {
    return this.nextStatusMap[status] || [];
  }

  openTransitionModal(schedule: ScheduleResponse, event?: Event): void {
    if (event) event.stopPropagation();
    this.selectedSchedule.set(schedule);
    const available = this.getNextStatuses(schedule.status);
    this.targetStatus = available[0] || '';
    this.transitionComment = '';
    this.showTransitionModal.set(true);
  }

  submitTransition(): void {
    const examId = this.selectedExamId();
    const sched = this.selectedSchedule();
    if (!examId || !sched || !this.targetStatus) return;

    const req: ScheduleTransitionRequest = {
      targetStatus: this.targetStatus,
      comment: this.transitionComment.trim() || undefined,
    };

    this.scheduleService.transitionSchedule(examId, sched.id, req).subscribe({
      next: (updated) => {
        this.showTransitionModal.set(false);
        this.snackBar.open(`Schedule transitioned to ${updated.status}`, 'OK', {
          duration: 3000,
        });
        this.loadSchedules();
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Transition rejected by DPI validation rules',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  // --- Amend Schedule ---
  openAmendModal(schedule: ScheduleResponse, event?: Event): void {
    if (event) event.stopPropagation();
    this.selectedSchedule.set(schedule);
    this.amendReason = '';
    this.amendScheduleName = schedule.scheduleName;
    this.amendNotificationNumber = schedule.notificationNumber || '';
    this.amendExamDate = schedule.examDate;
    this.amendReserveDate = schedule.reserveDate || '';
    this.amendEffectiveFrom = new Date().toISOString();
    this.amendTimeZone = schedule.timeZone || 'Asia/Kolkata';
    this.showAmendModal.set(true);
  }

  submitAmendment(): void {
    const examId = this.selectedExamId();
    const sched = this.selectedSchedule();
    if (!examId || !sched || !this.amendReason.trim()) {
      this.snackBar.open('Change Reason is required for schedule amendments', 'Dismiss', {
        duration: 3000,
      });
      return;
    }

    const req: AmendScheduleRequest = {
      changeReason: this.amendReason.trim(),
      scheduleName: this.amendScheduleName.trim(),
      notificationNumber: this.amendNotificationNumber.trim() || undefined,
      examDate: this.amendExamDate,
      reserveDate: this.amendReserveDate || undefined,
      effectiveFrom: this.amendEffectiveFrom || undefined,
      timeZone: this.amendTimeZone,
    };

    this.scheduleService.amendSchedule(examId, sched.id, req).subscribe({
      next: (newVersion) => {
        this.showAmendModal.set(false);
        this.selectSchedule(newVersion);
        this.snackBar.open(
          `Schedule amended! Created new Version ${newVersion.scheduleVersion}`,
          'OK',
          { duration: 3000 }
        );
        this.loadSchedules();
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to amend schedule',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  // --- Shift Management ---
  openAddShift(): void {
    this.editingShiftId = null;
    const nextNum = this.shifts().length + 1;
    this.shiftNumber = nextNum;
    this.shiftName = `Shift ${nextNum} (${nextNum === 1 ? 'Morning' : nextNum === 2 ? 'Afternoon' : 'Evening'})`;
    this.reportingTime = nextNum === 1 ? '07:30:00' : '13:00:00';
    this.gateClosingTime = nextNum === 1 ? '08:30:00' : '14:00:00';
    this.loginStartTime = nextNum === 1 ? '08:45:00' : '14:15:00';
    this.examStartTime = nextNum === 1 ? '09:00:00' : '14:30:00';
    this.examEndTime = nextNum === 1 ? '12:00:00' : '17:30:00';
    this.exitTime = nextNum === 1 ? '12:15:00' : '17:45:00';
    this.durationMinutes = this.selectedExam()?.durationMinutes || 180;
    this.bufferMinutes = 30;
    this.showShiftModal.set(true);
  }

  openEditShift(shift: ShiftResponse): void {
    this.editingShiftId = shift.id;
    this.shiftNumber = shift.shiftNumber;
    this.shiftName = shift.shiftName || '';
    this.reportingTime = shift.reportingTime;
    this.gateClosingTime = shift.gateClosingTime;
    this.loginStartTime = shift.loginStartTime;
    this.examStartTime = shift.examStartTime;
    this.examEndTime = shift.examEndTime;
    this.exitTime = shift.exitTime || '';
    this.durationMinutes = shift.durationMinutes;
    this.bufferMinutes = shift.bufferMinutes;
    this.showShiftModal.set(true);
  }

  saveShift(): void {
    const examId = this.selectedExamId();
    const sched = this.selectedSchedule();
    if (!examId || !sched) return;

    const payload: CreateShiftRequest = {
      shiftNumber: this.shiftNumber,
      shiftName: this.shiftName.trim() || undefined,
      reportingTime: this.reportingTime,
      gateClosingTime: this.gateClosingTime,
      loginStartTime: this.loginStartTime,
      examStartTime: this.examStartTime,
      examEndTime: this.examEndTime,
      exitTime: this.exitTime || undefined,
      durationMinutes: this.durationMinutes,
      bufferMinutes: this.bufferMinutes,
    };

    if (this.editingShiftId) {
      this.scheduleService
        .updateShift(examId, sched.id, this.editingShiftId, payload)
        .subscribe({
          next: () => {
            this.showShiftModal.set(false);
            this.snackBar.open('Shift updated successfully', 'OK', { duration: 3000 });
            this.loadShifts(sched.id);
          },
          error: (err) => {
            this.snackBar.open(
              err?.error?.message || 'Failed to update shift',
              'Dismiss',
              { duration: 4000 }
            );
          },
        });
    } else {
      this.scheduleService.addShift(examId, sched.id, payload).subscribe({
        next: () => {
          this.showShiftModal.set(false);
          this.snackBar.open('Shift added successfully', 'OK', { duration: 3000 });
          this.loadShifts(sched.id);
        },
        error: (err) => {
          this.snackBar.open(
            err?.error?.message || 'Failed to add shift',
            'Dismiss',
            { duration: 4000 }
          );
        },
      });
    }
  }

  // --- Seat Allocations ---
  viewAllocations(shift: ShiftResponse): void {
    this.selectedShift.set(shift);
    this.currentTab.set('ALLOCATIONS');
    this.loadAllocations(shift.id);
  }

  loadAllocations(shiftId: string): void {
    const examId = this.selectedExamId();
    const sched = this.selectedSchedule();
    if (!examId || !sched || !shiftId) return;

    this.loadingAllocations.set(true);
    this.centreService.listAllocations(examId, sched.id, shiftId).subscribe({
      next: (list) => {
        this.allocations.set(list || []);
        this.loadingAllocations.set(false);
      },
      error: () => this.loadingAllocations.set(false),
    });
  }

  openAddAllocation(): void {
    if (this.centres().length > 0 && !this.allocCentreId) {
      this.allocCentreId = this.centres()[0].id;
    }
    this.allocTotalSeats = 250;
    this.allocAvailableSeats = 220;
    this.allocReservedSeats = 30;
    this.allocPwdSeats = 10;
    this.allocEmergencyBufferSeats = 10;
    this.allocFemaleReservedSeats = 0;
    this.allocSpecialCategorySeats = 0;
    this.showAllocationModal.set(true);
  }

  saveAllocation(): void {
    const examId = this.selectedExamId();
    const sched = this.selectedSchedule();
    const shift = this.selectedShift();
    if (!examId || !sched || !shift || !this.allocCentreId) {
      this.snackBar.open('Please select a Test Centre', 'Dismiss', { duration: 3000 });
      return;
    }

    const payload: SeatAllocationRequest = {
      centreId: this.allocCentreId,
      totalSeats: this.allocTotalSeats,
      availableSeats: this.allocAvailableSeats,
      reservedSeats: this.allocReservedSeats,
      pwdSeats: this.allocPwdSeats,
      emergencyBufferSeats: this.allocEmergencyBufferSeats,
      femaleReservedSeats: this.allocFemaleReservedSeats,
      specialCategorySeats: this.allocSpecialCategorySeats,
    };

    this.centreService.upsertAllocation(examId, sched.id, shift.id, payload).subscribe({
      next: () => {
        this.showAllocationModal.set(false);
        this.snackBar.open('Seat Allocation updated successfully', 'OK', {
          duration: 3000,
        });
        this.loadAllocations(shift.id);
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to save seat allocation',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  getCentreName(centreId: string): string {
    const c = this.centres().find((item) => item.id === centreId);
    return c ? `${c.centreName} (${c.city}, ${c.state})` : centreId;
  }
}
