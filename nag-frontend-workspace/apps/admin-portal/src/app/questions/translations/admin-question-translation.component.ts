import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  TranslationService,
  QuestionBankService,
  SubjectTopicService,
  Subject,
  Question,
  INDIC_TRANSLATION_LANGUAGES,
  IndicTranslationLanguage,
  BatchTranslationJobResponse,
  TranslationResponse,
  AutoTranslateResponse,
} from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'app-admin-question-translation',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './admin-question-translation.component.html',
  styleUrl: './admin-question-translation.component.scss',
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

  readonly availableSubjects = computed(() => {
    const list = this.taxonomySubjects().map((s) => s.name);
    if (list.length > 0) return list;
    const fromQuestions = Array.from(new Set(this.questions().map((q) => q.subject).filter(Boolean)));
    return fromQuestions as string[];
  });

  readonly filteredQuestions = computed(() => {
    const qList = this.questions();
    const query = this.searchQuery().toLowerCase().trim();
    const subject = this.selectedSubject();

    return qList.filter((q) => {
      const matchSubject = subject === 'ALL' || q.subject === subject;
      const matchQuery =
        !query ||
        (q.content && q.content.toLowerCase().includes(query)) ||
        (q.code && q.code.toLowerCase().includes(query)) ||
        (q.id && q.id.toLowerCase().includes(query));
      return matchSubject && matchQuery;
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

  // Batch Translation Modal State
  readonly batchModalOpen = signal<boolean>(false);
  readonly submittingBatch = signal<boolean>(false);
  readonly loadingBatchJobs = signal<boolean>(false);
  readonly batchSourceLanguage = signal<string>('en');
  readonly batchTargetLang = signal<string>('hi');
  readonly batchSubject = signal<string>('ALL');
  readonly batchOverwrite = signal<boolean>(false);
  readonly batchJobs = signal<BatchTranslationJobResponse[]>([]);

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
      page: 0,
      size: 100,
    };
    this.questionBankService.loadQuestions(filter).subscribe({
      next: (res) => {
        this.questions.set(res.content || []);
        this.loadingQuestions.set(false);
      },
      error: () => {
        this.loadingQuestions.set(false);
      },
    });
  }

  loadBatchJobs(): void {
    this.loadingBatchJobs.set(true);
    this.translationService.listBatchJobs().subscribe({
      next: (jobs) => {
        this.batchJobs.set(jobs || []);
        this.loadingBatchJobs.set(false);
      },
      error: () => {
        this.batchJobs.set([]);
        this.loadingBatchJobs.set(false);
      },
    });
  }

  setLanguage(code: string): void {
    this.selectedLanguage.set(code);
    if (this.activeQuestion()) {
      this.fetchExistingTranslation(this.activeQuestion()!.id, code);
    }
  }

  openTranslationEditor(question: Question): void {
    this.activeQuestion.set(question);
    this.drawerOpen.set(true);
    this.fetchExistingTranslation(question.id, this.selectedLanguage());
  }

  openTranslationDrawer(question: Question): void {
    this.openTranslationEditor(question);
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
          this.translationStatus.set(res.status || status);
          this.savingTranslation.set(false);
          if (status === 'APPROVED' && (res.id || res.translationId)) {
            const targetId = res.id || res.translationId!;
            this.translationService.approveTranslation(targetId).subscribe({
              next: () => this.translationStatus.set('APPROVED'),
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

  triggerBatchJob(): void {
    this.submittingBatch.set(true);
    const req = {
      sourceLanguage: this.batchSourceLanguage(),
      targetLanguage: this.batchTargetLang(),
      subject: this.batchSubject() === 'ALL' ? undefined : this.batchSubject(),
      overwriteExisting: this.batchOverwrite(),
    };

    this.translationService.startBatchTranslation(req).subscribe({
      next: (job) => {
        this.batchJobs.update((jobs) => [job, ...jobs]);
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
}
