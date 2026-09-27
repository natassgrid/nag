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
import {
  ShiftResponse,
  SeatAllocationResponse,
  CentreResponse,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-seat-allocation-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './seat-allocation-panel.component.html',
  styleUrl: './seat-allocation-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeatAllocationPanelComponent {
  allocations = input<SeatAllocationResponse[]>([]);
  selectedShift = input<ShiftResponse | null>(null);
  loading = input<boolean>(false);
  centres = input<CentreResponse[]>([]);

  addAllocationClick = output<void>();

  onAddAllocation(): void {
    this.addAllocationClick.emit();
  }

  getCentreName(centreId: string): string {
    const c = (this.centres() || []).find((item) => item?.id === centreId);
    return c ? `${c.centreName} (${c.city}, ${c.state})` : centreId;
  }
}
