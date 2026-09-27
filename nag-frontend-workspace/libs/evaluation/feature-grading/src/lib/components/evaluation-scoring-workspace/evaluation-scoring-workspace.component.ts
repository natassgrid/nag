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
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { GradingTask } from '@nag-frontend-workspace/evaluation-data-access';
import {
  GradeSubmissionPayload,
  RubricCriterion,
  InlineComment,
  ModerationSignOff,
  createDefaultRubric,
} from '../../models';
import {
  EvaluationRubricPanelComponent,
  EvaluationResponseViewerComponent,
  EvaluationModerationCardComponent,
  CommentTag,
  ModerationVerdict,
} from './components';

@Component({
  selector: 'nag-evaluation-scoring-workspace',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    EvaluationRubricPanelComponent,
    EvaluationResponseViewerComponent,
    EvaluationModerationCardComponent,
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
  readonly selectedCommentTag = signal<CommentTag>('ACCURACY');

  // Moderation Sign-off
  readonly moderationRequired = signal<boolean>(false);
  readonly moderationVerdict = signal<ModerationVerdict>('APPROVED');
  readonly moderationNotes = signal<string>('');

  readonly commitGrade = output<GradeSubmissionPayload>();

  // Total marks computed dynamically from rubric criteria
  readonly totalAwardedMarks = computed(() => {
    const criteria = this.rubricCriteria() || [];
    if (criteria.length === 0) return 0;
    const sum = criteria.reduce((acc, c) => acc + (Number(c?.awardedMarks) || 0), 0);
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
        this.rubricCriteria.set(createDefaultRubric(current.maxMarks));
        this.comments.set('');
        this.inlineComments.set([]);
        this.moderationRequired.set(false);
        this.moderationVerdict.set('APPROVED');
        this.moderationNotes.set('');
      }
    });
  }

  updateCriterionScore(event: { criterionId: string; value: number }): void {
    this.rubricCriteria.update((list) =>
      list.map((c) => (c.id === event.criterionId ? { ...c, awardedMarks: event.value } : c))
    );
  }

  setComments(val: string): void {
    this.comments.set(val);
  }

  selectLineForComment(lineIndex: number): void {
    const lineNum = lineIndex + 1;
    this.selectedCommentLine.set(this.selectedCommentLine() === lineNum ? null : lineNum);
  }

  setCommentTag(tag: CommentTag): void {
    this.selectedCommentTag.set(tag);
  }

  setCommentText(text: string): void {
    this.newCommentText.set(text);
  }

  addInlineComment(): void {
    const text = (this.newCommentText() || '').trim();
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
    this.inlineComments.update((list) => list.filter((c) => c && c.id !== commentId));
  }

  setModerationRequired(required: boolean): void {
    this.moderationRequired.set(required);
  }

  setModerationVerdict(verdict: ModerationVerdict): void {
    this.moderationVerdict.set(verdict);
  }

  setModerationNotes(notes: string): void {
    this.moderationNotes.set(notes);
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
