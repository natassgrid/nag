import {
  Component,
  ChangeDetectionStrategy,
  inject,
  OnInit,
  signal,
  computed,
  ChangeDetectorRef,
  DestroyRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject as RxSubject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { PracticeSetService } from '../../services';
import { PracticeSet, CreatePracticeSetRequest, UpdatePracticeSetRequest } from '../../models';
import { PaperService, PaperSummary } from '@nag-frontend-workspace/examinations-data-access';
import {
  QuestionBankService,
  SubjectTopicService,
  Subject,
  Question,
  DifficultyLevel,
} from '@nag-frontend-workspace/questions-data-access';

export interface PracticeSetFormModalData {
  set: PracticeSet | null;
  mode: 'create' | 'edit';
  initialTab?: 'info' | 'curate';
}

@Component({
  selector: 'app-practice-set-form-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './practice-set-form-modal.component.html',
  styleUrls: ['./practice-set-form-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeSetFormModalComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialogRef = inject(MatDialogRef<PracticeSetFormModalComponent>);
  public readonly data = inject<PracticeSetFormModalData>(MAT_DIALOG_DATA);
  private readonly practiceSetService = inject(PracticeSetService);
  private readonly paperService = inject(PaperService);
  private readonly questionBankService = inject(QuestionBankService);
  private readonly subjectTopicService = inject(SubjectTopicService);

  form!: FormGroup;

  // Source selection: 'EXAM_CLONE' (Attach Practice Paper) or 'MANUAL' (Custom Curation)
  readonly sourceType = signal<'EXAM_CLONE' | 'MANUAL'>('EXAM_CLONE');
  readonly practicePapers = signal<PaperSummary[]>([]);
  readonly loadingPapers = signal<boolean>(false);
  readonly loadingPaperDetails = signal<boolean>(false);
  readonly selectedPaperId = signal<string | null>(null);
  readonly attachedQuestionIds = signal<string[]>([]);
  readonly isSubmitting = signal<boolean>(false);
  readonly isFormValid = signal<boolean>(false);
  readonly validationError = signal<string | null>(null);

  // Manual Question Curation State
  readonly curationTab = signal<'browse' | 'selected'>('browse');
  readonly questionSearchQuery = signal<string>('');
  readonly selectedDifficulty = signal<string>('ALL');
  readonly selectedSubject = signal<string>('ALL');
  readonly availableSubjects = signal<Subject[]>([]);
  readonly bankQuestions = signal<Question[]>([]);
  readonly loadingBankQuestions = signal<boolean>(false);
  readonly cachedQuestionsMap = signal<Map<string, Question>>(new Map());
  readonly bankPage = signal<number>(0);
  readonly bankPageSize = signal<number>(20);
  readonly bankTotal = signal<number>(0);
  readonly bankTotalPages = signal<number>(1);

  private readonly searchSubject$ = new RxSubject<string>();

  readonly totalQuestionsCount = computed(() => this.attachedQuestionIds().length);

  readonly canSubmit = computed(() => {
    if (this.isSubmitting()) return false;
    if (this.loadingPaperDetails()) return false;
    return this.isFormValid();
  });

  // Filtered bank questions (driven dynamically from database)
  readonly filteredBankQuestions = computed(() => this.bankQuestions());

  // Selected questions objects for display in the "Selected" tab
  readonly selectedQuestionsList = computed(() => {
    const ids = this.attachedQuestionIds();
    const map = this.cachedQuestionsMap();
    return ids.map((id) => {
      if (map.has(id)) {
        return map.get(id)!;
      }
      return {
        id,
        code: `Q-${id.substring(0, 8).toUpperCase()}`,
        content: `Question ID: ${id}`,
        difficulty: 'MEDIUM' as DifficultyLevel,
        subject: 'General',
        topic: '',
        type: 'SINGLE_MCQ',
        status: 'APPROVED',
        marks: 4,
        negativeMarks: 1,
        options: [],
        tags: [],
      } as Question;
    });
  });

  ngOnInit() {
    const initialName = this.data.set?.name || '';
    const initialDuration = this.data.set?.durationMinutes ?? 30;

    this.form = this.fb.group({
      name: [initialName, [Validators.required, Validators.maxLength(100)]],
      description: [this.data.set?.description || '', [Validators.maxLength(500)]],
      durationMinutes: [initialDuration, [Validators.required, Validators.min(1)]],
      subjectSlug: [this.data.set?.subjectSlug || ''],
    });

    this.isFormValid.set(this.form.valid);

    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.isFormValid.set(this.form.valid);
        if (this.form.valid) {
          this.validationError.set(null);
        }
        this.cdr.markForCheck();
      });

    this.form.statusChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.isFormValid.set(this.form.valid);
        this.cdr.markForCheck();
      });

    // Reactive debounced search to query DB dynamically
    this.searchSubject$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.bankPage.set(0);
        this.loadBankQuestions();
      });

    // Parse existing question IDs if present
    if (this.data.set?.questionIds) {
      const parsed = this.parseQuestionIds(this.data.set.questionIds);
      this.attachedQuestionIds.set(parsed);
      if (parsed.length > 0) {
        this.loadExistingAttachedQuestions(parsed);
      }
    }

    if (this.data.mode === 'create') {
      this.loadPracticePapers();
      this.loadSubjects();
      this.loadBankQuestions();
    } else {
      const setSource = this.data.set?.source === 'EXAM_CLONE' ? 'EXAM_CLONE' : 'MANUAL';
      this.sourceType.set(setSource);
      this.loadSubjects();
      this.loadBankQuestions();
      if (setSource === 'EXAM_CLONE') {
        this.loadPracticePapers();
      }
    }
  }

  private parseQuestionIds(raw: any): string[] {
    if (!raw) return [];
    if (Array.isArray(raw)) return raw.map(String);
    if (typeof raw === 'string') {
      try {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) return parsed.map(String);
      } catch {
        return [];
      }
    }
    return [];
  }

  loadPracticePapers() {
    this.loadingPapers.set(true);
    this.cdr.markForCheck();

    this.paperService.getPapers({ page: 0, size: 50, isPractice: true }).subscribe({
      next: (res) => {
        const papers = (res.content || []).filter((p) => p.isPractice !== false);
        this.practicePapers.set(papers.length > 0 ? papers : res.content || []);
        this.loadingPapers.set(false);
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.warn('Could not load practice papers from paper-generator:', err);
        this.loadingPapers.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  loadSubjects() {
    this.subjectTopicService.getSubjects().subscribe({
      next: (subs) => {
        this.availableSubjects.set(subs || []);
        // When editing, auto-detect and match subject from practice set
        if (this.data.mode === 'edit' && this.data.set && this.selectedSubject() === 'ALL') {
          const match = this.findMatchingSubject(this.data.set, subs || []);
          if (match) {
            this.selectedSubject.set(match.name);
            this.bankPage.set(0);
            this.loadBankQuestions();
          }
        }
        this.cdr.markForCheck();
      },
      error: () => this.availableSubjects.set([]),
    });
  }

  private findMatchingSubject(set: PracticeSet, subjects: Subject[]): Subject | undefined {
    if (!subjects || subjects.length === 0) return undefined;
    const targetSlug = (set.subjectSlug || '').toLowerCase().trim();
    const targetName = (set.name || '').toLowerCase().trim();

    return subjects.find((s) => {
      const subName = (s.name || '').toLowerCase().trim();
      const subCode = (s.code || '').toLowerCase().trim();

      if (targetSlug) {
        const subSlug = subName.replace(/[^a-z0-9]/g, '-');
        if (subSlug.includes(targetSlug) || targetSlug.includes(subSlug)) return true;
        if (subCode && targetSlug.includes(subCode)) return true;
      }
      if (targetName) {
        const words = subName.split(/[\s/&,-]+/).filter((w) => w.length > 3);
        const hasKeywordMatch = words.some((w) => targetName.includes(w));
        if (hasKeywordMatch) return true;
      }
      return false;
    });
  }

  private loadExistingAttachedQuestions(ids: string[]) {
    this.questionBankService.getQuestionsByIds(ids).subscribe({
      next: (questions) => {
        if (questions && questions.length > 0) {
          const map = new Map(this.cachedQuestionsMap());
          questions.forEach((q) => map.set(q.id, q));
          this.cachedQuestionsMap.set(map);
          this.cdr.markForCheck();
        }
      },
      error: (err) => {
        console.warn('Could not preload attached question details:', err);
      },
    });
  }

  loadBankQuestions() {
    this.loadingBankQuestions.set(true);
    this.cdr.markForCheck();

    const search = this.questionSearchQuery().trim();
    const subject = this.selectedSubject();
    const difficulty = this.selectedDifficulty();

    this.questionBankService
      .loadQuestions({
        page: this.bankPage(),
        size: this.bankPageSize(),
        search: search ? search : undefined,
        subject: subject !== 'ALL' ? subject : undefined,
        difficulty: (difficulty !== 'ALL' ? difficulty : undefined) as DifficultyLevel,
      })
      .subscribe({
        next: (res) => {
          const list = res.content || [];
          this.bankQuestions.set(list);
          this.bankTotal.set(res.totalElements ?? list.length);
          this.bankTotalPages.set(res.totalPages ?? 1);

          // Update cached map
          const map = new Map(this.cachedQuestionsMap());
          list.forEach((q) => map.set(q.id, q));
          this.cachedQuestionsMap.set(map);

          this.loadingBankQuestions.set(false);
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.warn('Could not load questions from question bank:', err);
          this.loadingBankQuestions.set(false);
          this.cdr.markForCheck();
        },
      });
  }

  onSourceTypeChange(type: 'EXAM_CLONE' | 'MANUAL') {
    this.sourceType.set(type);
    this.validationError.set(null);
    if (type === 'MANUAL') {
      this.selectedPaperId.set(null);
      if (this.bankQuestions().length === 0) {
        this.loadBankQuestions();
      }
    }
    this.cdr.markForCheck();
  }

  onPaperSelected(paperId: string) {
    this.validationError.set(null);
    if (!paperId) {
      this.selectedPaperId.set(null);
      this.attachedQuestionIds.set([]);
      this.cdr.markForCheck();
      return;
    }

    this.selectedPaperId.set(paperId);
    this.loadingPaperDetails.set(true);
    this.cdr.markForCheck();

    this.paperService.getPaper(paperId).subscribe({
      next: (paper) => {
        this.loadingPaperDetails.set(false);
        const questionIds: string[] = [];

        if (paper.questions && Array.isArray(paper.questions)) {
          paper.questions.forEach((q) => {
            const qid = q.questionId || (q as any).id;
            if (qid) questionIds.push(String(qid));
          });
        }

        if (questionIds.length === 0 && paper.paperDefinitionJson) {
          try {
            const def = JSON.parse(paper.paperDefinitionJson);
            if (Array.isArray(def.questionIds)) {
              def.questionIds.forEach((qid: any) => questionIds.push(String(qid)));
            }
          } catch (e) {
            console.warn('Failed to parse paperDefinitionJson', e);
          }
        }

        this.attachedQuestionIds.set(questionIds);

        // Auto-populate form fields if not already modified
        const currentName = this.form.get('name')?.value;
        if (!currentName || currentName.trim() === '') {
          this.form.patchValue({
            name: paper.name || paper.examName ? `Practice - ${paper.name || paper.examName}` : 'Practice Paper Set',
          });
        }

        if (paper.examName && !this.form.get('description')?.value) {
          this.form.patchValue({
            description: `Generated from official practice paper: ${paper.name || paper.examName}`,
          });
        }

        if (paper.shiftName && !this.form.get('subjectSlug')?.value) {
          this.form.patchValue({
            subjectSlug: paper.shiftName.toLowerCase().replace(/[^a-z0-9]/g, '-'),
          });
        }

        this.form.updateValueAndValidity();
        this.isFormValid.set(this.form.valid);
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to load paper details:', err);
        this.loadingPaperDetails.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  // --- Manual Question Curation Actions & Dynamic Filters ---

  onSearchChange(query: string) {
    this.questionSearchQuery.set(query);
    this.searchSubject$.next(query);
  }

  onSubjectChange(subject: string) {
    this.selectedSubject.set(subject);
    this.bankPage.set(0);
    this.loadBankQuestions();
  }

  onDifficultyChange(difficulty: string) {
    this.selectedDifficulty.set(difficulty);
    this.bankPage.set(0);
    this.loadBankQuestions();
  }

  onPageChange(page: number) {
    if (page >= 0 && page < this.bankTotalPages()) {
      this.bankPage.set(page);
      this.loadBankQuestions();
    }
  }

  onPageSizeChange(size: number) {
    this.bankPageSize.set(size);
    this.bankPage.set(0);
    this.loadBankQuestions();
  }

  isQuestionSelected(id: string): boolean {
    return this.attachedQuestionIds().includes(id);
  }

  toggleQuestionSelection(question: Question) {
    const current = [...this.attachedQuestionIds()];
    const index = current.indexOf(question.id);

    // Keep cached map updated
    const map = new Map(this.cachedQuestionsMap());
    map.set(question.id, question);
    this.cachedQuestionsMap.set(map);

    if (index >= 0) {
      current.splice(index, 1);
    } else {
      current.push(question.id);
      // Auto-suggest subject slug if not set
      if (!this.form.get('subjectSlug')?.value && question.subject) {
        this.form.patchValue({
          subjectSlug: question.subject.toLowerCase().replace(/[^a-z0-9]/g, '-'),
        });
      }
    }

    this.attachedQuestionIds.set(current);
    this.cdr.markForCheck();
  }

  selectAllFiltered() {
    const list = this.bankQuestions();
    const current = new Set(this.attachedQuestionIds());
    const map = new Map(this.cachedQuestionsMap());

    list.forEach((q) => {
      current.add(q.id);
      map.set(q.id, q);
    });

    this.cachedQuestionsMap.set(map);
    this.attachedQuestionIds.set(Array.from(current));
    this.cdr.markForCheck();
  }

  clearSelectedQuestions() {
    this.attachedQuestionIds.set([]);
    this.cdr.markForCheck();
  }

  removeQuestion(id: string) {
    const current = this.attachedQuestionIds().filter((qid) => qid !== id);
    this.attachedQuestionIds.set(current);
    this.cdr.markForCheck();
  }

  // --- Submission ---

  onSubmit() {
    if (this.isSubmitting() || this.loadingPaperDetails()) return;
    this.validationError.set(null);

    const formValue = this.form.value;
    const name = formValue.name?.trim();

    if (!name) {
      this.form.get('name')?.markAsTouched();
      this.validationError.set('Please enter a practice set name.');
      this.cdr.markForCheck();
      return;
    }

    const duration = Number(formValue.durationMinutes);
    if (!duration || duration < 1) {
      this.form.get('durationMinutes')?.markAsTouched();
      this.validationError.set('Duration must be at least 1 minute.');
      this.cdr.markForCheck();
      return;
    }

    if (this.data.mode === 'create' && this.sourceType() === 'EXAM_CLONE' && !this.selectedPaperId()) {
      this.validationError.set('Please select a generated practice paper to attach questions, or switch to Manual Curation.');
      this.cdr.markForCheck();
      return;
    }

    this.isSubmitting.set(true);
    this.cdr.markForCheck();

    const questionIds = this.attachedQuestionIds();

    if (this.data.mode === 'create') {
      const createReq: CreatePracticeSetRequest = {
        name: name,
        description: formValue.description || null,
        durationMinutes: duration,
        subjectSlug: formValue.subjectSlug || null,
        questionIds: questionIds,
        source: this.sourceType(),
        totalQuestions: questionIds.length,
      };

      this.practiceSetService.create(createReq).subscribe({
        next: (res) => {
          this.isSubmitting.set(false);
          this.dialogRef.close(res);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.validationError.set('Failed to create practice set. Please verify input data and try again.');
          this.cdr.markForCheck();
          console.error('Failed to create practice set', err);
        },
      });
    } else {
      const updateReq: UpdatePracticeSetRequest = {
        name: name,
        description: formValue.description || null,
        durationMinutes: duration,
        totalQuestions: questionIds.length || this.data.set?.totalQuestions || 0,
        subjectSlug: formValue.subjectSlug || null,
        questionIds: questionIds,
      };

      this.practiceSetService.update(this.data.set!.id, updateReq).subscribe({
        next: (res) => {
          this.isSubmitting.set(false);
          this.dialogRef.close(res);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.validationError.set('Failed to update practice set. Please try again.');
          this.cdr.markForCheck();
          console.error('Failed to update practice set', err);
        },
      });
    }
  }

  onCancel() {
    this.dialogRef.close();
  }
}
