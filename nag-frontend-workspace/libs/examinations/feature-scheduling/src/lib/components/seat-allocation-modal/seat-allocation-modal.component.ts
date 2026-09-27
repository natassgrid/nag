import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  effect,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  CentreResponse,
  SeatAllocationRequest,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-seat-allocation-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './seat-allocation-modal.component.html',
  styleUrl: './seat-allocation-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeatAllocationModalComponent {
  isOpen = input<boolean>(false);
  centres = input<CentreResponse[]>([]);

  closeModal = output<void>();
  saveAllocation = output<SeatAllocationRequest>();

  allocCentreId = '';
  allocTotalSeats = 250;
  allocAvailableSeats = 220;
  allocReservedSeats = 30;
  allocPwdSeats = 10;
  allocEmergencyBufferSeats = 10;
  allocFemaleReservedSeats = 0;
  allocSpecialCategorySeats = 0;

  constructor() {
    effect(() => {
      if (this.isOpen()) {
        const list = this.centres() || [];
        if (list.length > 0 && !this.allocCentreId) {
          this.allocCentreId = list[0].id;
        }
        this.allocTotalSeats = 250;
        this.allocAvailableSeats = 220;
        this.allocReservedSeats = 30;
        this.allocPwdSeats = 10;
        this.allocEmergencyBufferSeats = 10;
        this.allocFemaleReservedSeats = 0;
        this.allocSpecialCategorySeats = 0;
      }
    });
  }

  onClose(): void {
    this.closeModal.emit();
  }

  onSubmit(): void {
    if (!this.allocCentreId) return;

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

    this.saveAllocation.emit(payload);
  }
}
