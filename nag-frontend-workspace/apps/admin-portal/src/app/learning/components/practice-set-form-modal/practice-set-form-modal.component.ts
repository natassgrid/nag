import { Component, ChangeDetectionStrategy, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PracticeSetService } from '../../services';
import { PracticeSet, CreatePracticeSetRequest, UpdatePracticeSetRequest } from '../../models';
import { PaperService, PaperSummary } from '@nag-frontend-workspace/examinations-data-access';

export interface PracticeSetFormModalData {
  set: PracticeSet | null;
  mode: 'create' | 'edit';
}

@Component({
  selector: 'app-practice-set-form-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatRadioModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './practice-set-form-modal.component.html',
  styleUrls: ['./practice-set-form-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeSetFormModalComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<PracticeSetFormModalComponent>);
  public readonly data = inject<PracticeSetFormModalData>(MAT_DIALOG_DATA);
  private readonly practiceSetService = inject(PracticeSetService);
  private readonly paperService = inject(PaperService);

  form!: FormGroup;

  // Source selection: 'EXAM_CLONE' (Attach Practice Paper) or 'MANUAL' (Custom)
  readonly sourceType = signal<'EXAM_CLONE' | 'MANUAL'>('EXAM_CLONE');
  readonly practicePapers = signal<PaperSummary[]>([]);
  readonly loadingPapers = signal<boolean>(false);
  readonly loadingPaperDetails = signal<boolean>(false);
  readonly selectedPaperId = signal<string | null>(null);
  readonly attachedQuestionIds = signal<string[]>([]);
  readonly isSubmitting = signal<boolean>(false);

  readonly totalQuestionsCount = computed(() => this.attachedQuestionIds().length);

  ngOnInit() {
    this.form = this.fb.group({
      name: [this.data.set?.name || '', [Validators.required, Validators.maxLength(100)]],
      description: [this.data.set?.description || '', [Validators.maxLength(500)]],
      durationMinutes: [this.data.set?.durationMinutes || 30, [Validators.required, Validators.min(1)]],
      subjectSlug: [this.data.set?.subjectSlug || ''],
    });

    if (this.data.mode === 'create') {
      this.loadPracticePapers();
    } else if (this.data.set?.source) {
      this.sourceType.set(this.data.set.source === 'EXAM_CLONE' ? 'EXAM_CLONE' : 'MANUAL');
    }
  }

  loadPracticePapers() {
    this.loadingPapers.set(true);
    this.paperService.getPapers({ page: 0, size: 50, isPractice: true }).subscribe({
      next: (res) => {
        // Include papers where isPractice is true or any generated paper available
        const papers = (res.content || []).filter((p) => p.isPractice !== false);
        this.practicePapers.set(papers);
        this.loadingPapers.set(false);
      },
      error: (err) => {
        console.warn('Could not load practice papers from paper-generator:', err);
        this.loadingPapers.set(false);
      },
    });
  }

  onSourceTypeChange(type: 'EXAM_CLONE' | 'MANUAL') {
    this.sourceType.set(type);
    if (type === 'MANUAL') {
      this.selectedPaperId.set(null);
      this.attachedQuestionIds.set([]);
    }
  }

  onPaperSelected(paperId: string) {
    if (!paperId) {
      this.selectedPaperId.set(null);
      this.attachedQuestionIds.set([]);
      return;
    }

    this.selectedPaperId.set(paperId);
    this.loadingPaperDetails.set(true);

    this.paperService.getPaper(paperId).subscribe({
      next: (paper) => {
        this.loadingPaperDetails.set(false);
        const questionIds: string[] = [];

        if (paper.questions && Array.isArray(paper.questions)) {
          paper.questions.forEach((q) => {
            if (q.id) questionIds.push(String(q.id));
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

        // Auto-populate form fields if not manually modified
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
      },
      error: (err) => {
        console.error('Failed to load paper details:', err);
        this.loadingPaperDetails.set(false);
      },
    });
  }

  onSubmit() {
    if (this.form.invalid || this.isSubmitting()) return;

    this.isSubmitting.set(true);
    const formValue = this.form.value;

    if (this.data.mode === 'create') {
      const createReq: CreatePracticeSetRequest = {
        name: formValue.name,
        description: formValue.description || null,
        durationMinutes: formValue.durationMinutes,
        subjectSlug: formValue.subjectSlug || null,
        questionIds: this.attachedQuestionIds(),
        source: this.sourceType(),
        totalQuestions: this.attachedQuestionIds().length,
      };

      this.practiceSetService.create(createReq).subscribe({
        next: (res) => {
          this.isSubmitting.set(false);
          this.dialogRef.close(res);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          console.error('Failed to create practice set', err);
        },
      });
    } else {
      const updateReq: UpdatePracticeSetRequest = {
        name: formValue.name,
        description: formValue.description || null,
        durationMinutes: formValue.durationMinutes,
        totalQuestions: this.data.set?.totalQuestions || this.attachedQuestionIds().length,
        subjectSlug: formValue.subjectSlug || null,
      };

      this.practiceSetService.update(this.data.set!.id, updateReq).subscribe({
        next: (res) => {
          this.isSubmitting.set(false);
          this.dialogRef.close(res);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          console.error('Failed to update practice set', err);
        },
      });
    }
  }

  onCancel() {
    this.dialogRef.close();
  }
}
