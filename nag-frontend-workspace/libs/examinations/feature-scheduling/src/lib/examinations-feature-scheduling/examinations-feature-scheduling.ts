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
  ScheduleToolbarHeaderComponent,
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
    ScheduleToolbarHeaderComponent,
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

  // Modals & Drawers Visibility
  readonly showCreateScheduleModal = signal<boolean>(false);
  readonly showTransitionModal = signal<boolean>(false);
  readonly showAmendModal = signal<boolean>(false);
  readonly showShiftModal = signal<boolean>(false);
  readonly showAllocationModal = signal<boolean>(false);

  // Computeds
  readonly totalSchedulesCount = computed(() => this.schedules().length);
  readonly totalShiftsCount = computed(() => this.shifts().length);
  readonly filteredSchedules = computed(() => this.schedules());

  ngOnInit(): void {
    this.examService.getExams(0, 100).subscribe((exams) => {
      if (exams && exams.length > 0) {
        this.selectedExamId.set(exams[0].id);
        this.loadSchedules();
      }
    });

    this.centreService.listCentres().subscribe();

    this.route.queryParams.subscribe((params) => {
      if (params['examId']) {
        this.selectedExamId.set(params['examId']);
        this.loadSchedules();
      }
      if (params['tab']) {
        this.currentTab.set(params['tab']);
      }
    });
  }

  selectExam(examId: string): void {
    this.selectedExamId.set(examId);
    this.selectedSchedule.set(null);
    this.selectedShift.set(null);
    this.allocations.set([]);
    this.loadSchedules();
  }

  loadSchedules(): void {
    const examId = this.selectedExamId();
    if (!examId) return;

    this.scheduleService.listSchedules(examId, 0, 50).subscribe((schedules) => {
      if (schedules && schedules.length > 0) {
        this.selectSchedule(schedules[0]);
      } else {
        this.selectedSchedule.set(null);
        this.selectedShift.set(null);
      }
    });
  }

  selectSchedule(schedule: ScheduleResponse): void {
    this.selectedSchedule.set(schedule);
    this.selectedShift.set(null);
    this.allocations.set([]);
    this.loadShifts(schedule.id);
  }

  loadShifts(scheduleId: string): void {
    const examId = this.selectedExamId();
    if (!examId || !scheduleId) return;

    this.scheduleService.listShifts(examId, scheduleId).subscribe((shifts) => {
      if (shifts && shifts.length > 0) {
        this.selectedShift.set(shifts[0]);
      }
    });
  }

  // Action Triggers
  openCreateSchedule(): void {
    this.showCreateScheduleModal.set(true);
  }

  handleCreateSchedule(formData: CreateScheduleRequest): void {
    const examId = this.selectedExamId();
    if (!examId) return;

    this.scheduleService.createSchedule(examId, formData).subscribe({
      next: (res) => {
        this.snackBar.open(`Schedule "${res.scheduleName}" created!`, 'OK', { duration: 3000 });
        this.showCreateScheduleModal.set(false);
        this.loadSchedules();
      },
      error: (err: any) => {
        this.snackBar.open(err?.error?.message || 'Failed to create schedule session', 'Dismiss', {
          duration: 4000,
        });
      },
    });
  }

  openTransitionModal(schedule: ScheduleResponse): void {
    this.selectedSchedule.set(schedule);
    this.showTransitionModal.set(true);
  }

  handleTransition(req: ScheduleTransitionRequest): void {
    const examId = this.selectedExamId();
    const scheduleId = this.selectedSchedule()?.id;
    if (!examId || !scheduleId) return;

    this.scheduleService.transitionSchedule(examId, scheduleId, req).subscribe({
      next: (res) => {
        this.snackBar.open(`Schedule status transitioned to ${res.status}`, 'OK', {
          duration: 3000,
        });
        this.showTransitionModal.set(false);
        this.loadSchedules();
      },
      error: (err: any) => {
        this.snackBar.open(err?.error?.message || 'Status transition rejected', 'Dismiss', {
          duration: 4000,
        });
      },
    });
  }

  openAmendModal(schedule: ScheduleResponse): void {
    this.selectedSchedule.set(schedule);
    this.showAmendModal.set(true);
  }

  handleAmend(req: AmendScheduleRequest): void {
    const examId = this.selectedExamId();
    const scheduleId = this.selectedSchedule()?.id;
    if (!examId || !scheduleId) return;

    this.scheduleService.amendSchedule(examId, scheduleId, req).subscribe({
      next: (res) => {
        this.snackBar.open(`Schedule amended to version ${res.scheduleVersion}`, 'OK', {
          duration: 3000,
        });
        this.showAmendModal.set(false);
        this.loadSchedules();
      },
      error: (err: any) => {
        this.snackBar.open(err?.error?.message || 'Schedule amendment failed', 'Dismiss', {
          duration: 4000,
        });
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
    const scheduleId = this.selectedSchedule()?.id;
    if (!examId || !scheduleId) return;

    const op$ = event.shiftId
      ? this.scheduleService.updateShift(examId, scheduleId, event.shiftId, event.request)
      : this.scheduleService.addShift(examId, scheduleId, event.request);

    op$.subscribe({
      next: () => {
        this.snackBar.open('Shift session saved successfully!', 'OK', { duration: 3000 });
        this.showShiftModal.set(false);
        this.loadShifts(scheduleId);
      },
      error: (err: any) => {
        this.snackBar.open(err?.error?.message || 'Shift save failed', 'Dismiss', {
          duration: 4000,
        });
      },
    });
  }

  viewAllocations(shift: ShiftResponse): void {
    this.selectedShift.set(shift);
    this.currentTab.set('ALLOCATIONS');
    this.loadAllocations(shift.id);
  }

  loadAllocations(shiftId: string): void {
    const examId = this.selectedExamId();
    const scheduleId = this.selectedSchedule()?.id;
    if (!examId || !scheduleId || !shiftId) return;

    this.loadingAllocations.set(true);
    this.centreService.listAllocations(examId, scheduleId, shiftId).subscribe({
      next: (allocations: SeatAllocationResponse[]) => {
        this.allocations.set(allocations || []);
        this.loadingAllocations.set(false);
      },
      error: () => {
        this.loadingAllocations.set(false);
      },
    });
  }

  openAddAllocation(): void {
    this.showAllocationModal.set(true);
  }

  handleSaveAllocation(req: SeatAllocationRequest): void {
    const examId = this.selectedExamId();
    const scheduleId = this.selectedSchedule()?.id;
    const shiftId = this.selectedShift()?.id;
    if (!examId || !scheduleId || !shiftId) return;

    this.centreService.upsertAllocation(examId, scheduleId, shiftId, req).subscribe({
      next: () => {
        this.snackBar.open('Seat quota allocated successfully!', 'OK', { duration: 3000 });
        this.showAllocationModal.set(false);
        this.loadAllocations(shiftId);
      },
      error: (err: any) => {
        this.snackBar.open(err?.error?.message || 'Seat allocation failed', 'Dismiss', {
          duration: 4000,
        });
      },
    });
  }
}
