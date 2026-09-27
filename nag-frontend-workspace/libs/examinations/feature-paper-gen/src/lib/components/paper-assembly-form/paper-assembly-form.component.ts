import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import {
  ExaminationResponse,
  ScheduleResponse,
  ShiftResponse,
  BlueprintRule,
} from '@nag-frontend-workspace/examinations-data-access';
import {
  Subject,
  SubjectHierarchy,
  BlueprintTemplateResponse,
} from '@nag-frontend-workspace/questions-data-access';
import { BlueprintRuleBuilderComponent } from '../blueprint-rule-builder/blueprint-rule-builder.component';
import { PaperTargetSelectorComponent } from '../paper-target-selector/paper-target-selector.component';
import { PaperGenFormData } from '../../models';

@Component({
  selector: 'nag-paper-assembly-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    BlueprintRuleBuilderComponent,
    PaperTargetSelectorComponent,
  ],
  templateUrl: './paper-assembly-form.component.html',
  styleUrl: './paper-assembly-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaperAssemblyFormComponent {
  readonly exams = input<ExaminationResponse[]>([]);
  readonly schedules = input<ScheduleResponse[]>([]);
  readonly shifts = input<ShiftResponse[]>([]);
  readonly templates = input<BlueprintTemplateResponse[]>([]);
  readonly taxonomySubjects = input<Subject[]>([]);
  readonly taxonomyHierarchy = input<SubjectHierarchy[]>([]);
  readonly isGenerating = input<boolean>(false);
  readonly isCheckingFeasibility = input<boolean>(false);

  readonly examChange = output<string>();
  readonly scheduleChange = output<string>();
  readonly checkFeasibility = output<void>();
  readonly generate = output<PaperGenFormData>();

  // Internal Form State
  genExamId = signal<string>('');
  genScheduleId = signal<string>('');
  genShiftId = signal<string>('');
  genPaperName = signal<string>('');
  genIsPractice = signal<boolean>(false);
  genUseTemplate = signal<boolean>(true);
  genSelectedTemplateId = signal<string>('');

  genRules = signal<BlueprintRule[]>([
    {
      subject: 'Quantitative Aptitude',
      topic: 'Arithmetic',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'APPLY',
      questionType: 'SINGLE_MCQ',
      targetCount: 15,
      questionCount: 15,
    },
    {
      subject: 'General Intelligence',
      topic: 'Reasoning',
      difficulty: 'EASY',
      cognitiveLevel: 'UNDERSTAND',
      questionType: 'SINGLE_MCQ',
      targetCount: 15,
      questionCount: 15,
    },
  ]);

  readonly totalRequestedQuestions = computed(() => {
    if (this.genUseTemplate()) {
      const tpl = this.templates().find((t) => t.id === this.genSelectedTemplateId());
      if (tpl && tpl.rules) {
        return tpl.rules.reduce((acc, r) => acc + (r.questionCount || r.targetCount || 0), 0);
      }
      return 0;
    }
    return this.genRules().reduce((acc, r) => acc + (r.questionCount || r.targetCount || 0), 0);
  });

  setTemplate(templateId: string): void {
    this.genUseTemplate.set(true);
    this.genSelectedTemplateId.set(templateId);
  }

  onExamSelected(examId: string): void {
    this.genExamId.set(examId);
    this.genScheduleId.set('');
    this.genShiftId.set('');
    this.examChange.emit(examId);
  }

  onScheduleSelected(scheduleId: string): void {
    this.genScheduleId.set(scheduleId);
    this.genShiftId.set('');
    this.scheduleChange.emit(scheduleId);
  }

  getTopicsForSubject(subjectName: string): string[] {
    const node = this.taxonomyHierarchy().find(
      (h) => h.name.toLowerCase() === (subjectName || '').toLowerCase()
    );
    if (node && node.topics && node.topics.length > 0) {
      return node.topics.map((t) => t.name);
    }
    return [];
  }

  onRuleSubjectChange(rule: BlueprintRule): void {
    const topics = this.getTopicsForSubject(rule.subject);
    if (topics.length > 0 && !topics.includes(rule.topic)) {
      rule.topic = topics[0];
    }
  }

  addRule(): void {
    const subjects = this.taxonomySubjects();
    const defaultSubject = subjects.length > 0 ? subjects[0].name : '';
    const topics = this.getTopicsForSubject(defaultSubject);
    const defaultTopic = topics.length > 0 ? topics[0] : '';

    this.genRules.update((rules) => [
      ...rules,
      {
        subject: defaultSubject,
        topic: defaultTopic,
        difficulty: 'MEDIUM',
        cognitiveLevel: 'UNDERSTAND',
        questionType: 'SINGLE_MCQ',
        targetCount: 10,
        questionCount: 10,
      },
    ]);
  }

  removeRule(index: number): void {
    this.genRules.update((rules) => rules.filter((_, i) => i !== index));
  }

  submitGenerate(): void {
    this.generate.emit({
      examId: this.genExamId(),
      scheduleId: this.genScheduleId(),
      shiftId: this.genShiftId(),
      paperName: this.genPaperName(),
      isPractice: this.genIsPractice(),
      useTemplate: this.genUseTemplate(),
      selectedTemplateId: this.genSelectedTemplateId(),
      rules: this.genRules(),
    });
  }
}
