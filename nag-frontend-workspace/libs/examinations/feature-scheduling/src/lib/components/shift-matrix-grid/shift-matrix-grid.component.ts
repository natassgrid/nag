import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  ScheduleResponse,
  ShiftResponse,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-shift-matrix-grid',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
  ],
  templateUrl: './shift-matrix-grid.component.html',
  styleUrl: './shift-matrix-grid.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShiftMatrixGridComponent {
  shifts = input<ShiftResponse[]>([]);
  selectedSchedule = input<ScheduleResponse | null>(null);

  addShiftClick = output<void>();
  editShiftClick = output<ShiftResponse>();
  viewAllocationsClick = output<ShiftResponse>();

  onAddShift(): void {
    this.addShiftClick.emit();
  }

  onEditShift(shift: ShiftResponse): void {
    this.editShiftClick.emit(shift);
  }

  onViewAllocations(shift: ShiftResponse): void {
    this.viewAllocationsClick.emit(shift);
  }
}
