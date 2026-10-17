import { Component, ChangeDetectionStrategy, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  ExternalMicroLessonPromptService,
  MicroLessonPromptConfig,
} from '../../services/external-microlesson-prompt.service';
import { ParsedAuthoringLessonState } from '../micro-lesson-authoring/models';
import {
  SubjectHierarchy,
  SubjectTopicService,
  TargetExaminationOption,
} from '@nag-frontend-workspace/questions-data-access';

export interface MicroLessonPromptImportDialogData {
  subject?: string;
  topic?: string;
  subtopic?: string;
  subjectId?: number;
  topicId?: number;
  subtopicId?: number;
  targetExam?: string;
  referenceText?: string;
  activeTab?: 'prompt' | 'import';
}

@Component({
  selector: 'app-micro-lesson-prompt-import-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './micro-lesson-prompt-import-dialog.component.html',
  styleUrls: ['./micro-lesson-prompt-import-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonPromptImportDialogComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<MicroLessonPromptImportDialogComponent>);
  private readonly promptService = inject(ExternalMicroLessonPromptService);
  private readonly subjectTopicService = inject(SubjectTopicService);
  readonly data: MicroLessonPromptImportDialogData = inject(MAT_DIALOG_DATA, { optional: true }) || {};

  readonly activeTab = signal<'prompt' | 'import'>('prompt');

  // Dynamic Taxonomy State
  readonly hierarchy = signal<SubjectHierarchy[]>([]);
  readonly loadingTaxonomy = signal<boolean>(false);

  readonly selectedSubjectId = signal<number | null>(null);
  readonly isCustomSubject = signal<boolean>(false);
  readonly customSubjectName = signal<string>('Physics');

  readonly selectedTopicId = signal<number | null>(null);
  readonly isCustomTopic = signal<boolean>(false);
  readonly customTopicName = signal<string>('Classical Mechanics');

  readonly selectedSubtopicId = signal<number | null>(null);
  readonly isCustomSubtopic = signal<boolean>(false);
  readonly customSubtopicName = signal<string>('Work-Energy Theorem');

  // Dynamic Exam Options
  readonly examOptions = signal<{ label: string; value: string }[]>([
    { label: 'JEE Main & Advanced (JEE_MAIN)', value: 'JEE_MAIN' },
    { label: 'JEE Advanced (JEE_ADVANCED)', value: 'JEE_ADVANCED' },
    { label: 'NEET-UG Medical (NEET)', value: 'NEET' },
    { label: 'GATE Engineering (GATE)', value: 'GATE' },
    { label: 'UPSC Civil Services (UPSC_CSAT)', value: 'UPSC_CSAT' },
    { label: 'SSC CGL (SSC_CGL)', value: 'SSC_CGL' },
    { label: 'CAT Management (CAT)', value: 'CAT' },
    { label: 'General / Foundational (GENERAL)', value: 'GENERAL' },
  ]);
  readonly targetExam = signal<string>('JEE_MAIN');

  readonly referenceText = signal<string>('');
  readonly copied = signal<boolean>(false);

  // Import state
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
    if (this.isCustomSubject()) return this.customSubjectName().trim() || 'Physics';
    const subj = this.hierarchy().find((s) => s.id === this.selectedSubjectId());
    return subj?.name || this.customSubjectName().trim() || 'Physics';
  });

  readonly resolvedTopicName = computed(() => {
    if (this.isCustomTopic()) return this.customTopicName().trim() || 'Classical Mechanics';
    const top = this.availableTopics().find((t) => t.id === this.selectedTopicId());
    return top?.name || this.customTopicName().trim() || 'Classical Mechanics';
  });

  readonly resolvedSubtopicName = computed(() => {
    if (this.isCustomSubtopic()) return this.customSubtopicName().trim() || 'Work-Energy Theorem';
    const sub = this.availableSubtopics().find((s) => s.id === this.selectedSubtopicId());
    return sub?.name || this.customSubtopicName().trim() || 'Work-Energy Theorem';
  });

  readonly generatedPrompt = computed(() => {
    const config: MicroLessonPromptConfig = {
      subject: this.resolvedSubjectName(),
      topic: this.resolvedTopicName(),
      subtopic: this.resolvedSubtopicName(),
      targetExam: this.targetExam(),
      referenceText: this.referenceText(),
    };
    return this.promptService.generateMicroLessonPrompt(config);
  });

  ngOnInit(): void {
    if (this.data.activeTab) this.activeTab.set(this.data.activeTab);
    if (this.data.subject) this.customSubjectName.set(this.data.subject);
    if (this.data.topic) this.customTopicName.set(this.data.topic);
    if (this.data.subtopic) this.customSubtopicName.set(this.data.subtopic);
    if (this.data.targetExam) this.targetExam.set(this.data.targetExam);
    if (this.data.referenceText) this.referenceText.set(this.data.referenceText);

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
              } else if (matchedTopic.subtopics?.length > 0) {
                this.selectedSubtopicId.set(matchedTopic.subtopics[0].id);
                this.isCustomSubtopic.set(false);
              } else {
                this.isCustomSubtopic.set(true);
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
          } else if (!this.data.subject) {
            this.onSubjectSelect(String(list[0].id));
          } else {
            this.isCustomSubject.set(true);
            this.isCustomTopic.set(true);
            this.isCustomSubtopic.set(true);
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
    const subtopClean = (this.resolvedSubtopicName() || 'lesson').replace(/[^a-zA-Z0-9_-]/g, '_').toLowerCase();
    const filename = `prompt-microlesson-${subjClean}-${subtopClean}.txt`;
    this.promptService.downloadFile(filename, this.generatedPrompt());
  }

  loadSampleJson(): void {
    const sample = {
      targetExam: this.targetExam(),
      subtopicCustomName: this.resolvedSubtopicName() || 'Work-Energy Theorem',
      coreConcept:
        'The **Work-Energy Theorem** states that the net work done by all external and internal forces on a system equals the change in its kinetic energy:\n\n$$W_{\\text{all}} = \\Delta K = \\frac{1}{2}mv_f^2 - \\frac{1}{2}mv_i^2$$',
      trapAlert: {
        title: 'Sign Convention & Boundary Condition Pitfall',
        description: 'Candidates frequently apply formulas without verifying whether the reference frame is inertial.',
        commonMistake: 'Neglecting pseudo-force terms in accelerating frames of reference.',
        correctApproach: 'Always draw the complete free-body diagram and include $$-m\\vec{a}_{\\text{frame}}$$ before writing conservation equations.',
      },
      formulaEntries: [
        {
          name: 'Work-Energy Theorem',
          latex: '$$W_{\\text{all}} = \\Delta K = \\frac{1}{2}mv_f^2 - \\frac{1}{2}mv_i^2$$',
          description: 'Total work done by all forces equals change in kinetic energy.',
          variables: ['W_all: Total work', 'm: Mass', 'v_f, v_i: Final & initial speed'],
        },
      ],
      stepByStepBreakdown: {
        problemStatement:
          'A block of mass $$m = 4\\text{ kg}$$ is pushed across a rough surface ($$\\mu_k = 0.25$$) by a force $$F = 30\\text{ N}$$ over $$d = 5\\text{ m}$$ starting from rest. Find its final speed.',
        steps: [
          {
            stepNumber: 1,
            title: 'Identify System & Applied Forces',
            explanation: 'Isolate the block and calculate work of applied force and kinetic friction.',
            formula: '$$W_{\\text{net}} = (F - \\mu_k mg)d = (30 - 0.25 \\times 4 \\times 10)(5) = 100\\text{ J}$$',
            tip: 'Ensure SI units are used throughout.',
          },
          {
            stepNumber: 2,
            title: 'Equate to Kinetic Energy Change',
            explanation: 'Apply the theorem $$W_{\\text{net}} = \\frac{1}{2}mv_f^2$$.',
            formula: '$$v_f = \\sqrt{\\frac{2(100)}{4}} = 5\\sqrt{2}\\text{ m/s}$$',
            tip: 'Check dimensional consistency.',
          },
        ],
        finalAnswer: '$$v_f = 5\\sqrt{2}\\text{ m/s} \\approx 7.07\\text{ m/s}$$',
      },
      keyTakeaways: [
        'Include both conservative and non-conservative work terms in $$W_{\\text{all}} = \\Delta K$$.',
        'Verify whether reference frame acceleration exists.',
      ],
      quickCheckItems: [
        {
          question: 'Does the Work-Energy Theorem hold when non-conservative friction is present?',
          options: [
            'Yes, provided work done by friction is included in W_all',
            'No, only for conservative fields',
            'Only when acceleration is zero',
            'Only for point masses in vacuum',
          ],
          answer: 'Yes, provided work done by friction is included in W_all',
          explanation: 'The Work-Energy Theorem $$W_{\\text{all}} = \\Delta K$$ is universally valid when work of all forces is summed.',
        },
      ],
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
    const res = this.promptService.validateAndParseMicroLessonJson(this.jsonInput());
    this.validationErrors.set(res.errors);
    this.validationWarnings.set(res.warnings || []);
    return res.valid;
  }

  validateAndImport(): void {
    const res = this.promptService.validateAndParseMicroLessonJson(this.jsonInput());
    this.validationErrors.set(res.errors);
    this.validationWarnings.set(res.warnings || []);

    if (res.valid && res.data) {
      const resultData: ParsedAuthoringLessonState = {
        ...res.data,
        targetExam: res.data.targetExam || this.targetExam(),
        subtopicCustomName: res.data.subtopicCustomName || this.resolvedSubtopicName(),
      };
      this.dialogRef.close(resultData);
    }
  }

  close(): void {
    this.dialogRef.close(null);
  }
}
