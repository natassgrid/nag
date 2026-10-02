import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PracticeService } from '../../services/practice.service';
import { PracticeSet, PracticeSessionMode } from '../../models';

@Component({
  selector: 'app-practice-launch-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatRadioModule, MatProgressSpinnerModule, FormsModule],
  templateUrl: './practice-launch-dialog.component.html',
  styleUrl: './practice-launch-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeLaunchDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<PracticeLaunchDialogComponent>);
  private readonly data = inject<{ set: PracticeSet }>(MAT_DIALOG_DATA);
  private readonly practiceService = inject(PracticeService);
  private readonly router = inject(Router);

  readonly set = this.data.set;
  readonly selectedMode = signal<PracticeSessionMode>('TIMED');
  readonly isStarting = signal(false);

  startSession(): void {
    if (this.isStarting()) return;
    this.isStarting.set(true);
    
    this.practiceService.startSession({
      practiceSetId: this.set.id,
      mode: this.selectedMode()
    }).subscribe({
      next: (session) => {
        this.dialogRef.close();
        this.router.navigate(['/delivery'], { 
          queryParams: { mode: 'PRACTICE', paperId: session.id, sessionId: session.id }
        });
      },
      error: () => {
        this.isStarting.set(false);
      }
    });
  }
}
