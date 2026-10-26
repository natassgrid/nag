import { Component, ChangeDetectionStrategy, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  ExternalPromptService,
  ImportedQuestion,
  QuestionPromptConfig,
} from '../../services/external-prompt.service';
import {
  DifficultyLevel,
  QuestionType,
  SubjectHierarchy,
  SubjectTopicService,
  TargetExaminationOption,
} from '@nag-frontend-workspace/questions-data-access';

export interface QuestionPromptImportDialogData {
  subject?: string;
  topic?: string;
  subtopic?: string;
  subjectId?: number;
  topicId?: number;
  subtopicId?: number;
  targetExam?: string;
  questionType?: QuestionType | string;
  difficulty?: DifficultyLevel | string;
  cognitiveLevel?: string;
  marks?: number;
  negativeMarks?: number;
  activeTab?: 'prompt' | 'import';
}

export interface PromptImportTaxonomySelection {
  subjectId: number | null;
  topicId: number | null;
  subtopicId: number | null;
  subjectName: string;
  topicName: string;
  subtopicName: string;
  isCustomSubject: boolean;
  isCustomTopic: boolean;
  isCustomSubtopic: boolean;
}

export interface QuestionPromptImportDialogResult {
  action: 'import' | 'close';
  importedQuestion?: ImportedQuestion;
  taxonomy: PromptImportTaxonomySelection;
}

@Component({
  selector: 'nag-question-prompt-import-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './question-prompt-import-dialog.component.html',
  styleUrls: ['./question-prompt-import-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionPromptImportDialogComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<QuestionPromptImportDialogComponent>);
  private readonly promptService = inject(ExternalPromptService);
  private readonly subjectTopicService = inject(SubjectTopicService);
  readonly data: QuestionPromptImportDialogData = inject(MAT_DIALOG_DATA, { optional: true }) || {};

  readonly activeTab = signal<'prompt' | 'import'>('prompt');

  // Taxonomy Dynamic State
  readonly hierarchy = signal<SubjectHierarchy[]>([]);
  readonly loadingTaxonomy = signal<boolean>(false);

  readonly selectedSubjectId = signal<number | null>(null);
  readonly isCustomSubject = signal<boolean>(false);
  readonly customSubjectName = signal<string>('');

  readonly selectedTopicId = signal<number | null>(null);
  readonly isCustomTopic = signal<boolean>(false);
  readonly customTopicName = signal<string>('');

  readonly selectedSubtopicId = signal<number | null>(null);
  readonly isCustomSubtopic = signal<boolean>(false);
  readonly customSubtopicName = signal<string>('');

  // Examinations Dynamic State
  readonly examOptions = signal<{ label: string; value: string }[]>([
    { label: 'JEE Main (JEE_MAIN)', value: 'JEE_MAIN' },
    { label: 'JEE Advanced (JEE_ADVANCED)', value: 'JEE_ADVANCED' },
    { label: 'NEET-UG Medical (NEET)', value: 'NEET' },
    { label: 'GATE Engineering (GATE)', value: 'GATE' },
    { label: 'UPSC Civil Services (UPSC_CSAT)', value: 'UPSC_CSAT' },
    { label: 'SSC CGL (SSC_CGL)', value: 'SSC_CGL' },
    { label: 'General Competitive Exam (GENERAL)', value: 'GENERAL' },
  ]);
  readonly targetExam = signal<string>('JEE_MAIN');

  // Question Configuration Signals
  readonly questionType = signal<string>('SINGLE_MCQ');
  readonly difficulty = signal<string>('MEDIUM');
  readonly cognitiveLevel = signal<string>('UNDERSTAND');
  readonly marks = signal<number>(4);
  readonly negativeMarks = signal<number>(1);
  readonly additionalInstructions = signal<string>('');

  readonly copied = signal<boolean>(false);

  // Import signals
  readonly jsonInput = signal<string>('');
  readonly validationErrors = signal<string[]>([]);
  readonly validationWarnings = signal<string[]>([]);

  // Computed cascading taxonomy options
  readonly availableTopics = computed(() => {
    const subjectId = this.selectedSubjectId();
    if (!subjectId) return [];
    const subj = this.hierarchy().find((s) => s.id === subjectId);
    return subj?.topics || [];
  });

  readonly availableSubtopics = computed(() => {
    const topicId = this.selectedTopicId();
    if (!topicId) return [];
    const topic = this.availableTopics().find((t) => t.id === topicId);
    return topic?.subtopics || [];
  });

  // Resolved taxonomy names
  readonly resolvedSubjectName = computed(() => {
    if (this.isCustomSubject()) return this.customSubjectName().trim() || 'General Science';
    const subj = this.hierarchy().find((s) => s.id === this.selectedSubjectId());
    return subj?.name || this.customSubjectName().trim() || 'General Science';
  });

  readonly resolvedTopicName = computed(() => {
    if (this.isCustomTopic()) return this.customTopicName().trim() || 'Core Topic';
    const top = this.availableTopics().find((t) => t.id === this.selectedTopicId());
    return top?.name || this.customTopicName().trim() || 'Core Topic';
  });

  readonly resolvedSubtopicName = computed(() => {
    if (this.isCustomSubtopic()) return this.customSubtopicName().trim() || 'Key Sub-topic';
    const sub = this.availableSubtopics().find((s) => s.id === this.selectedSubtopicId());
    return sub?.name || this.customSubtopicName().trim() || 'Key Sub-topic';
  });

  readonly generatedPrompt = computed(() => {
    const config: QuestionPromptConfig = {
      subject: this.resolvedSubjectName(),
      topic: this.resolvedTopicName(),
      subtopic: this.resolvedSubtopicName(),
      targetExam: this.targetExam(),
      questionType: this.questionType(),
      difficulty: this.difficulty(),
      cognitiveLevel: this.cognitiveLevel(),
      marks: this.marks(),
      negativeMarks: this.negativeMarks(),
      additionalInstructions: this.additionalInstructions(),
    };
    return this.promptService.generateQuestionPrompt(config);
  });

  getCurrentTaxonomy(): PromptImportTaxonomySelection {
    return {
      subjectId: this.isCustomSubject() ? null : this.selectedSubjectId(),
      topicId: this.isCustomTopic() ? null : this.selectedTopicId(),
      subtopicId: this.isCustomSubtopic() ? null : this.selectedSubtopicId(),
      subjectName: this.resolvedSubjectName(),
      topicName: this.resolvedTopicName(),
      subtopicName: this.resolvedSubtopicName(),
      isCustomSubject: this.isCustomSubject(),
      isCustomTopic: this.isCustomTopic(),
      isCustomSubtopic: this.isCustomSubtopic(),
    };
  }

  ngOnInit(): void {
    if (this.data.activeTab) {
      this.activeTab.set(this.data.activeTab);
    }
    if (this.data.subjectId) this.selectedSubjectId.set(this.data.subjectId);
    if (this.data.topicId) this.selectedTopicId.set(this.data.topicId);
    if (this.data.subtopicId) this.selectedSubtopicId.set(this.data.subtopicId);
    if (this.data.subject) this.customSubjectName.set(this.data.subject);
    if (this.data.topic) this.customTopicName.set(this.data.topic);
    if (this.data.subtopic) this.customSubtopicName.set(this.data.subtopic);
    if (this.data.targetExam) this.targetExam.set(this.data.targetExam);
    if (this.data.questionType) {
      const q = this.data.questionType;
      this.questionType.set(
        q === 'MULTIPLE_CHOICE' ? 'SINGLE_MCQ' : q === 'MULTIPLE_SELECT' ? 'MULTI_MCQ' : q
      );
    }
    if (this.data.difficulty) this.difficulty.set(this.data.difficulty);
    if (this.data.cognitiveLevel) this.cognitiveLevel.set(this.data.cognitiveLevel);
    if (this.data.marks) this.marks.set(this.data.marks);
    if (this.data.negativeMarks !== undefined) this.negativeMarks.set(this.data.negativeMarks);

    if (this.dialogRef.backdropClick) {
      this.dialogRef.backdropClick().subscribe(() => this.close());
    }
    if (this.dialogRef.keydownEvents) {
      this.dialogRef.keydownEvents().subscribe((event) => {
        if (event.key === 'Escape') {
          this.close();
        }
      });
    }

    this.loadTaxonomy();
    this.loadExaminations();
  }

  loadTaxonomy(): void {
    this.loadingTaxonomy.set(true);
    this.subjectTopicService.getHierarchy().subscribe({
      next: (tree) => {
        const list = tree || [];
        this.hierarchy.set(list);
        this.loadingTaxonomy.set(false);

        if (list.length > 0) {
          // Attempt to match initial subject by ID or name
          let matchedSubj = this.data.subjectId
            ? list.find((s) => s.id === this.data.subjectId)
            : undefined;

          if (!matchedSubj && this.data.subject) {
            matchedSubj = list.find(
              (s) => s.name.toLowerCase() === this.data.subject?.toLowerCase()
            );
          }

          if (matchedSubj) {
            this.selectedSubjectId.set(matchedSubj.id);
            this.isCustomSubject.set(false);

            // Attempt to match topic
            let matchedTopic = this.data.topicId
              ? matchedSubj.topics.find((t) => t.id === this.data.topicId)
              : undefined;

            if (!matchedTopic && this.data.topic) {
              matchedTopic = matchedSubj.topics.find(
                (t) => t.name.toLowerCase() === this.data.topic?.toLowerCase()
              );
            }

            if (matchedTopic) {
              this.selectedTopicId.set(matchedTopic.id);
              this.isCustomTopic.set(false);

              // Attempt to match subtopic
              let matchedSubtop = this.data.subtopicId
                ? matchedTopic.subtopics.find((st) => st.id === this.data.subtopicId)
                : undefined;

              if (!matchedSubtop && this.data.subtopic) {
                matchedSubtop = matchedTopic.subtopics.find(
                  (st) => st.name.toLowerCase() === this.data.subtopic?.toLowerCase()
                );
              }

              if (matchedSubtop) {
                this.selectedSubtopicId.set(matchedSubtop.id);
                this.isCustomSubtopic.set(false);
              } else if (this.data.subtopic) {
                this.isCustomSubtopic.set(true);
                this.customSubtopicName.set(this.data.subtopic);
              } else if (matchedTopic.subtopics?.length > 0) {
                this.selectedSubtopicId.set(matchedTopic.subtopics[0].id);
                this.isCustomSubtopic.set(false);
              } else {
                this.isCustomSubtopic.set(true);
              }
            } else if (this.data.topic) {
              this.isCustomTopic.set(true);
              this.customTopicName.set(this.data.topic);
              if (this.data.subtopic) {
                this.isCustomSubtopic.set(true);
                this.customSubtopicName.set(this.data.subtopic);
              }
            } else if (matchedSubj.topics?.length > 0) {
              this.selectedTopicId.set(matchedSubj.topics[0].id);
              this.isCustomTopic.set(false);
              if (matchedSubj.topics[0].subtopics?.length > 0) {
                this.selectedSubtopicId.set(matchedSubj.topics[0].subtopics[0].id);
                this.isCustomSubtopic.set(false);
              } else {
                this.isCustomSubtopic.set(true);
              }
            } else {
              this.isCustomTopic.set(true);
              this.isCustomSubtopic.set(true);
            }
          } else if (!this.data.subject && !this.data.subjectId) {
            // Default to first subject
            this.onSubjectSelect(String(list[0].id));
          } else {
            // Keep provided custom subject
            this.isCustomSubject.set(true);
            if (this.data.subject) this.customSubjectName.set(this.data.subject);
            this.isCustomTopic.set(true);
            if (this.data.topic) this.customTopicName.set(this.data.topic);
            this.isCustomSubtopic.set(true);
            if (this.data.subtopic) this.customSubtopicName.set(this.data.subtopic);
          }
        } else {
          this.isCustomSubject.set(true);
          this.isCustomTopic.set(true);
          this.isCustomSubtopic.set(true);
        }
      },
      error: () => {
        this.loadingTaxonomy.set(false);
        this.isCustomSubject.set(true);
        this.isCustomTopic.set(true);
        this.isCustomSubtopic.set(true);
      },
    });
  }

  loadExaminations(): void {
    this.subjectTopicService.getExaminations().subscribe({
      next: (exams: TargetExaminationOption[]) => {
        if (exams && exams.length > 0) {
          const mapped = exams.map((e) => ({
            label: `${e.name} (${e.code})`,
            value: e.code,
          }));
          this.examOptions.set(mapped);
          if (!this.data.targetExam) {
            this.targetExam.set(mapped[0].value);
          }
        }
      },
      error: () => {},
    });
  }

  onSubjectSelect(val: string): void {
    if (val === '__CUSTOM__') {
      this.isCustomSubject.set(true);
      this.selectedSubjectId.set(null);
      this.isCustomTopic.set(true);
      this.selectedTopicId.set(null);
      this.isCustomSubtopic.set(true);
      this.selectedSubtopicId.set(null);
    } else {
      const id = Number(val);
      this.isCustomSubject.set(false);
      this.selectedSubjectId.set(id);

      const subj = this.hierarchy().find((s) => s.id === id);
      const tops = subj?.topics || [];
      if (tops.length > 0) {
        this.onTopicSelect(String(tops[0].id));
      } else {
        this.isCustomTopic.set(true);
        this.selectedTopicId.set(null);
        this.isCustomSubtopic.set(true);
        this.selectedSubtopicId.set(null);
      }
    }
  }

  onTopicSelect(val: string): void {
    if (val === '__CUSTOM__') {
      this.isCustomTopic.set(true);
      this.selectedTopicId.set(null);
      this.isCustomSubtopic.set(true);
      this.selectedSubtopicId.set(null);
    } else {
      const id = Number(val);
      this.isCustomTopic.set(false);
      this.selectedTopicId.set(id);

      const top = this.availableTopics().find((t) => t.id === id);
      const subs = top?.subtopics || [];
      if (subs.length > 0) {
        this.onSubtopicSelect(String(subs[0].id));
      } else {
        this.isCustomSubtopic.set(true);
        this.selectedSubtopicId.set(null);
      }
    }
  }

  onSubtopicSelect(val: string): void {
    if (val === '__CUSTOM__') {
      this.isCustomSubtopic.set(true);
      this.selectedSubtopicId.set(null);
    } else {
      const id = Number(val);
      this.isCustomSubtopic.set(false);
      this.selectedSubtopicId.set(id);
    }
  }

  setTab(tab: 'prompt' | 'import'): void {
    this.activeTab.set(tab);
  }

  async copyPrompt(): Promise<void> {
    const success = await this.promptService.copyToClipboard(this.generatedPrompt());
    if (success) {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2500);
    }
  }

  downloadPrompt(): void {
    const subjClean = (this.resolvedSubjectName() || 'general').replace(/[^a-zA-Z0-9_-]/g, '_').toLowerCase();
    const typeClean = this.questionType().toLowerCase();
    const filename = `prompt-question-${subjClean}-${typeClean}.txt`;
    this.promptService.downloadFile(filename, this.generatedPrompt());
  }

  loadSampleJson(): void {
    const isMulti = this.questionType() === 'MULTI_MCQ' || this.questionType() === 'MULTIPLE_SELECT';
    const sample = isMulti
      ? {
          content: 'Which of the following physical quantities are **vectors** and conserve under spatial rotations?',
          questionType: 'MULTI_MCQ',
          difficulty: 'MEDIUM',
          cognitiveLevel: 'ANALYZE',
          marks: 4,
          negativeMarks: 1,
          options: [
            { id: 'A', text: 'Linear Momentum $$\\vec{p} = m\\vec{v}$$', isCorrect: true },
            { id: 'B', text: 'Kinetic Energy $$K = \\frac{1}{2}mv^2$$', isCorrect: false },
            { id: 'C', text: 'Angular Momentum $$\\vec{L} = \\vec{r} \\times \\vec{p}$$', isCorrect: true },
            { id: 'D', text: 'Electrostatic Potential $$V$$', isCorrect: false },
          ],
          answerKey: 'A,C',
          explanation: 'Linear momentum and angular momentum are true vectors, whereas kinetic energy and electric potential are scalars.',
        }
      : {
          content: 'A block of mass $$m = 4\\text{ kg}$$ is placed on a frictionless horizontal plane and accelerated by a force $$F = 20\\text{ N}$$. What is its velocity after $$t = 3\\text{ s}$$ starting from rest?',
          questionType: 'SINGLE_MCQ',
          difficulty: 'MEDIUM',
          cognitiveLevel: 'APPLY',
          marks: 4,
          negativeMarks: 1,
          options: [
            { id: 'A', text: '$$10\\text{ m/s}$$', isCorrect: false },
            { id: 'B', text: '$$15\\text{ m/s}$$', isCorrect: true },
            { id: 'C', text: '$$20\\text{ m/s}$$', isCorrect: false },
            { id: 'D', text: '$$25\\text{ m/s}$$', isCorrect: false },
          ],
          answerKey: 'B',
          explanation: 'From Newton\'s second law, acceleration is $$a = \\frac{F}{m} = \\frac{20}{4} = 5\\text{ m/s}^2$$. Then $$v = u + at = 0 + 5(3) = 15\\text{ m/s}$$.',
        };

    this.jsonInput.set(JSON.stringify(sample, null, 2));
    this.validationErrors.set([]);
    this.validationWarnings.set([]);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const reader = new FileReader();
      reader.onload = (e) => {
        const text = e.target?.result as string;
        this.jsonInput.set(text || '');
        this.validateInputOnly();
      };
      reader.readAsText(file);
    }
  }

  validateInputOnly(): boolean {
    const res = this.promptService.validateAndParseQuestionJson(this.jsonInput());
    this.validationErrors.set(res.errors);
    this.validationWarnings.set(res.warnings || []);
    return res.valid;
  }

  validateAndImport(): void {
    const res = this.promptService.validateAndParseQuestionJson(this.jsonInput());
    this.validationErrors.set(res.errors);
    this.validationWarnings.set(res.warnings || []);

    if (res.valid && res.data) {
      const taxonomy = this.getCurrentTaxonomy();
      const resultData = {
        ...res.data,
        subject: res.data.subject || taxonomy.subjectName,
        topic: res.data.topic || taxonomy.topicName,
        subtopic: res.data.subtopic || taxonomy.subtopicName,
        subjectId: res.data.subjectId || (taxonomy.subjectId ?? undefined),
        topicId: res.data.topicId || (taxonomy.topicId ?? undefined),
        subtopicId: res.data.subtopicId || (taxonomy.subtopicId ?? undefined),
      };
      this.dialogRef.close({
        action: 'import',
        importedQuestion: resultData as ImportedQuestion,
        taxonomy,
        ...resultData,
      } as any);
    }
  }

  close(): void {
    this.dialogRef.close({
      action: 'close',
      taxonomy: this.getCurrentTaxonomy(),
    } as QuestionPromptImportDialogResult);
  }
}
