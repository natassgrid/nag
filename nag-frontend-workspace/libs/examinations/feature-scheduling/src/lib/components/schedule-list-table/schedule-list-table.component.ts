import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ScheduleResponse } from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-schedule-list-table',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './schedule-list-table.component.html',
  styleUrl: './schedule-list-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleListTableComponent {
  schedules = input<ScheduleResponse[]>([]);
  selectedSchedule = input<ScheduleResponse | null>(null);
  loading = input<boolean>(false);

  scheduleSelect = output<ScheduleResponse>();
  viewShifts = output<ScheduleResponse>();
  transitionClick = output<ScheduleResponse>();
  amendClick = output<ScheduleResponse>();
  createClick = output<void>();

  onSelect(schedule: ScheduleResponse): void {
    this.scheduleSelect.emit(schedule);
  }

  onViewShifts(schedule: ScheduleResponse, event: Event): void {
    event.stopPropagation();
    this.viewShifts.emit(schedule);
  }

  onTransition(schedule: ScheduleResponse, event: Event): void {
    event.stopPropagation();
    this.transitionClick.emit(schedule);
  }

  onAmend(schedule: ScheduleResponse, event: Event): void {
    event.stopPropagation();
    this.amendClick.emit(schedule);
  }

  onCreate(): void {
    this.createClick.emit();
  }
}
