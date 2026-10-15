import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import { GenerateMicroLessonRequest, MicroLessonReviewItem } from '../../models/micro-lesson.model';

@Component({
  selector: 'app-generate-micro-lesson-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './generate-micro-lesson-dialog.component.html',
  styleUrls: ['./generate-micro-lesson-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenerateMicroLessonDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<GenerateMicroLessonDialogComponent>);
  private readonly microLessonService = inject(MicroLessonAdminService);

  readonly subject = signal<string>('Physics');
  readonly topic = signal<string>('Mechanics');
  readonly subtopic = signal<string>('Rotational Dynamics');
  readonly targetExam = signal<string>('JEE_MAIN');
  readonly referenceText = signal<string>('');
  readonly mode = signal<'SINGLE' | 'BATCH'>('SINGLE');

  readonly isGenerating = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  readonly examOptions = [
    { label: 'JEE Main & Advanced', value: 'JEE_MAIN' },
    { label: 'NEET-UG (Medical)', value: 'NEET' },
    { label: 'GATE Engineering', value: 'GATE' },
    { label: 'SSC CGL', value: 'SSC_CGL' },
    { label: 'UPSC Civil Services', value: 'UPSC_CSAT' },
  ];

  onGenerate(): void {
    if (!this.subtopic().trim()) {
      this.errorMessage.set('Subtopic name is required.');
      return;
    }

    this.isGenerating.set(true);
    this.errorMessage.set(null);

    const req: GenerateMicroLessonRequest = {
      subject: this.subject().trim() || 'Physics',
      topic: this.topic().trim() || 'General',
      subtopic: this.subtopic().trim(),
      targetExam: this.targetExam(),
      referenceText: this.referenceText().trim() || undefined,
    };

    this.microLessonService.generateSingle(req).subscribe({
      next: (item: MicroLessonReviewItem) => {
        this.isGenerating.set(false);
        this.dialogRef.close(item);
      },
      error: (err) => {
        this.isGenerating.set(false);
        this.errorMessage.set(
          err?.error?.message || 'AI micro-lesson generation failed. Please try again.'
        );
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }
}
