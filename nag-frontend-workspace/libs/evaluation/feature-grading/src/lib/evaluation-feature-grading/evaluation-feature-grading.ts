import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  EmptyStateComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  EvaluationService,
  GradingTask,
  DisputeRecord,
} from '@nag-frontend-workspace/evaluation-data-access';
import {
  EvaluationKpiSummaryComponent,
  EvaluationTaskListComponent,
  EvaluationScoringWorkspaceComponent,
  EvaluationDisputesTableComponent,
} from '../components';
import { EvaluationActiveTab, GradeSubmissionPayload } from '../models';

@Component({
  selector: 'nag-evaluation-feature-grading',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    EmptyStateComponent,
    EvaluationKpiSummaryComponent,
    EvaluationTaskListComponent,
    EvaluationScoringWorkspaceComponent,
    EvaluationDisputesTableComponent,
  ],
  templateUrl: './evaluation-feature-grading.component.html',
  styleUrl: './evaluation-feature-grading.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EvaluationFeatureGrading implements OnInit {
  readonly evalService = inject(EvaluationService);

  readonly activeTab = signal<EvaluationActiveTab>('grading');
  readonly selectedTask = signal<GradingTask | null>(null);
  readonly submittingScore = signal<boolean>(false);

  ngOnInit(): void {
    this.evalService.loadPendingTasks().subscribe((tasks: GradingTask[]) => {
      if (tasks && tasks.length > 0) {
        this.selectedTask.set(tasks[0]);
      }
    });
    this.evalService.loadDisputes().subscribe();
  }

  handleSubmitGrade(payload: GradeSubmissionPayload): void {
    this.submittingScore.set(true);
    this.evalService
      .submitGrade(payload.taskId, payload.awardedMarks, payload.evaluatorComments)
      .subscribe({
        next: () => {
          this.submittingScore.set(false);
          this.selectedTask.set(null);
        },
        error: () => this.submittingScore.set(false),
      });
  }

  handleResolveDispute(event: { dispute: DisputeRecord; resolution: 'RESOLVED' | 'REJECTED' }): void {
    this.evalService
      .resolveDispute(
        event.dispute.id,
        event.resolution,
        `Dispute ${event.resolution.toLowerCase()} by evaluator desk`
      )
      .subscribe();
  }
}
