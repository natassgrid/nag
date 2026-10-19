import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';
import { SubjectTopicService } from '@nag-frontend-workspace/questions-data-access';
import { AssetService } from '../../assets/asset.service';
import {
  QuestionPaperMetadata,
  ExtractedQuestion,
  PaperUploadRequest,
  BulkAnswerKeyRequest,
  UpdateExtractedQuestionRequest,
  PaperKpiStats,
} from './models';
import { PaperExtractionService } from './services';
import {
  PaperKpiRibbonComponent,
  PaperUploadDialogComponent,
  PaperListTableComponent,
  ExtractedQuestionsPanelComponent,
  BulkAnswerKeyDialogComponent,
} from './components';

@Component({
  selector: 'nag-admin-paper-extraction',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    PaperKpiRibbonComponent,
    PaperUploadDialogComponent,
    PaperListTableComponent,
    ExtractedQuestionsPanelComponent,
    BulkAnswerKeyDialogComponent,
  ],
  templateUrl: './admin-paper-extraction.component.html',
  styleUrl: './admin-paper-extraction.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminPaperExtractionComponent implements OnInit {
  private readonly paperService = inject(PaperExtractionService);
  private readonly assetService = inject(AssetService);
  private readonly notificationService = inject(NotificationService);
  private readonly subjectTopicService = inject(SubjectTopicService);

  // View state: 'list' or 'detail'
  readonly viewMode = signal<'list' | 'detail'>('list');
  readonly selectedPaper = signal<QuestionPaperMetadata | null>(null);

  // Papers table state
  readonly papers = signal<QuestionPaperMetadata[]>([]);
  readonly loadingPapers = signal<boolean>(false);
  readonly papersPage = signal<number>(0);
  readonly papersPageSize = signal<number>(20);
  readonly papersTotalElements = signal<number>(0);
  readonly searchQuery = signal<string>('');
  readonly statusFilter = signal<string>('ALL');

  // Available subjects for upload
  readonly availableSubjects = signal<string[]>([]);

  // Dialog state
  readonly uploadDialogOpen = signal<boolean>(false);
  readonly uploading = signal<boolean>(false);
  readonly answerKeyDialogOpen = signal<boolean>(false);
  readonly applyingAnswerKey = signal<boolean>(false);
  readonly activePaperForAnswerKey = signal<QuestionPaperMetadata | null>(null);

  // Extracted staging questions detail state
  readonly extractedQuestions = signal<ExtractedQuestion[]>([]);
  readonly loadingQuestions = signal<boolean>(false);
  readonly questionsPage = signal<number>(0);
  readonly questionsPageSize = signal<number>(50);
  readonly questionsTotalElements = signal<number>(0);
  readonly questionsStatusFilter = signal<string>('ALL');
  readonly promotingQuestions = signal<boolean>(false);
  readonly updatingQuestion = signal<boolean>(false);

  // Computed KPIs
  readonly kpiStats = computed<PaperKpiStats>(() => {
    const list = this.papers() || [];
    let totalPapers = this.papersTotalElements() || list.length;
    let pagesProcessed = 0;
    let questionsExtracted = 0;
    let questionsApproved = 0;
    let questionsPromoted = 0;
    let processingPapers = 0;

    for (const p of list) {
      if (!p) continue;
      pagesProcessed += p.pagesProcessed || 0;
      questionsExtracted += p.questionsExtracted || 0;
      questionsApproved += p.questionsApproved || 0;
      questionsPromoted += p.questionsPromoted || 0;
      if (p.extractionStatus === 'PENDING' || p.extractionStatus === 'PROCESSING') {
        processingPapers++;
      }
    }

    return {
      totalPapers,
      pagesProcessed,
      questionsExtracted,
      questionsApproved,
      questionsPromoted,
      processingPapers,
    };
  });

  ngOnInit(): void {
    this.loadSubjects();
    this.loadPapers();
  }

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe({
      next: (subjects) => {
        const names = (subjects || []).map((s) => s.name).filter(Boolean) as string[];
        this.availableSubjects.set(names);
      },
      error: () => {
        this.availableSubjects.set([]);
      },
    });
  }

  loadPapers(): void {
    this.loadingPapers.set(true);
    const filter = this.statusFilter();
    const query = this.searchQuery().trim();

    this.paperService
      .listPapers({
        examinationName: query || undefined,
        status: filter !== 'ALL' ? filter : undefined,
        page: this.papersPage(),
        size: this.papersPageSize(),
      })
      .subscribe({
        next: (page) => {
          this.papers.set(page?.content || []);
          this.papersTotalElements.set(page?.totalElements || 0);
          this.loadingPapers.set(false);
        },
        error: (err) => {
          this.loadingPapers.set(false);
          this.notificationService.error(
            'Failed to load question papers: ' + (err?.error?.message || err?.message || 'Server error')
          );
        },
      });
  }

  loadExtractedQuestions(paperId: string): void {
    this.loadingQuestions.set(true);
    const filter = this.questionsStatusFilter();

    this.paperService
      .listExtractedQuestions(paperId, {
        status: filter !== 'ALL' ? filter : undefined,
        page: this.questionsPage(),
        size: this.questionsPageSize(),
      })
      .subscribe({
        next: (page) => {
          this.extractedQuestions.set(page?.content || []);
          this.questionsTotalElements.set(page?.totalElements || 0);
          this.loadingQuestions.set(false);
        },
        error: (err) => {
          this.loadingQuestions.set(false);
          this.notificationService.error(
            'Failed to load extracted questions: ' + (err?.error?.message || err?.message || 'Server error')
          );
        },
      });
  }

  switchToTab(tab: 'list' | 'detail'): void {
    if (tab === 'list') {
      this.handleBackToList();
    } else {
      if (!this.selectedPaper() && this.papers().length > 0) {
        this.handleSelectPaper(this.papers()[0]);
      } else if (this.selectedPaper()) {
        this.viewMode.set('detail');
      }
    }
  }

  onSearchChange(query: string): void {
    this.searchQuery.set(query || '');
    this.papersPage.set(0);
    this.loadPapers();
  }

  onStatusChange(status: string): void {
    this.statusFilter.set(status || 'ALL');
    this.papersPage.set(0);
    this.loadPapers();
  }

  onPapersPageChange(event: { pageIndex: number; pageSize: number }): void {
    this.papersPage.set(event.pageIndex);
    this.papersPageSize.set(event.pageSize);
    this.loadPapers();
  }

  handleSelectPaper(paper: QuestionPaperMetadata): void {
    this.selectedPaper.set(paper);
    this.viewMode.set('detail');
    this.questionsPage.set(0);
    this.questionsStatusFilter.set('ALL');
    this.loadExtractedQuestions(paper.id);
  }

  handleBackToList(): void {
    this.viewMode.set('list');
    this.selectedPaper.set(null);
    this.extractedQuestions.set([]);
    this.loadPapers();
  }

  openUploadDialog(): void {
    this.uploadDialogOpen.set(true);
  }

  closeUploadDialog(): void {
    this.uploadDialogOpen.set(false);
  }

  handleUpload(event: { file: File; metadata: Partial<PaperUploadRequest> }): void {
    this.uploading.set(true);

    // 1. Upload PDF binary to asset-service for centralized asset management & SHA-256 integrity
    this.assetService.upload(event.file).subscribe({
      next: (asset) => {
        const payload: PaperUploadRequest = {
          examinationName: event.metadata.examinationName || 'Untitled Examination',
          examYear: event.metadata.examYear || new Date().getFullYear(),
          department: event.metadata.department,
          paperCode: event.metadata.paperCode,
          bookletSerialNo: event.metadata.bookletSerialNo,
          subjectName: event.metadata.subjectName,
          defaultSubjectId: event.metadata.defaultSubjectId,
          defaultTopicId: event.metadata.defaultTopicId,
          defaultSubtopicId: event.metadata.defaultSubtopicId,
          languages: event.metadata.languages || ['en', 'hi'],
          totalQuestionsExpected: event.metadata.totalQuestionsExpected,
          durationMinutes: event.metadata.durationMinutes,
          maxMarks: event.metadata.maxMarks,
          negativeMarkingRatio: event.metadata.negativeMarkingRatio,
          forceReExtract: !!event.metadata.forceReExtract,
          fileAssetId: asset.id,
        };

        // 2. Initiate async extraction via pure JSON payload to question-bank-service
        this.paperService.initiateExtraction(payload).subscribe({
          next: (paper) => {
            this.uploading.set(false);
            this.uploadDialogOpen.set(false);
            this.notificationService.success(
              `Question Paper "${paper.examinationName}" initiated successfully via Asset #${(asset?.id || '').substring(0, 8)}. Extraction batch started.`
            );
            this.loadPapers();
          },
          error: (err) => {
            this.uploading.set(false);
            this.notificationService.error(
              'Paper extraction initiation failed: ' + (err?.error?.message || err?.message || 'Server error')
            );
          },
        });
      },
      error: (assetErr) => {
        this.uploading.set(false);
        this.notificationService.error(
          'Failed to upload PDF to asset-service: ' + (assetErr?.error?.message || assetErr?.message || 'Asset service unavailable')
        );
      },
    });
  }

  handleCancelJob(paper: QuestionPaperMetadata): void {
    this.paperService.cancelJob(paper.id).subscribe({
      next: () => {
        this.notificationService.info(`Extraction job for "${paper.examinationName}" cancelled.`);
        this.loadPapers();
      },
      error: (err) => {
        this.notificationService.error(
          'Failed to cancel job: ' + (err?.error?.message || err?.message || 'Server error')
        );
      },
    });
  }

  handleRetryJob(paper: QuestionPaperMetadata): void {
    this.paperService.retryJob(paper.id).subscribe({
      next: () => {
        this.notificationService.success(
          `Extraction job for "${paper.examinationName}" restarted.`
        );
        this.loadPapers();
      },
      error: (err) => {
        this.notificationService.error(
          'Failed to retry extraction: ' + (err?.error?.message || err?.message || 'Server error')
        );
      },
    });
  }

  openAnswerKeyDialog(paper: QuestionPaperMetadata): void {
    this.activePaperForAnswerKey.set(paper);
    this.answerKeyDialogOpen.set(true);
  }

  closeAnswerKeyDialog(): void {
    this.answerKeyDialogOpen.set(false);
    this.activePaperForAnswerKey.set(null);
  }

  handleApplyAnswerKey(event: { paperId: string; request: BulkAnswerKeyRequest }): void {
    this.applyingAnswerKey.set(true);
    this.paperService.applyAnswerKey(event.paperId, event.request).subscribe({
      next: (updatedPaper) => {
        this.applyingAnswerKey.set(false);
        this.closeAnswerKeyDialog();
        this.notificationService.success(
          `Applied answer keys to paper "${updatedPaper.examinationName}".`
        );
        // Refresh detail view if currently open
        if (this.selectedPaper()?.id === event.paperId) {
          this.selectedPaper.set(updatedPaper);
          this.loadExtractedQuestions(event.paperId);
        } else {
          this.loadPapers();
        }
      },
      error: (err) => {
        this.applyingAnswerKey.set(false);
        this.notificationService.error(
          'Failed to apply answer key: ' + (err?.error?.message || err?.message || 'Server error')
        );
      },
    });
  }

  onQuestionsStatusFilterChange(status: string): void {
    this.questionsStatusFilter.set(status);
    this.questionsPage.set(0);
    const paper = this.selectedPaper();
    if (paper) {
      this.loadExtractedQuestions(paper.id);
    }
  }

  onQuestionsPageChange(event: { pageIndex: number; pageSize: number }): void {
    this.questionsPage.set(event.pageIndex);
    this.questionsPageSize.set(event.pageSize);
    const paper = this.selectedPaper();
    if (paper) {
      this.loadExtractedQuestions(paper.id);
    }
  }

  handleUpdateQuestion(event: { questionId: string; request: UpdateExtractedQuestionRequest }): void {
    const paper = this.selectedPaper();
    if (!paper) return;

    this.updatingQuestion.set(true);
    this.paperService
      .updateExtractedQuestion(paper.id, event.questionId, event.request)
      .subscribe({
        next: (updated) => {
          this.updatingQuestion.set(false);
          // In-memory update of question in list for instant reactive response
          const list = this.extractedQuestions().map((q) => (q.id === updated.id ? updated : q));
          this.extractedQuestions.set(list);
          this.notificationService.success(`Question Q${updated.questionNumber} updated successfully.`);
        },
        error: (err) => {
          this.updatingQuestion.set(false);
          this.notificationService.error(
            'Failed to update question: ' + (err?.error?.message || err?.message || 'Server error')
          );
        },
      });
  }

  handlePromoteApproved(event: { paperId: string; questionIds?: string[] }): void {
    this.promotingQuestions.set(true);
    this.paperService
      .promoteQuestions(event.paperId, { extractedQuestionIds: event.questionIds })
      .subscribe({
        next: (res) => {
          this.promotingQuestions.set(false);
          this.notificationService.success(
            `Promotion Complete: ${res.totalPromoted} questions promoted to Question Bank (${res.totalSkipped} skipped, ${res.totalFailed} failed).`
          );
          // Reload paper metadata & questions
          this.paperService.getPaper(event.paperId).subscribe({
            next: (p) => this.selectedPaper.set(p),
          });
          this.loadExtractedQuestions(event.paperId);
        },
        error: (err) => {
          this.promotingQuestions.set(false);
          this.notificationService.error(
            'Promotion failed: ' + (err?.error?.message || err?.message || 'Server error')
          );
        },
      });
  }
}
