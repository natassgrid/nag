import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { PageHeaderComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  PaperService,
  ExaminationService,
  SchedulingService,
  PaperSummary,
  PaperDetail,
  ExaminationResponse,
  ScheduleResponse,
  ShiftResponse,
  BlueprintRule,
  BlueprintTemplateResponse,
  BlueprintFeasibilityResponse,
  PaperGenerationRequest,
  PaperTranslateRequest,
  PaperTranslateResponse,
  GapDetail,
} from '@nag-frontend-workspace/examinations-data-access';

export interface SupportedLanguage {
  code: string;
  label: string;
  native: string;
}

export const SUPPORTED_LANGUAGES: SupportedLanguage[] = [
  { code: 'hi', label: 'Hindi', native: 'हिन्दी' },
  { code: 'bn', label: 'Bengali', native: 'বাংলা' },
  { code: 'ta', label: 'Tamil', native: 'தமிழ்' },
  { code: 'te', label: 'Telugu', native: 'తెలుగు' },
  { code: 'mr', label: 'Marathi', native: 'मराठी' },
  { code: 'gu', label: 'Gujarati', native: 'ગુજરાતી' },
  { code: 'kn', label: 'Kannada', native: 'ಕನ್ನಡ' },
  { code: 'ml', label: 'Malayalam', native: 'മലയാളം' },
  { code: 'or', label: 'Odia', native: 'ଓଡ଼ିଆ' },
  { code: 'pa', label: 'Punjabi', native: 'ਪੰਜਾਬੀ' },
  { code: 'as', label: 'Assamese', native: 'অসমীয়া' },
];

@Component({
  selector: 'nag-examinations-feature-paper-gen',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    PageHeaderComponent,
  ],
  templateUrl: './examinations-feature-paper-gen.component.html',
  styleUrl: './examinations-feature-paper-gen.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExaminationsFeaturePaperGen implements OnInit {
  private readonly paperService = inject(PaperService);
  private readonly examService = inject(ExaminationService);
  private readonly scheduleService = inject(SchedulingService);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);

  // Active View Tab: 'PAPERS' | 'GENERATOR' | 'TEMPLATES'
  readonly currentTab = signal<'PAPERS' | 'GENERATOR' | 'TEMPLATES'>('PAPERS');

  // Examinations & Schedules Reference State
  readonly exams = this.examService.exams;
  readonly schedules = signal<ScheduleResponse[]>([]);
  readonly shifts = signal<ShiftResponse[]>([]);

  // Papers List State
  readonly papers = this.paperService.papers;
  readonly loading = this.paperService.loading;
  readonly searchQuery = signal<string>('');
  readonly statusFilter = signal<string>('ALL');
  readonly selectedExamFilter = signal<string>('ALL');

  // Blueprint Templates State
  readonly templates = signal<BlueprintTemplateResponse[]>([]);
  readonly loadingTemplates = signal<boolean>(false);

  // Selected Paper Detail & Summary Drawer
  readonly selectedPaperId = signal<string | null>(null);
  readonly paperDetail = signal<PaperDetail | null>(null);
  readonly loadingDetail = signal<boolean>(false);
  readonly drawerOpen = signal<boolean>(false);

  // Algorithmic Generator State
  readonly isGenerating = signal<boolean>(false);
  genExamId = '';
  genScheduleId = '';
  genShiftId = '';
  genPaperName = '';
  genIsPractice = false;
  genUseTemplate = true;
  genSelectedTemplateId = '';

  // Custom Blueprint Rules Matrix
  genRules: BlueprintRule[] = [
    {
      subject: 'Physics',
      topic: 'Mechanics & Dynamics',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'APPLY',
      questionType: 'SINGLE_MCQ',
      targetCount: 15,
    },
    {
      subject: 'Chemistry',
      topic: 'Organic Reaction Mechanisms',
      difficulty: 'HARD',
      cognitiveLevel: 'ANALYZE',
      questionType: 'SINGLE_MCQ',
      targetCount: 15,
    },
    {
      subject: 'Mathematics',
      topic: 'Calculus & Linear Algebra',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'APPLY',
      questionType: 'SINGLE_MCQ',
      targetCount: 20,
    },
  ];

  // Feasibility Check State
  readonly isCheckingFeasibility = signal<boolean>(false);
  readonly feasibilityResult = signal<BlueprintFeasibilityResponse | null>(null);
  readonly showFeasibilityModal = signal<boolean>(false);

  // Translation Panel State
  readonly supportedLanguages = SUPPORTED_LANGUAGES;
  transTargetLanguage = 'hi';
  transOverwriteExisting = false;
  readonly isTranslating = signal<boolean>(false);
  readonly activeTranslationJob = signal<PaperTranslateResponse | null>(null);

  // Computed Metrics
  readonly totalPapersCount = computed(() => (this.papers() || []).length);
  readonly approvedPapersCount = computed(
    () => (this.papers() || []).filter((p) => p?.status === 'APPROVED' || p?.status === 'ENCRYPTED').length
  );
  readonly draftPapersCount = computed(
    () => (this.papers() || []).filter((p) => p?.status === 'DRAFT').length
  );
  readonly practicePapersCount = computed(
    () => (this.papers() || []).filter((p) => !!p?.isPractice).length
  );

  readonly filteredPapers = computed(() => {
    const list = this.papers() || [];
    const q = this.searchQuery().toLowerCase().trim();
    const st = this.statusFilter();
    const ex = this.selectedExamFilter();

    return list.filter((p) => {
      if (!p) return false;
      const matchSearch =
        !q ||
        (p.name && p.name.toLowerCase().includes(q)) ||
        (p.examName && p.examName.toLowerCase().includes(q)) ||
        (p.shiftName && p.shiftName.toLowerCase().includes(q)) ||
        (p.id && p.id.toLowerCase().includes(q));

      const matchStatus = st === 'ALL' || p.status === st;
      const matchExam = ex === 'ALL' || p.examId === ex;

      return matchSearch && matchStatus && matchExam;
    });
  });

  readonly totalRequestedQuestions = computed(() => {
    if (this.genUseTemplate) {
      const t = (this.templates() || []).find((tpl) => tpl?.id === this.genSelectedTemplateId);
      return t?.rules?.reduce((sum, r) => sum + (r.targetCount || r.questionCount || 0), 0) || 0;
    }
    return this.genRules.reduce((sum, r) => sum + (r.targetCount || r.questionCount || 0), 0);
  });

  ngOnInit(): void {
    this.loadExams();
    this.loadPapers();
    this.loadTemplates();
  }

  loadExams(): void {
    this.examService.getExams(0, 100).subscribe({
      next: (exams) => {
        const list = exams || [];
        this.route.queryParams.subscribe((params) => {
          const examIdParam = params['examId'];
          if (examIdParam && list.some((e) => e?.id === examIdParam)) {
            this.genExamId = examIdParam;
            this.onExamChange(examIdParam);
          } else if (list.length > 0 && !this.genExamId) {
            this.genExamId = list[0].id;
            this.onExamChange(list[0].id);
          }
        });
      },
    });
  }

  loadPapers(): void {
    this.paperService.getPapers({ page: 0, size: 50 }).subscribe({
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to load question papers',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  loadTemplates(): void {
    this.loadingTemplates.set(true);
    this.paperService.listTemplates().subscribe({
      next: (tpls) => {
        const items = tpls || [];
        this.templates.set(items);
        if (items.length > 0 && !this.genSelectedTemplateId) {
          this.genSelectedTemplateId = items[0].id;
        }
        this.loadingTemplates.set(false);
      },
      error: () => this.loadingTemplates.set(false),
    });
  }

  onExamChange(examId: string): void {
    this.schedules.set([]);
    this.shifts.set([]);
    this.genScheduleId = '';
    this.genShiftId = '';

    if (!examId) return;

    const exam = (this.exams() || []).find((e) => e?.id === examId);
    if (exam) {
      this.genPaperName = `${exam.name} - Paper ${new Date().getFullYear()}`;
    }

    this.scheduleService.listSchedules(examId, 0, 50).subscribe({
      next: (scheds) => {
        const items = scheds || [];
        this.schedules.set(items);
        if (items.length > 0) {
          this.genScheduleId = items[0].id;
          this.onScheduleChange(items[0].id);
        }
      },
    });
  }

  onScheduleChange(scheduleId: string): void {
    this.shifts.set([]);
    this.genShiftId = '';

    if (!this.genExamId || !scheduleId) return;

    this.scheduleService.listShifts(this.genExamId, scheduleId).subscribe({
      next: (shiftList) => {
        const items = shiftList || [];
        this.shifts.set(items);
        if (items.length > 0) {
          this.genShiftId = items[0].id;
        }
      },
    });
  }

  // --- Dynamic Rules Builder ---
  addRule(): void {
    this.genRules.push({
      subject: 'Physics',
      topic: 'General Topics',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'UNDERSTAND',
      questionType: 'SINGLE_MCQ',
      targetCount: 10,
    });
  }

  removeRule(index: number): void {
    if (this.genRules.length > 1) {
      this.genRules.splice(index, 1);
    }
  }

  // --- Feasibility Checker ---
  checkFeasibility(): void {
    let rulesToCheck: BlueprintRule[] = [];

    if (this.genUseTemplate) {
      const selectedTpl = (this.templates() || []).find((t) => t?.id === this.genSelectedTemplateId);
      if (!selectedTpl || !selectedTpl.rules?.length) {
        this.snackBar.open('Please select a valid Blueprint Template', 'Dismiss', { duration: 3000 });
        return;
      }
      rulesToCheck = selectedTpl.rules;
    } else {
      if (!this.genRules.length) {
        this.snackBar.open('Please add at least one blueprint rule', 'Dismiss', { duration: 3000 });
        return;
      }
      rulesToCheck = this.genRules;
    }

    this.isCheckingFeasibility.set(true);
    this.paperService
      .checkFeasibility({
        examId: this.genExamId || undefined,
        shiftId: this.genShiftId || undefined,
        rules: rulesToCheck,
      })
      .subscribe({
        next: (res) => {
          this.isCheckingFeasibility.set(false);
          this.feasibilityResult.set(res);
          this.showFeasibilityModal.set(true);
        },
        error: (err) => {
          this.isCheckingFeasibility.set(false);
          this.snackBar.open(
            err?.error?.message || 'Feasibility check failed',
            'Dismiss',
            { duration: 4000 }
          );
        },
      });
  }

  closeFeasibilityModal(): void {
    this.showFeasibilityModal.set(false);
  }

  // --- Algorithmic Paper Generation ---
  generatePaper(): void {
    if (!this.genExamId) {
      this.snackBar.open('Please select a Target Examination', 'Dismiss', { duration: 3000 });
      return;
    }
    if (!this.genShiftId) {
      this.snackBar.open('Please select a Session / Shift', 'Dismiss', { duration: 3000 });
      return;
    }

    let rules: BlueprintRule[] = [];
    if (this.genUseTemplate) {
      const selectedTpl = (this.templates() || []).find((t) => t?.id === this.genSelectedTemplateId);
      if (!selectedTpl || !selectedTpl.rules?.length) {
        this.snackBar.open('Please select a valid Blueprint Template', 'Dismiss', { duration: 3000 });
        return;
      }
      rules = selectedTpl.rules;
    } else {
      rules = this.genRules;
    }

    const payload: PaperGenerationRequest = {
      examId: this.genExamId,
      shiftId: this.genShiftId,
      name: this.genPaperName.trim() || undefined,
      isPractice: this.genIsPractice,
      blueprintRules: rules,
    };

    this.isGenerating.set(true);
    this.paperService.generatePaper(payload).subscribe({
      next: (res) => {
        this.isGenerating.set(false);
        this.snackBar.open(
          `Question Paper "${res.name || res.paperId}" generated successfully!`,
          'OK',
          { duration: 4000 }
        );
        this.currentTab.set('PAPERS');
        this.loadPapers();
        if (res.paperId) {
          this.openSummaryDrawer(res.paperId);
        }
      },
      error: (err) => {
        this.isGenerating.set(false);
        const gaps = err?.error?.gaps;
        if (gaps && Array.isArray(gaps) && gaps.length > 0) {
          this.feasibilityResult.set({
            feasible: false,
            summary: err?.error?.message || 'Inventory sufficiency validation failed',
            gaps: gaps,
          });
          this.showFeasibilityModal.set(true);
        } else {
          this.snackBar.open(
            err?.error?.message || err?.message || 'Paper generation failed',
            'Dismiss',
            { duration: 5000 }
          );
        }
      },
    });
  }

  // --- Paper Summary & Question Inspection Drawer ---
  openSummaryDrawer(paperId: string, event?: Event): void {
    if (event) event.stopPropagation();
    this.selectedPaperId.set(paperId);
    this.paperDetail.set(null);
    this.activeTranslationJob.set(null);
    this.drawerOpen.set(true);
    this.loadingDetail.set(true);

    this.paperService.getPaper(paperId).subscribe({
      next: (detail) => {
        this.paperDetail.set(detail);
        this.loadingDetail.set(false);
      },
      error: (err) => {
        this.loadingDetail.set(false);
        this.snackBar.open(
          err?.error?.message || 'Failed to load paper details',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
    this.selectedPaperId.set(null);
    this.paperDetail.set(null);
  }

  // --- Approval & Encryption Actions ---
  approvePaper(paperId: string, event?: Event): void {
    if (event) event.stopPropagation();
    this.paperService.approvePaper(paperId).subscribe({
      next: () => {
        this.snackBar.open('Question Paper approved by Controller of Examinations!', 'OK', {
          duration: 3000,
        });
        this.loadPapers();
        if (this.selectedPaperId() === paperId) {
          this.openSummaryDrawer(paperId);
        }
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to approve paper',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  publishPaper(paperId: string, event?: Event): void {
    if (event) event.stopPropagation();
    this.paperService.publishPaper(paperId).subscribe({
      next: (res) => {
        this.snackBar.open(
          `Paper published & encrypted with Key ID: ${res.encryptionKeyId || 'KMS-AEAD-256'}`,
          'OK',
          { duration: 4000 }
        );
        this.loadPapers();
        if (this.selectedPaperId() === paperId) {
          this.openSummaryDrawer(paperId);
        }
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to encrypt and publish paper',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  // --- Indic AI Translation ---
  startTranslation(): void {
    const paperId = this.selectedPaperId();
    if (!paperId) return;

    const req: PaperTranslateRequest = {
      targetLanguage: this.transTargetLanguage,
      overwriteExisting: this.transOverwriteExisting,
      targetStatus: 'PUBLISHED',
    };

    this.isTranslating.set(true);
    this.paperService.startTranslation(paperId, req).subscribe({
      next: (job) => {
        this.activeTranslationJob.set(job);
        this.isTranslating.set(false);
        this.snackBar.open(
          `Translation job queued for ${this.getLanguageLabel(this.transTargetLanguage)}`,
          'OK',
          { duration: 3000 }
        );
        this.pollTranslationStatus(paperId);
      },
      error: (err) => {
        this.isTranslating.set(false);
        this.snackBar.open(
          err?.error?.message || 'Failed to initiate translation job',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  private pollTranslationStatus(paperId: string): void {
    const intervalId = setInterval(() => {
      if (!this.drawerOpen() || this.selectedPaperId() !== paperId) {
        clearInterval(intervalId);
        return;
      }
      this.paperService.getTranslationStatus(paperId).subscribe({
        next: (status) => {
          this.activeTranslationJob.set(status);
          if (status.status === 'COMPLETED' || status.status === 'FAILED') {
            clearInterval(intervalId);
            if (status.status === 'COMPLETED') {
              this.snackBar.open('Paper translation finished successfully!', 'OK', {
                duration: 3000,
              });
              this.openSummaryDrawer(paperId);
            }
          }
        },
        error: () => clearInterval(intervalId),
      });
    }, 2500);
  }

  getLanguageLabel(code: string): string {
    const l = this.supportedLanguages.find((lang) => lang.code === code);
    return l ? `${l.label} (${l.native})` : code;
  }

  getExamName(examId?: string): string {
    if (!examId) return 'N/A';
    const ex = (this.exams() || []).find((e) => e?.id === examId);
    return ex ? ex.name : examId;
  }
}
