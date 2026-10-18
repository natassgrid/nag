import {
  Component,
  ChangeDetectionStrategy,
  inject,
  OnInit,
  OnDestroy,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import {
  SubjectTopicService,
  SubjectHierarchy,
  TargetExaminationOption,
} from '@nag-frontend-workspace/questions-data-access';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import {
  BatchGenerateMicroLessonsRequest,
  BatchGenerateResponse,
} from '../../models/micro-lesson.model';
import { Subscription, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';

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
    MatProgressBarModule,
  ],
  templateUrl: './batch-generate-dialog.component.html',
  styleUrls: ['./batch-generate-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BatchGenerateMicroLessonDialogComponent implements OnInit, OnDestroy {
  private readonly dialogRef = inject(MatDialogRef<BatchGenerateMicroLessonDialogComponent>);
  private readonly microLessonService = inject(MicroLessonAdminService);
  private readonly subjectTopicService = inject(SubjectTopicService);

  readonly hierarchy = signal<SubjectHierarchy[]>([]);
  readonly isLoadingTaxonomy = signal<boolean>(false);

  readonly selectedSubjectId = signal<number | null>(null);
  readonly selectedTopicId = signal<number | null>(null);

  readonly targetExam = signal<string>('JEE_MAIN');
  readonly examOptions = signal<{ label: string; value: string; category?: string }[]>([]);
  readonly isLoadingExams = signal<boolean>(false);
  readonly isCustomExam = signal<boolean>(false);

  readonly maxCount = signal<number>(10);
  readonly forceRegenerate = signal<boolean>(false);

  readonly isGenerating = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Live progress monitoring state
  readonly isMonitoring = signal<boolean>(false);
  readonly currentJob = signal<BatchGenerateResponse | null>(null);
  readonly isCancelling = signal<boolean>(false);
  private pollSub?: Subscription;

  readonly countOptions = [5, 10, 15, 20, 25];

  readonly availableTopics = computed(() => {
    const subjectId = this.selectedSubjectId();
    if (!subjectId) return [];
    const subj = this.hierarchy().find((s) => s.id === subjectId);
    return subj ? subj.topics : [];
  });

  ngOnInit(): void {
    this.loadTaxonomy();
    this.loadExaminations();
    this.checkActiveBatch();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  checkActiveBatch(): void {
    this.microLessonService.getActiveBatchJob().subscribe({
      next: (active) => {
        if (active && (active.status === 'IN_PROGRESS' || active.status === 'QUEUED')) {
          this.currentJob.set(active);
          this.isMonitoring.set(true);
          if (active.batchId) {
            this.startPolling(active.batchId);
          }
        }
      },
      error: () => {
        // Ignore failure on initial active job check
      },
    });
  }

  loadExaminations(): void {
    this.isLoadingExams.set(true);
    this.subjectTopicService.getExaminations().subscribe({
      next: (exams: TargetExaminationOption[]) => {
        this.isLoadingExams.set(false);
        if (exams && exams.length > 0) {
          const mapped = exams.map((e) => ({
            label: `${e.name} (${e.code})`,
            value: e.code,
            category: e.category,
          }));
          this.examOptions.set(mapped);
          this.targetExam.set(mapped[0].value);
        } else {
          this.examOptions.set([
            { label: 'JEE Main & Advanced (JEE_MAIN)', value: 'JEE_MAIN' },
            { label: 'NEET-UG Medical (NEET)', value: 'NEET' },
            { label: 'GATE Engineering (GATE)', value: 'GATE' },
            { label: 'SSC CGL (SSC_CGL)', value: 'SSC_CGL' },
            { label: 'UPSC Civil Services (UPSC_CSAT)', value: 'UPSC_CSAT' },
          ]);
          this.targetExam.set('JEE_MAIN');
        }
      },
      error: () => {
        this.isLoadingExams.set(false);
        this.examOptions.set([
          { label: 'JEE Main & Advanced (JEE_MAIN)', value: 'JEE_MAIN' },
          { label: 'NEET-UG Medical (NEET)', value: 'NEET' },
          { label: 'GATE Engineering (GATE)', value: 'GATE' },
          { label: 'SSC CGL (SSC_CGL)', value: 'SSC_CGL' },
          { label: 'UPSC Civil Services (UPSC_CSAT)', value: 'UPSC_CSAT' },
        ]);
        this.targetExam.set('JEE_MAIN');
      },
    });
  }

  onExamChange(val: string): void {
    if (val === '__CUSTOM__') {
      this.isCustomExam.set(true);
      this.targetExam.set('');
    } else {
      this.isCustomExam.set(false);
      this.targetExam.set(val);
    }
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

    const examCode = this.targetExam().trim() || 'General';

    const req: BatchGenerateMicroLessonsRequest = {
      subjectId: this.selectedSubjectId() ?? undefined,
      topicId: this.selectedTopicId() ?? undefined,
      targetExam: examCode,
      maxCount: this.maxCount(),
      forceRegenerate: this.forceRegenerate(),
    };

    this.microLessonService.triggerBatchGenerate(req).subscribe({
      next: (res: BatchGenerateResponse) => {
        this.isGenerating.set(false);
        this.currentJob.set(res);
        this.isMonitoring.set(true);

        if (res.batchId && (res.status === 'IN_PROGRESS' || res.status === 'QUEUED')) {
          this.startPolling(res.batchId);
        }
      },
      error: (err) => {
        this.isGenerating.set(false);
        this.errorMessage.set(
          err?.error?.message || 'Failed to queue batch micro-lesson generation. Please try again.'
        );
      },
    });
  }

  startPolling(batchId: string): void {
    this.stopPolling();
    this.pollSub = timer(1500, 1500)
      .pipe(switchMap(() => this.microLessonService.getBatchJobStatus(batchId)))
      .subscribe({
        next: (status) => {
          this.currentJob.set(status);
          if (
            status.status === 'COMPLETED' ||
            status.status === 'FAILED' ||
            status.status === 'CANCELLED'
          ) {
            this.stopPolling();
          }
        },
        error: (err) => {
          console.error('Failed to fetch batch job status', err);
        },
      });
  }

  stopPolling(): void {
    if (this.pollSub) {
      this.pollSub.unsubscribe();
      this.pollSub = undefined;
    }
  }

  onCancelJob(): void {
    const batchId = this.currentJob()?.batchId;
    if (!batchId) return;

    this.isCancelling.set(true);
    this.microLessonService.cancelBatchJob(batchId).subscribe({
      next: (res) => {
        this.isCancelling.set(false);
        this.currentJob.set(res);
        this.stopPolling();
      },
      error: (err) => {
        this.isCancelling.set(false);
        console.error('Failed to cancel batch job', err);
      },
    });
  }

  onRunInBackground(): void {
    this.dialogRef.close(this.currentJob());
  }

  onDone(): void {
    this.dialogRef.close(this.currentJob());
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }
}
