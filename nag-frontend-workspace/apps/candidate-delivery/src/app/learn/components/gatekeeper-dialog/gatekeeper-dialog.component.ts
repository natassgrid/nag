import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { GatekeeperStatusResponse } from '../../models/learning-path.model';

export interface GatekeeperDialogData {
  statusResponse?: GatekeeperStatusResponse;
  mockExamTitle?: string;
}

@Component({
  selector: 'app-gatekeeper-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
  ],
  templateUrl: './gatekeeper-dialog.component.html',
  styleUrl: './gatekeeper-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GatekeeperDialogComponent {
  readonly dialogRef = inject(MatDialogRef<GatekeeperDialogComponent>);
  readonly data: GatekeeperDialogData = inject(MAT_DIALOG_DATA, { optional: true }) || {};
  private readonly router = inject(Router);

  get conceptualGaps() {
    return this.data?.statusResponse?.conceptualGaps || [];
  }

  get applicationGaps() {
    return this.data?.statusResponse?.applicationGaps || [];
  }

  onGoToRoadmap(): void {
    this.dialogRef.close('GO_TO_ROADMAP');
    this.router.navigate(['/learn/roadmap']);
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
