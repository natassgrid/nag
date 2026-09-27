import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  ExaminationResponse,
  ScheduleResponse,
  ShiftResponse,
  SeatAllocationResponse,
} from '@nag-frontend-workspace/examinations-data-access';
import { SchedulingTab } from '../../models';

@Component({
  selector: 'nag-schedule-toolbar-header',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './schedule-toolbar-header.component.html',
  styleUrl: './schedule-toolbar-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleToolbarHeaderComponent {
  readonly exams = input<ExaminationResponse[]>([]);
  readonly selectedExamId = input<string>('');
  readonly selectedSchedule = input<ScheduleResponse | null>(null);
  readonly selectedShift = input<ShiftResponse | null>(null);
  readonly currentTab = input<SchedulingTab>('SCHEDULES');
  readonly loading = input<boolean>(false);
  readonly totalSchedules = input<number>(0);
  readonly totalShifts = input<number>(0);
  readonly totalAllocations = input<number>(0);

  readonly refresh = output<void>();
  readonly createSchedule = output<void>();
  readonly examChange = output<string>();
  readonly tabChange = output<SchedulingTab>();
  readonly transitionClick = output<ScheduleResponse>();
  readonly amendClick = output<ScheduleResponse>();
}
