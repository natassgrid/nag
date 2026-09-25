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
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
  ExaminationService,
  SchedulingService,
  CentreManagementService,
  ScheduleResponse,
  ShiftResponse,
  SeatAllocationResponse,
  CreateScheduleRequest,
  ScheduleTransitionRequest,
  AmendScheduleRequest,
  CreateShiftRequest,
  SeatAllocationRequest,
} from '@nag-frontend-workspace/examinations-data-access';
import { SchedulingTab } from '../models';
import {
  ScheduleListTableComponent,
  ShiftMatrixGridComponent,
  SeatAllocationPanelComponent,
  CreateScheduleModalComponent,
  ScheduleTransitionModalComponent,
  ScheduleAmendModalComponent,
  ShiftEditModalComponent,
  SeatAllocationModalComponent,
} from '../components';

@Component({
  selector: 'nag-examinations-feature-scheduling',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    ScheduleListTableComponent,
    ShiftMatrixGridComponent,
    SeatAllocationPanelComponent,
    CreateScheduleModalComponent,
    ScheduleTransitionModalComponent,
    ScheduleAmendModalComponent,
    ShiftEditModalComponent,
    SeatAllocationModalComponent,
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
  readonly currentTab = signal<SchedulingTab>('SCHEDULES');

  // Exam Selection
  readonly exams = this.examService.exams;
  readonly selectedExamId = signal<string>('');
  readonly selectedExam = computed(() =>
    (this.exams() || []).find((e) => e?.id === this.selectedExamId()) || null
  );

  // Schedules State
  readonly schedules = this.scheduleService.schedules;
  readonly loading = this.scheduleService.loading;
  readonly selectedSchedule = signal<ScheduleResponse | null>(null);

  // Shifts State
  readonly shifts = this.scheduleService.shifts;
  readonly selectedShift = signal<ShiftResponse | null>(null);
  readonly editingShift = signal<ShiftResponse | null>(null);

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

  // Computed KPIs
  readonly totalSchedulesCount = computed(() => (this.schedules() || []).length);
  readonly totalShiftsCount = computed(() => (this.shifts() || []).length);

  readonly filteredSchedules = computed(() => {
    const list = this.schedules() || [];
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) return list;
    return list.filter((s) => {
      if (!s) return false;
      return (
        (s.scheduleName && s.scheduleName.toLowerCase().includes(q)) ||
        (s.notificationNumber && s.notificationNumber.toLowerCase().includes(q)) ||
        (s.examDate && s.examDate.includes(q))
      );
    });
  });

  ngOnInit(): void {
    this.examService.getExams(0, 50).subscribe({
      next: (exams) => {
        const list = exams || [];
        this.route.queryParams.subscribe((params) => {
          const paramExamId = params['examId'];
          if (paramExamId && list.some((e) => e?.id === paramExamId)) {
            this.selectExam(paramExamId);
          } else if (list.length > 0 && !this.selectedExamId()) {
            this.selectExam(list[0].id);
          }
        });
      },
    });

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
        const items = list || [];
        if (items.length > 0 && !this.selectedSchedule()) {
          this.selectSchedule(items[0]);
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
    if (schedule?.id) {
      this.loadShifts(schedule.id);
    }
  }

  loadShifts(scheduleId: string): void {
    const examId = this.selectedExamId();
    if (!examId || !scheduleId) return;

    this.scheduleService.listShifts(examId, scheduleId).subscribe({
      next: (shifts) => {
        const items = shifts || [];
        if (items.length > 0 && !this.selectedShift()) {
          this.selectedShift.set(items[0]);
        }
      },
      error: () => {},
    });
  }

  openCreateSchedule(): void {
    this.showCreateScheduleModal.set(true);
  }

  handleCreateSchedule(payload: CreateScheduleRequest): void {
    const examId = this.selectedExamId();
    if (!examId) return;

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

  openTransitionModal(schedule: ScheduleResponse): void {
    this.selectedSchedule.set(schedule);
    this.showTransitionModal.set(true);
  }

  handleTransition(req: ScheduleTransitionRequest): void {
    const examId = this.selectedExamId();
    const sched = this.selectedSchedule();
    if (!examId || !sched) return;

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

  openAmendModal(schedule: ScheduleResponse): void {
    this.selectedSchedule.set(schedule);
    this.showAmendModal.set(true);
  }

  handleAmend(req: AmendScheduleRequest): void {
    const examId = this.selectedExamId();
    const sched = this.selectedSchedule();
    if (!examId || !sched) return;

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

  openAddShift(): void {
    this.editingShift.set(null);
    this.showShiftModal.set(true);
  }

  openEditShift(shift: ShiftResponse): void {
    this.editingShift.set(shift);
    this.showShiftModal.set(true);
  }

  handleSaveShift(event: { shiftId: string | null; request: CreateShiftRequest }): void {
    const examId = this.selectedExamId();
    const sched = this.selectedSchedule();
    if (!examId || !sched) return;

    if (event.shiftId) {
      this.scheduleService
        .updateShift(examId, sched.id, event.shiftId, event.request)
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
      this.scheduleService.addShift(examId, sched.id, event.request).subscribe({
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

  viewAllocations(shift: ShiftResponse): void {
    this.selectedShift.set(shift);
    this.currentTab.set('ALLOCATIONS');
    if (shift?.id) {
      this.loadAllocations(shift.id);
    }
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
    this.showAllocationModal.set(true);
  }

  handleSaveAllocation(payload: SeatAllocationRequest): void {
    const examId = this.selectedExamId();
    const sched = this.selectedSchedule();
    const shift = this.selectedShift();
    if (!examId || !sched || !shift) return;

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
}
