import { Component, ChangeDetectionStrategy, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SubjectTopicService, SubjectHierarchy } from '@nag-frontend-workspace/questions-data-access';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import { BatchGenerateMicroLessonsRequest, BatchGenerateResponse } from '../../models/micro-lesson.model';

@Component({
  selector: 'app-batch-generate-micro-lesson-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './batch-generate-dialog.component.html',
  styleUrls: ['./batch-generate-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BatchGenerateMicroLessonDialogComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<BatchGenerateMicroLessonDialogComponent>);
  private readonly microLessonService = inject(MicroLessonAdminService);
  private readonly subjectTopicService = inject(SubjectTopicService);

  readonly hierarchy = signal<SubjectHierarchy[]>([]);
  readonly isLoadingTaxonomy = signal<boolean>(false);

  readonly selectedSubjectId = signal<number | null>(null);
  readonly selectedTopicId = signal<number | null>(null);
  readonly targetExam = signal<string>('JEE_MAIN');
  readonly maxCount = signal<number>(10);
  readonly forceRegenerate = signal<boolean>(false);

  readonly isGenerating = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  readonly examOptions = [
    { label: 'JEE Main & Advanced', value: 'JEE_MAIN' },
    { label: 'NEET-UG (Medical)', value: 'NEET' },
    { label: 'GATE Engineering', value: 'GATE' },
    { label: 'SSC CGL', value: 'SSC_CGL' },
    { label: 'UPSC Civil Services', value: 'UPSC_CSAT' },
  ];

  readonly countOptions = [5, 10, 15, 20, 25];

  readonly availableTopics = computed(() => {
    const subjectId = this.selectedSubjectId();
    if (!subjectId) return [];
    const subj = this.hierarchy().find((s) => s.id === subjectId);
    return subj ? subj.topics : [];
  });

  ngOnInit(): void {
    this.loadTaxonomy();
  }

  loadTaxonomy(): void {
    this.isLoadingTaxonomy.set(true);
    this.subjectTopicService.getHierarchy().subscribe({
      next: (tree) => {
        this.hierarchy.set(tree || []);
        this.isLoadingTaxonomy.set(false);
      },
      error: (err) => {
        console.error('Failed to load subject taxonomy for batch generation', err);
        this.isLoadingTaxonomy.set(false);
      },
    });
  }

  onSubjectChange(val: string): void {
    const id = val === 'ALL' || !val ? null : Number(val);
    this.selectedSubjectId.set(id);
    this.selectedTopicId.set(null);
  }

  onTopicChange(val: string): void {
    const id = val === 'ALL' || !val ? null : Number(val);
    this.selectedTopicId.set(id);
  }

  onStartBatch(): void {
    this.isGenerating.set(true);
    this.errorMessage.set(null);

    const req: BatchGenerateMicroLessonsRequest = {
      subjectId: this.selectedSubjectId() ?? undefined,
      topicId: this.selectedTopicId() ?? undefined,
      targetExam: this.targetExam(),
      maxCount: this.maxCount(),
      forceRegenerate: this.forceRegenerate(),
    };

    this.microLessonService.triggerBatchGenerate(req).subscribe({
      next: (res: BatchGenerateResponse) => {
        this.isGenerating.set(false);
        this.dialogRef.close(res);
      },
      error: (err) => {
        this.isGenerating.set(false);
        this.errorMessage.set(
          err?.error?.message || 'Failed to queue batch micro-lesson generation. Please try again.'
        );
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }
}
