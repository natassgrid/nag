import { Component, ChangeDetectionStrategy, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '@nag-frontend-workspace/shared-ui-components';
import { Subject, SubjectTopicService, Subtopic, Topic } from '@nag-frontend-workspace/questions-data-access';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import { FormulaEntry, MicroLessonStep, QuickCheckItem, TrapAlert } from '../../models/micro-lesson.model';
import {
  DEFAULT_CORE_CONCEPT, DEFAULT_EXAM_OPTIONS, DEFAULT_FINAL_ANSWER, DEFAULT_FORMULA_ENTRIES,
  DEFAULT_KEY_TAKEAWAYS, DEFAULT_PROBLEM_STATEMENT, DEFAULT_QUICK_CHECKS,
  DEFAULT_STEP_ITEMS, DEFAULT_TRAP_ALERT, TargetExamOption,
  buildCreateOrUpdateRequest, parseLessonForAuthoring,
} from './models';
import {
  CoreConceptEditorComponent, FormulaSheetBuilderComponent, LessonLivePreviewComponent,
  LessonMetadataBarComponent, QuickCheckBuilderComponent, StepBreakdownBuilderComponent, TrapAlertEditorComponent,
} from './components';

@Component({
  selector: 'app-micro-lesson-authoring',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterModule, MatButtonModule, MatIconModule, PageHeaderComponent,
    LessonMetadataBarComponent, CoreConceptEditorComponent, TrapAlertEditorComponent,
    FormulaSheetBuilderComponent, StepBreakdownBuilderComponent, QuickCheckBuilderComponent, LessonLivePreviewComponent,
  ],
  templateUrl: './micro-lesson-authoring.component.html',
  styleUrl: './micro-lesson-authoring.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonAuthoringComponent implements OnInit {
  private readonly microLessonService = inject(MicroLessonAdminService);
  private readonly subjectTopicService = inject(SubjectTopicService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly editingLessonId = signal<string | null>(null);
  readonly loadingLesson = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly feedback = signal<{ type: 'success' | 'error'; message: string } | null>(null);

  readonly subjects = signal<Subject[]>([]);
  readonly topics = signal<Topic[]>([]);
  readonly subtopics = signal<Subtopic[]>([]);
  readonly selectedSubjectId = signal<number | null>(null);
  readonly selectedTopicId = signal<number | null>(null);
  readonly selectedSubtopicId = signal<number | null>(null);
  readonly customSubtopicName = signal<string>('');

  readonly examOptions = signal<TargetExamOption[]>(DEFAULT_EXAM_OPTIONS);
  readonly targetExam = signal<string>('JEE_MAIN');
  readonly status = signal<string>('APPROVED');

  readonly coreConcept = signal<string>(DEFAULT_CORE_CONCEPT);
  readonly trapAlert = signal<TrapAlert>({ ...DEFAULT_TRAP_ALERT });
  readonly formulaEntries = signal<FormulaEntry[]>([...DEFAULT_FORMULA_ENTRIES]);
  readonly problemStatement = signal<string>(DEFAULT_PROBLEM_STATEMENT);
  readonly steps = signal<MicroLessonStep[]>([...DEFAULT_STEP_ITEMS]);
  readonly finalAnswer = signal<string>(DEFAULT_FINAL_ANSWER);
  readonly keyTakeaways = signal<string[]>([...DEFAULT_KEY_TAKEAWAYS]);
  readonly quickCheckItems = signal<QuickCheckItem[]>([...DEFAULT_QUICK_CHECKS]);
  readonly reviewerNotes = signal<string>('');

  readonly resolvedSubjectName = computed(() => this.subjects().find((s) => s.id === this.selectedSubjectId())?.name || 'General');
  readonly resolvedTopicName = computed(() => this.topics().find((t) => t.id === this.selectedTopicId())?.name || 'General');
  readonly resolvedSubtopicName = computed(() => {
    const custom = this.customSubtopicName().trim();
    return custom || this.subtopics().find((st) => st.id === this.selectedSubtopicId())?.name || 'General';
  });

  ngOnInit(): void {
    const editId = this.route.snapshot?.queryParamMap?.get('edit');
    if (editId) {
      this.editingLessonId.set(editId);
      this.loadLessonForEdit(editId);
    }
    this.loadTaxonomy(!!editId);
    this.subjectTopicService.getExaminations().subscribe({
      next: (exams) => {
        if (exams?.length) this.examOptions.set(exams.map((e) => ({ label: `${e.name} (${e.code})`, value: e.code })));
      },
      error: () => {},
    });
  }

  loadTaxonomy(isEditMode: boolean): void {
    this.subjectTopicService.getSubjects().subscribe({
      next: (subs) => {
        this.subjects.set(subs || []);
        if (!isEditMode && subs?.length) this.onSubjectChange(subs[0].id);
        else if (isEditMode && this.selectedSubjectId()) this.loadCascadingForEdit(this.selectedSubjectId()!, this.selectedTopicId());
      },
      error: () => {},
    });
  }

  loadLessonForEdit(id: string): void {
    this.loadingLesson.set(true);
    this.feedback.set(null);
    this.microLessonService.getMicroLessonById(id).subscribe({
      next: (lesson) => {
        this.loadingLesson.set(false);
        const p = parseLessonForAuthoring(lesson);
        this.targetExam.set(p.targetExam);
        this.status.set(p.status);
        this.customSubtopicName.set(p.subtopicCustomName);
        this.coreConcept.set(p.coreConcept);
        this.trapAlert.set(p.trapAlert);
        this.formulaEntries.set(p.formulaEntries);
        this.problemStatement.set(p.problemStatement);
        this.steps.set(p.steps);
        this.finalAnswer.set(p.finalAnswer);
        this.keyTakeaways.set(p.keyTakeaways);
        this.quickCheckItems.set(p.quickCheckItems);
        this.reviewerNotes.set(p.reviewerNotes);
        if (lesson.subjectId) this.selectedSubjectId.set(Number(lesson.subjectId));
        if (lesson.topicId) this.selectedTopicId.set(Number(lesson.topicId));
        if (lesson.subtopicId) this.selectedSubtopicId.set(Number(lesson.subtopicId));
        if (this.selectedSubjectId()) this.loadCascadingForEdit(this.selectedSubjectId()!, this.selectedTopicId());
      },
      error: (err) => {
        this.loadingLesson.set(false);
        this.feedback.set({ type: 'error', message: err?.error?.message || `Could not load micro-lesson ${id}.` });
      },
    });
  }

  private loadCascadingForEdit(subjectId: number, topicId: number | null): void {
    this.subjectTopicService.getTopics(subjectId).subscribe({
      next: (tops) => {
        this.topics.set(tops || []);
        if (topicId) this.subjectTopicService.getSubtopics(subjectId, topicId).subscribe({ next: (subs) => this.subtopics.set(subs || []) });
      },
    });
  }

  onSubjectChange(subjectId: number): void {
    this.selectedSubjectId.set(subjectId);
    this.selectedTopicId.set(null);
    this.selectedSubtopicId.set(null);
    this.topics.set([]);
    this.subtopics.set([]);
    if (subjectId) {
      this.subjectTopicService.getTopics(subjectId).subscribe({
        next: (tops) => {
          this.topics.set(tops || []);
          if (tops?.length) this.onTopicChange(tops[0].id);
        },
      });
    }
  }

  onTopicChange(topicId: number): void {
    this.selectedTopicId.set(topicId);
    this.selectedSubtopicId.set(null);
    this.subtopics.set([]);
    const subjectId = this.selectedSubjectId();
    if (subjectId && topicId) {
      this.subjectTopicService.getSubtopics(subjectId, topicId).subscribe({
        next: (subs) => {
          this.subtopics.set(subs || []);
          if (subs?.length) {
            this.selectedSubtopicId.set(subs[0].id);
            this.customSubtopicName.set(subs[0].name);
          }
        },
      });
    }
  }

  onSubtopicChange(subtopicId: number | null): void {
    this.selectedSubtopicId.set(subtopicId);
    const found = this.subtopics().find((s) => s.id === subtopicId);
    if (found) this.customSubtopicName.set(found.name);
  }

  createQuickSubject(name: string): void {
    this.subjectTopicService.createSubject({ name }).subscribe({
      next: (sub) => { this.subjects.update((list) => [...list, sub]); this.onSubjectChange(sub.id); },
    });
  }

  createQuickTopic(name: string): void {
    const subjId = this.selectedSubjectId();
    if (!subjId) return;
    this.subjectTopicService.createTopic(subjId, { name }).subscribe({
      next: (top) => { this.topics.update((list) => [...list, top]); this.onTopicChange(top.id); },
    });
  }

  resetForm(): void {
    this.coreConcept.set('');
    this.trapAlert.set({ title: '', description: '', commonMistake: '', correctApproach: '' });
    this.formulaEntries.set([{ name: 'Formula 1', latex: '', description: '', variables: [] }]);
    this.problemStatement.set('');
    this.steps.set([{ stepNumber: 1, title: 'Step 1', explanation: '', formula: '', tip: '' }]);
    this.finalAnswer.set('');
    this.keyTakeaways.set(['']);
    this.quickCheckItems.set([{ question: '', options: [], answer: '', explanation: '' }]);
    this.feedback.set(null);
  }

  saveLesson(): void {
    if (!this.coreConcept().trim()) {
      this.feedback.set({ type: 'error', message: 'Core Concept explanation is required.' });
      return;
    }
    const payload = buildCreateOrUpdateRequest({
      subjectId: this.selectedSubjectId(), subjectName: this.resolvedSubjectName(),
      topicId: this.selectedTopicId(), topicName: this.resolvedTopicName(),
      subtopicId: this.selectedSubtopicId(), subtopicName: this.resolvedSubtopicName(),
      targetExam: this.targetExam(), status: this.status(), coreConcept: this.coreConcept(),
      trapAlert: this.trapAlert(), formulaEntries: this.formulaEntries(),
      problemStatement: this.problemStatement(), steps: this.steps(), finalAnswer: this.finalAnswer(),
      keyTakeaways: this.keyTakeaways(), quickCheckItems: this.quickCheckItems(), reviewerNotes: this.reviewerNotes(),
    });
    this.saving.set(true);
    this.feedback.set(null);
    const editId = this.editingLessonId();
    const req$ = editId ? this.microLessonService.updateMicroLesson(editId, payload) : this.microLessonService.createMicroLesson(payload);
    req$.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.feedback.set({ type: 'success', message: `Micro-lesson "${saved.subtopicName || saved.id}" ${editId ? 'updated' : 'created'} successfully!` });
      },
      error: (err) => {
        this.saving.set(false);
        this.feedback.set({ type: 'error', message: err?.error?.message || 'Failed to save micro-lesson. Verify KaTeX syntax and retry.' });
      },
    });
  }

  backToModerationDesk(): void {
    this.router.navigate(['/learning/micro-lessons']);
  }
}
