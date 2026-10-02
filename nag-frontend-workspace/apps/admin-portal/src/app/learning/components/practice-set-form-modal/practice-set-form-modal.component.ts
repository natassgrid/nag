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

  readonly totalQuestionsCount = computed(() => this.attachedQuestionIds().length);

  readonly canSubmit = computed(() => {
    if (this.isSubmitting()) return false;
    if (this.loadingPaperDetails()) return false;
    return this.isFormValid();
  });

  // Filtered bank questions for display
  readonly filteredBankQuestions = computed(() => {
    const list = this.bankQuestions();
    const query = this.questionSearchQuery().trim().toLowerCase();
    const diff = this.selectedDifficulty();
    const subj = this.selectedSubject();

    return list.filter((q) => {
      const matchesDiff = diff === 'ALL' || q.difficulty === diff;
      const matchesSubj = subj === 'ALL' || q.subject === subj;
      const matchesQuery =
        !query ||
        (q.content && q.content.toLowerCase().includes(query)) ||
        (q.code && q.code.toLowerCase().includes(query)) ||
        (q.topic && q.topic.toLowerCase().includes(query));

      return matchesDiff && matchesSubj && matchesQuery;
    });
  });

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

    // Parse existing question IDs if present
    if (this.data.set?.questionIds) {
      const parsed = this.parseQuestionIds(this.data.set.questionIds);
      this.attachedQuestionIds.set(parsed);
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
        this.cdr.markForCheck();
      },
      error: () => this.availableSubjects.set([]),
    });
  }

  loadBankQuestions() {
    this.loadingBankQuestions.set(true);
    this.cdr.markForCheck();

    this.questionBankService
      .loadQuestions({
        page: 0,
        size: 100,
      })
      .subscribe({
        next: (res) => {
          const list = res.content || [];
          this.bankQuestions.set(list);

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

  // --- Manual Question Curation Actions ---

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
    const filtered = this.filteredBankQuestions();
    const current = new Set(this.attachedQuestionIds());
    const map = new Map(this.cachedQuestionsMap());

    filtered.forEach((q) => {
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
