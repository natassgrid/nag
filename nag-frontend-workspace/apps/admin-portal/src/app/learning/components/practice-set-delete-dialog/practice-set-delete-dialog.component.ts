import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PracticeSetService } from '../../services';
import { PracticeSet } from '../../models';

export interface PracticeSetDeleteDialogData {
  set: PracticeSet;
}

@Component({
  selector: 'app-practice-set-delete-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './practice-set-delete-dialog.component.html',
  styleUrls: ['./practice-set-delete-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeSetDeleteDialogComponent {
  public readonly data = inject<PracticeSetDeleteDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<PracticeSetDeleteDialogComponent>);
  private readonly practiceSetService = inject(PracticeSetService);

  isDeleting = signal(false);

  onDelete() {
    this.isDeleting.set(true);
    this.practiceSetService.delete(this.data.set.id).subscribe({
      next: () => this.dialogRef.close(true),
      error: (err) => {
        console.error('Failed to delete', err);
        this.isDeleting.set(false);
      },
    });
  }

  onCancel() {
    this.dialogRef.close(false);
  }
}
