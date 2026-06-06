import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-reject-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>Reject Question</h2>
    <mat-dialog-content>
      <p>Please provide comments explaining why this question is being rejected.</p>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Rejection Comments</mat-label>
        <textarea
          matInput
          [(ngModel)]="comments"
          rows="4"
          placeholder="Enter feedback for the author..."
          aria-label="Rejection comments"
        ></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button
        mat-raised-button
        color="warn"
        [disabled]="!comments.trim()"
        (click)="confirm()"
      >
        Reject
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width {
      width: 100%;
    }
    mat-dialog-content {
      min-width: 350px;
    }
  `]
})
export class RejectDialogComponent {
  comments = '';

  constructor(private dialogRef: MatDialogRef<RejectDialogComponent>) {}

  confirm(): void {
    this.dialogRef.close(this.comments.trim());
  }
}
