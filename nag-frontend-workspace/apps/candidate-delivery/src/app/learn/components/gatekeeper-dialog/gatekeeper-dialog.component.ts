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
  quizTargetThreshold?: number;
  gatekeeperPassThreshold?: number;
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

  get quizTargetThreshold(): number {
    return this.data?.quizTargetThreshold ?? 70;
  }

  get gatekeeperPassThreshold(): number {
    return this.data?.gatekeeperPassThreshold ?? 75;
  }

  get conceptualGaps(): Array<{ subtopicName: string }> {
    const gaps = this.data?.statusResponse?.conceptualGaps;
    if (Array.isArray(gaps) && gaps.length > 0) {
      return gaps.map((g) => (typeof g === 'string' ? { subtopicName: g } : g));
    }
    const rem = this.data?.statusResponse?.remedialSubtopics;
    if (Array.isArray(rem) && rem.length > 0) {
      return rem.map((s) => ({ subtopicName: s }));
    }
    return [];
  }

  get applicationGaps(): Array<{ subtopicName: string }> {
    const gaps = this.data?.statusResponse?.applicationGaps;
    if (Array.isArray(gaps) && gaps.length > 0) {
      return gaps.map((g) => (typeof g === 'string' ? { subtopicName: g } : g));
    }
    return [];
  }

  get remedialSubtopics(): string[] {
    const gaps = this.data?.statusResponse?.conceptualGaps;
    if (Array.isArray(gaps) && gaps.length > 0) {
      return this.data?.statusResponse?.remedialSubtopics || [];
    }
    return [];
  }

  onGoToRoadmap(): void {
    this.dialogRef.close('GO_TO_ROADMAP');
    this.router.navigate(['/learn/roadmap']);
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
