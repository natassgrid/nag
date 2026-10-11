import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import {
  TranslationService,
  QuestionBankService,
  SubjectTopicService,
  Subject,
  Question,
  INDIC_TRANSLATION_LANGUAGES,
  BatchTranslationJobResponse,
  BatchTranslationRequest,
  AutoTranslateResponse,
} from '@nag-frontend-workspace/questions-data-access';
import {
  TranslationLanguageStripComponent,
  QuestionTranslationTableComponent,
  BatchJobsListComponent,
  TranslationWorkbenchDrawerComponent,
  BatchTranslationModalComponent,
} from './components';

@Component({
  selector: 'app-admin-question-translation',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    TranslationLanguageStripComponent,
    QuestionTranslationTableComponent,
    BatchJobsListComponent,
    TranslationWorkbenchDrawerComponent,
    BatchTranslationModalComponent,
  ],
  templateUrl: './admin-question-translation.component.html',
  styleUrl: './admin-question-translation.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminQuestionTranslationComponent implements OnInit {
  private readonly translationService = inject(TranslationService);
  private readonly questionBankService = inject(QuestionBankService);
  private readonly subjectTopicService = inject(SubjectTopicService);

  readonly languages = INDIC_TRANSLATION_LANGUAGES;
  readonly selectedLanguage = signal<string>('hi');
  readonly currentTab = signal<'list' | 'batch'>('list');

  // Dynamic Taxonomy & Questions
  readonly taxonomySubjects = signal<Subject[]>([]);
  readonly questions = signal<Question[]>([]);
  readonly loadingQuestions = signal<boolean>(false);
  readonly searchQuery = signal<string>('');
  readonly selectedSubject = signal<string>('ALL');

  // Question Pagination State
  readonly questionsPage = signal<number>(0);
  readonly questionsPageSize = signal<number>(20);
  readonly questionsTotalElements = signal<number>(0);
  readonly pageSizeOptions = [10, 25, 50];

  readonly availableSubjects = computed(() => {
    const list = this.taxonomySubjects().map((s) => s.name);
    if (list.length > 0) return list;
    const fromQuestions = Array.from(new Set(this.questions().map((q) => q.subject).filter(Boolean)));
    return fromQuestions as string[];
  });

  readonly displayedQuestions = computed(() => {
    return [...this.questions()].sort((a: any, b: any) => {
      const timeA = new Date(a.updatedAt || a.createdAt || 0).getTime();
      const timeB = new Date(b.updatedAt || b.createdAt || 0).getTime();
      return timeB - timeA;
    });
  });

  // Translation Drawer / Editor State
  readonly drawerOpen = signal<boolean>(false);
  readonly activeQuestion = signal<Question | null>(null);
  readonly aiTranslating = signal<boolean>(false);
  readonly savingTranslation = signal<boolean>(false);
  readonly translatedContent = signal<string>('');
  readonly translatedOptions = signal<{ id: string; text: string }[]>([]);
  readonly translatedExplanation = signal<string>('');
  readonly translationStatus = signal<string>('DRAFT');
  readonly currentTranslationId = signal<string | null>(null);

  // Batch Translation State
  readonly batchModalOpen = signal<boolean>(false);
  readonly submittingBatch = signal<boolean>(false);
  readonly loadingBatchJobs = signal<boolean>(false);
  readonly batchJobs = signal<BatchTranslationJobResponse[]>([]);
  readonly batchJobsPage = signal<number>(0);
  readonly batchJobsPageSize = signal<number>(20);
  readonly batchJobsTotalElements = signal<number>(0);
  readonly batchJobsPageSizeOptions = [10, 20, 50];

  readonly activeLanguageName = computed(() => {
    const lang = INDIC_TRANSLATION_LANGUAGES.find((l) => l.code === this.selectedLanguage());
    return lang ? `${lang.name} (${lang.nativeName})` : this.selectedLanguage();
  });

  ngOnInit(): void {
    this.loadSubjects();
    this.loadQuestions();
    this.loadBatchJobs();
  }

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe({
      next: (subjects) => this.taxonomySubjects.set(subjects || []),
      error: () => this.taxonomySubjects.set([]),
    });
  }

  loadQuestions(): void {
    this.loadingQuestions.set(true);
    const filter: any = {
      page: this.questionsPage(),
      size: this.questionsPageSize(),
      targetLang: this.selectedLanguage(),
      sort: 'updatedAt',
      order: 'desc',
    };
    if (this.selectedSubject() && this.selectedSubject() !== 'ALL') {
      filter.subject = this.selectedSubject();
    }
    if (this.searchQuery() && this.searchQuery().trim()) {
      filter.search = this.searchQuery().trim();
    }

    this.questionBankService.loadQuestions(filter).subscribe({
      next: (res) => {
        this.questions.set(res.content || []);
        this.questionsTotalElements.set(res.totalElements ?? (res.content?.length || 0));
        this.loadingQuestions.set(false);
      },
      error: () => {
        this.questions.set([]);
        this.questionsTotalElements.set(0);
        this.loadingQuestions.set(false);
      },
    });
  }

  onSearchChange(query: string): void {
    this.searchQuery.set(query);
    this.questionsPage.set(0);
    this.loadQuestions();
  }

  onSubjectChange(subject: string): void {
    this.selectedSubject.set(subject);
    this.questionsPage.set(0);
    this.loadQuestions();
  }

  onQuestionsPageChange(event: { pageIndex: number; pageSize: number }): void {
    this.questionsPage.set(event.pageIndex);
    this.questionsPageSize.set(event.pageSize);
    this.loadQuestions();
  }

  loadBatchJobs(): void {
    this.loadingBatchJobs.set(true);
    this.translationService
      .listBatchJobs(this.batchJobsPage(), this.batchJobsPageSize())
      .subscribe({
        next: (res) => {
          this.batchJobs.set(res.content || []);
          this.batchJobsTotalElements.set(res.totalElements ?? (res.content?.length || 0));
          this.loadingBatchJobs.set(false);
        },
        error: () => {
          this.batchJobs.set([]);
          this.batchJobsTotalElements.set(0);
          this.loadingBatchJobs.set(false);
        },
      });
  }

  onBatchJobsPageChange(event: { pageIndex: number; pageSize: number }): void {
    this.batchJobsPage.set(event.pageIndex);
    this.batchJobsPageSize.set(event.pageSize);
    this.loadBatchJobs();
  }

  setLanguage(code: string): void {
    this.selectedLanguage.set(code);
    this.questionsPage.set(0);
    if (this.activeQuestion()) {
      this.fetchExistingTranslation(this.activeQuestion()!.id, code);
    }
    this.loadQuestions();
  }

  openTranslationEditor(question: Question): void {
    this.activeQuestion.set(question);
    this.drawerOpen.set(true);
    this.fetchExistingTranslation(question.id, this.selectedLanguage());
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
    this.activeQuestion.set(null);
  }

  fetchExistingTranslation(questionId: string, langCode: string): void {
    this.translationService.getApprovedTranslation(questionId, langCode).subscribe({
      next: (trans) => {
        if (trans) {
          this.currentTranslationId.set(trans.id || trans.translationId || null);
          this.translatedContent.set(trans.translatedContent || '');
          this.translatedExplanation.set(trans.translatedExplanation || '');
          this.translationStatus.set(trans.status || 'DRAFT');
          if (trans.translatedOptions && trans.translatedOptions.length > 0) {
            this.translatedOptions.set(
              trans.translatedOptions.map((o) => ({ id: o.id, text: o.text }))
            );
          } else {
            this.initDefaultOptions();
          }
        } else {
          this.resetTranslationFields();
        }
      },
      error: () => {
        this.resetTranslationFields();
      },
    });
  }

  private resetTranslationFields(): void {
    this.currentTranslationId.set(null);
    this.translatedContent.set('');
    this.translatedExplanation.set('');
    this.translationStatus.set('DRAFT');
    this.initDefaultOptions();
  }

  private initDefaultOptions(): void {
    const q = this.activeQuestion();
    if (q && q.options) {
      this.translatedOptions.set(
        q.options.map((opt) => ({ id: opt.id, text: '' }))
      );
    } else {
      this.translatedOptions.set([]);
    }
  }

  updateTranslatedOption(event: { index: number; text: string }): void {
    this.translatedOptions.update((opts) => {
      const copy = [...opts];
      if (copy[event.index]) {
        copy[event.index] = { ...copy[event.index], text: event.text };
      }
      return copy;
    });
  }

  runIndicAiTranslation(): void {
    const q = this.activeQuestion();
    if (!q) return;

    this.aiTranslating.set(true);
    this.translationService.autoTranslateQuestion(q.id, this.selectedLanguage()).subscribe({
      next: (res: AutoTranslateResponse) => {
        this.translatedContent.set(res.translatedContent || '');
        this.translatedExplanation.set(res.translatedExplanation || '');
        if (res.translatedOptions && res.translatedOptions.length > 0) {
          this.translatedOptions.set(
            res.translatedOptions.map((o) => ({ id: o.id, text: o.text }))
          );
        }
        this.aiTranslating.set(false);
      },
      error: () => {
        this.aiTranslating.set(false);
      },
    });
  }

  approveCurrentTranslation(): void {
    this.saveTranslation('APPROVED');
  }

  saveTranslation(status: 'DRAFT' | 'APPROVED' = 'DRAFT'): void {
    const q = this.activeQuestion();
    if (!q) return;

    this.savingTranslation.set(true);
    this.translationService
      .saveTranslation({
        questionId: q.id,
        languageCode: this.selectedLanguage(),
        translatedContent: this.translatedContent(),
        translatedExplanation: this.translatedExplanation(),
        translatedOptions: this.translatedOptions(),
      })
      .subscribe({
        next: (res) => {
          this.currentTranslationId.set(res.id || res.translationId || null);
          const lang = this.selectedLanguage();
          const savedStatus = status === 'APPROVED' ? 'APPROVED' : (res.status || status);
          this.translationStatus.set(savedStatus);
          this.savingTranslation.set(false);

          this.questions.update((items) =>
            items.map((item) => {
              if (item.id === q.id) {
                const map = { ...(item.translationStatusMap || {}), [lang]: savedStatus };
                return { ...item, translationStatusMap: map, translationStatus: savedStatus };
              }
              return item;
            })
          );

          if (status === 'APPROVED' && (res.id || res.translationId)) {
            const targetId = res.id || res.translationId!;
            this.translationService.approveTranslation(targetId).subscribe({
              next: () => {
                this.translationStatus.set('APPROVED');
                this.questions.update((items) =>
                  items.map((item) => {
                    if (item.id === q.id) {
                      const map = { ...(item.translationStatusMap || {}), [lang]: 'APPROVED' };
                      return { ...item, translationStatusMap: map, translationStatus: 'APPROVED' };
                    }
                    return item;
                  })
                );
              },
            });
          }
        },
        error: () => {
          this.savingTranslation.set(false);
        },
      });
  }

  // --- Batch Modal Actions ---

  openBatchModal(): void {
    this.batchModalOpen.set(true);
  }

  closeBatchModal(): void {
    this.batchModalOpen.set(false);
  }

  triggerBatchJob(req: BatchTranslationRequest): void {
    this.submittingBatch.set(true);
    this.translationService.startBatchTranslation(req).subscribe({
      next: (job) => {
        this.batchJobs.update((jobs) => [job, ...jobs]);
        this.batchJobsTotalElements.update((total) => total + 1);
        this.submittingBatch.set(false);
        this.closeBatchModal();
        this.currentTab.set('batch');
      },
      error: () => {
        this.submittingBatch.set(false);
      },
    });
  }

  cancelJob(jobId: string): void {
    this.translationService.cancelBatchJob(jobId).subscribe({
      next: (updatedJob) => {
        this.batchJobs.update((jobs) =>
          jobs.map((j) => (j.id === jobId ? updatedJob : j))
        );
      },
    });
  }

  resumeJob(jobId: string): void {
    this.translationService.resumeBatchJob(jobId).subscribe({
      next: (updatedJob) => {
        this.batchJobs.update((jobs) =>
          jobs.map((j) => (j.id === jobId ? updatedJob : j))
        );
      },
    });
  }
}
