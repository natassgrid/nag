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
import { Subscription, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import {
  SubjectTopicService,
  SubjectHierarchy,
} from '@nag-frontend-workspace/questions-data-access';
import { ExaminationService } from '@nag-frontend-workspace/examinations-data-access';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import {
  MicroLessonBatchTranslationRequest,
  MicroLessonBatchTranslationJob,
  SUPPORTED_MICROLESSON_LANGUAGES,
  MicroLessonLanguage,
} from '../../models/micro-lesson.model';

@Component({
  selector: 'app-batch-translate-micro-lesson-dialog',
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
  templateUrl: './batch-translate-micro-lesson-dialog.component.html',
  styleUrls: ['./batch-translate-micro-lesson-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BatchTranslateMicroLessonDialogComponent implements OnInit, OnDestroy {
  private readonly dialogRef = inject(MatDialogRef<BatchTranslateMicroLessonDialogComponent>);
  private readonly microLessonService = inject(MicroLessonAdminService);
  private readonly subjectTopicService = inject(SubjectTopicService);
  private readonly examinationService = inject(ExaminationService, { optional: true });

  readonly languages: MicroLessonLanguage[] = SUPPORTED_MICROLESSON_LANGUAGES.filter(
    (l) => l.code !== 'en'
  );
  readonly hierarchy = signal<SubjectHierarchy[]>([]);
  readonly isLoadingTaxonomy = signal<boolean>(false);

  // Form State
  readonly sourceLanguage = signal<string>('en');
  readonly targetLanguage = signal<string>('hi');
  readonly selectedSubject = signal<string>('ALL');
  readonly selectedTopic = signal<string>('ALL');
  readonly targetExam = signal<string>('ALL');
  readonly targetStatus = signal<string>('ALL');
  readonly overwriteExisting = signal<boolean>(false);
  readonly batchSize = signal<number>(25);
  readonly maxConcurrency = signal<number>(4);

  readonly isSubmitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Live Monitoring State
  readonly isMonitoring = signal<boolean>(false);
  readonly currentJob = signal<MicroLessonBatchTranslationJob | null>(null);
  readonly isCancelling = signal<boolean>(false);
  private pollSub?: Subscription;

  private readonly defaultExamOptions = [
    { label: 'All Target Exams', value: 'ALL' },
    { label: 'JEE Main & Advanced', value: 'JEE_MAIN' },
    { label: 'NEET-UG Medical', value: 'NEET' },
    { label: 'GATE Engineering', value: 'GATE' },
    { label: 'SSC CGL', value: 'SSC_CGL' },
    { label: 'UPSC Civil Services', value: 'UPSC_CSAT' },
  ];

  private readonly dynamicExamOptions = signal<Array<{ label: string; value: string }>>(
    this.defaultExamOptions
  );

  get examOptions(): Array<{ label: string; value: string }> {
    return this.dynamicExamOptions();
  }

  readonly statusFilterOptions = [
    { label: 'All Micro-Lessons', value: 'ALL' },
    { label: 'Approved Only', value: 'APPROVED' },
    { label: 'Published Only', value: 'PUBLISHED' },
    { label: 'AI Generated', value: 'AI_GENERATED' },
  ];

  readonly availableTopics = computed(() => {
    const subjName = this.selectedSubject();
    if (!subjName || subjName === 'ALL') return [];
    const subj = this.hierarchy().find((s) => s.name === subjName);
    return subj ? subj.topics : [];
  });

  ngOnInit(): void {
    this.loadTaxonomy();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  loadTaxonomy(): void {
    this.isLoadingTaxonomy.set(true);
    this.subjectTopicService.getHierarchy().subscribe({
      next: (data) => {
        this.hierarchy.set(data || []);
        this.isLoadingTaxonomy.set(false);
      },
      error: () => {
        this.isLoadingTaxonomy.set(false);
      },
    });

    if (typeof (this.subjectTopicService as any).getExaminations === 'function') {
      this.subjectTopicService.getExaminations().subscribe({
        next: (exams) => {
          if (Array.isArray(exams) && exams.length > 0) {
            const seen = new Set<string>(['ALL']);
            const mapped: Array<{ label: string; value: string }> = [
              { label: 'All Target Exams', value: 'ALL' },
            ];
            for (const ex of exams) {
              const code = (ex.code || ex.name || '').trim();
              if (code && !seen.has(code)) {
                seen.add(code);
                mapped.push({
                  label: ex.name ? `${ex.name} (${code})` : code,
                  value: code,
                });
              }
            }
            if (mapped.length > 1) {
              this.dynamicExamOptions.set(mapped);
            }
          }
        },
        error: () => {
          // Keep defaultExamOptions fallback
        },
      });
    } else if (this.examinationService) {
      this.examinationService.getExams(0, 50).subscribe({
        next: (exams) => {
          if (Array.isArray(exams) && exams.length > 0) {
            const seen = new Set<string>(['ALL']);
            const mapped: Array<{ label: string; value: string }> = [
              { label: 'All Target Exams', value: 'ALL' },
            ];
            for (const ex of exams) {
              const code = (ex.code || ex.name || '').trim();
              if (code && !seen.has(code)) {
                seen.add(code);
                mapped.push({
                  label: ex.name ? `${ex.name} (${code})` : code,
                  value: code,
                });
              }
            }
            if (mapped.length > 1) {
              this.dynamicExamOptions.set(mapped);
            }
          }
        },
        error: () => {
          // Keep defaultExamOptions fallback
        },
      });
    }
  }

  onSubjectChange(subject: string): void {
    this.selectedSubject.set(subject);
    this.selectedTopic.set('ALL');
  }

  onTopicChange(topic: string): void {
    this.selectedTopic.set(topic);
  }

  startBatchTranslation(): void {
    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const req: MicroLessonBatchTranslationRequest = {
      sourceLanguage: this.sourceLanguage(),
      targetLanguage: this.targetLanguage(),
      subject: this.selectedSubject() === 'ALL' ? undefined : this.selectedSubject(),
      topic: this.selectedTopic() === 'ALL' ? undefined : this.selectedTopic(),
      targetExam: this.targetExam() === 'ALL' ? undefined : this.targetExam(),
      targetStatus: this.targetStatus() === 'ALL' ? undefined : this.targetStatus(),
      overwriteExisting: this.overwriteExisting(),
      batchSize: this.batchSize(),
      maxConcurrency: this.maxConcurrency(),
    };

    this.microLessonService.startBatchTranslation(req).subscribe({
      next: (job) => {
        this.isSubmitting.set(false);
        this.currentJob.set(job);
        this.isMonitoring.set(true);
        if (job.status === 'IN_PROGRESS' || job.status === 'PENDING') {
          const jobId = job.id || job.jobId || '';
          if (jobId) {
            this.startPolling(jobId);
          }
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(
          err?.error?.message || 'Failed to start batch translation job.'
        );
      },
    });
  }

  startPolling(jobId: string): void {
    if (!jobId) return;
    this.stopPolling();
    this.pollSub = timer(2000, 2000)
      .pipe(switchMap(() => this.microLessonService.getBatchTranslationJobStatus(jobId)))
      .subscribe({
        next: (job) => {
          this.currentJob.set(job);
          if (
            job.status === 'COMPLETED' ||
            job.status === 'FAILED' ||
            job.status === 'CANCELLED'
          ) {
            this.stopPolling();
          }
        },
        error: (err) => {
          console.error('Polling error for batch translation job', err);
        },
      });
  }

  stopPolling(): void {
    if (this.pollSub) {
      this.pollSub.unsubscribe();
      this.pollSub = undefined;
    }
  }

  cancelJob(): void {
    const job = this.currentJob();
    const jobId = job?.id || job?.jobId;
    if (!jobId) return;

    this.isCancelling.set(true);
    this.microLessonService.cancelBatchTranslationJob(jobId).subscribe({
      next: (res) => {
        this.isCancelling.set(false);
        this.currentJob.set(res);
        this.stopPolling();
      },
      error: (err) => {
        this.isCancelling.set(false);
        this.errorMessage.set(err?.error?.message || 'Failed to cancel job.');
      },
    });
  }

  onClose(): void {
    this.dialogRef.close(this.currentJob());
  }
}
