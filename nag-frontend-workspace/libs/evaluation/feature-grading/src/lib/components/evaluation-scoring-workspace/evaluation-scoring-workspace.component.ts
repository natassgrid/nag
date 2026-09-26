import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { GradingTask } from '@nag-frontend-workspace/evaluation-data-access';
import {
  GradeSubmissionPayload,
  RubricCriterion,
  InlineComment,
  ModerationSignOff,
} from '../../models';

@Component({
  selector: 'nag-evaluation-scoring-workspace',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MathRendererComponent,
  ],
  templateUrl: './evaluation-scoring-workspace.component.html',
  styleUrl: './evaluation-scoring-workspace.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EvaluationScoringWorkspaceComponent {
  readonly task = input<GradingTask | null>(null);
  readonly submitting = input<boolean>(false);

  readonly comments = signal<string>('');
  readonly rubricCriteria = signal<RubricCriterion[]>([]);
  readonly inlineComments = signal<InlineComment[]>([]);
  readonly newCommentText = signal<string>('');
  readonly selectedCommentLine = signal<number | null>(null);
  readonly selectedCommentTag = signal<'ACCURACY' | 'METHODOLOGY' | 'SYNTAX_LOGIC' | 'FORMATTING'>('ACCURACY');

  // Moderation Sign-off
  readonly moderationRequired = signal<boolean>(false);
  readonly moderationVerdict = signal<'APPROVED' | 'REQUIRES_REVALUATION' | 'ESCALATED'>('APPROVED');
  readonly moderationNotes = signal<string>('');

  readonly commitGrade = output<GradeSubmissionPayload>();

  // Total marks computed dynamically from rubric criteria
  readonly totalAwardedMarks = computed(() => {
    const criteria = this.rubricCriteria();
    if (criteria.length === 0) return 0;
    const sum = criteria.reduce((acc, c) => acc + (Number(c.awardedMarks) || 0), 0);
    return Math.round(sum * 10) / 10;
  });

  // Candidate response lines for split-pane code / essay viewer
  readonly responseLines = computed(() => {
    const text = this.task()?.candidateResponse || '';
    return text.split('\n');
  });

  constructor() {
    effect(() => {
      const current = this.task();
      if (current) {
        this.initializeRubric(current.maxMarks);
        this.comments.set('');
        this.inlineComments.set([]);
        this.moderationRequired.set(false);
        this.moderationVerdict.set('APPROVED');
        this.moderationNotes.set('');
      }
    });
  }

  private initializeRubric(maxMarks: number): void {
    // Generate balanced rubric criteria based on max marks
    const p1 = Math.round(maxMarks * 0.4);
    const p2 = Math.round(maxMarks * 0.3);
    const p3 = Math.round(maxMarks * 0.2);
    const p4 = Math.max(1, maxMarks - p1 - p2 - p3);

    this.rubricCriteria.set([
      {
        id: 'crit-1',
        name: 'Conceptual Accuracy & Core Logic',
        description: 'Correctness of mathematical/algorithmic formulation and theoretical principles.',
        maxMarks: p1,
        awardedMarks: p1,
      },
      {
        id: 'crit-2',
        name: 'Methodological Rigor & Intermediate Steps',
        description: 'Sound step-by-step reasoning, derivation, and adherence to edge cases.',
        maxMarks: p2,
        awardedMarks: p2,
      },
      {
        id: 'crit-3',
        name: 'Efficiency & Optimization',
        description: 'Time/space complexity, resource utilization, or structural conciseness.',
        maxMarks: p3,
        awardedMarks: p3,
      },
      {
        id: 'crit-4',
        name: 'Presentation & Notation Quality',
        description: 'Clarity of variables, clean documentation, and standard scientific syntax.',
        maxMarks: p4,
        awardedMarks: p4,
      },
    ]);
  }

  updateCriterionScore(criterionId: string, value: number): void {
    this.rubricCriteria.update((list) =>
      list.map((c) => (c.id === criterionId ? { ...c, awardedMarks: Number(value) } : c))
    );
  }

  selectLineForComment(lineIndex: number): void {
    const lineNum = lineIndex + 1;
    this.selectedCommentLine.set(this.selectedCommentLine() === lineNum ? null : lineNum);
  }

  addInlineComment(): void {
    const text = this.newCommentText().trim();
    if (!text) return;

    const newComment: InlineComment = {
      id: `comm-${Date.now()}`,
      lineNumber: this.selectedCommentLine() ?? undefined,
      text,
      author: 'Lead Evaluator',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      tag: this.selectedCommentTag(),
    };

    this.inlineComments.update((list) => [...list, newComment]);
    this.newCommentText.set('');
    this.selectedCommentLine.set(null);
  }

  removeInlineComment(commentId: string): void {
    this.inlineComments.update((list) => list.filter((c) => c.id !== commentId));
  }

  getCommentsForLine(lineNum: number): InlineComment[] {
    return this.inlineComments().filter((c) => c.lineNumber === lineNum);
  }

  onSubmit(): void {
    const current = this.task();
    if (!current) return;

    let moderationSignOff: ModerationSignOff | undefined = undefined;
    if (this.moderationRequired()) {
      moderationSignOff = {
        moderatorName: 'Dr. A. Verma (Senior Panel Chair)',
        moderatorRole: 'CHIEF_MODERATOR',
        signedAt: new Date().toISOString(),
        verdict: this.moderationVerdict(),
        moderationNotes: this.moderationNotes(),
      };
    }

    this.commitGrade.emit({
      taskId: current.id,
      awardedMarks: this.totalAwardedMarks(),
      evaluatorComments: this.comments(),
      rubricBreakdown: this.rubricCriteria(),
      inlineComments: this.inlineComments(),
      moderationSignOff,
    });
  }
}
