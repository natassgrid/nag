import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
  PageHeaderComponent,
  EmptyStateComponent,
  extractHttpErrorMessage,
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
    MatSnackBarModule,
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
  private readonly snackBar = inject(MatSnackBar);
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
          this.snackBar.open('Grade submitted successfully.', 'OK', { duration: 3000 });
        },
        error: (err) => {
          this.submittingScore.set(false);
          this.snackBar.open(extractHttpErrorMessage(err, 'Failed to submit grade.'), 'Dismiss', {
            duration: 5000,
          });
        },
      });
  }

  handleResolveDispute(event: { dispute: DisputeRecord; resolution: 'RESOLVED' | 'REJECTED' }): void {
    this.evalService
      .resolveDispute(
        event.dispute.id,
        event.resolution,
        `Dispute ${event.resolution.toLowerCase()} by evaluator desk`
      )
      .subscribe({
        next: () => {
          this.snackBar.open(`Dispute ${event.resolution.toLowerCase()} successfully.`, 'OK', { duration: 3000 });
        },
        error: (err) => {
          this.snackBar.open(extractHttpErrorMessage(err, 'Failed to resolve dispute.'), 'Dismiss', {
            duration: 5000,
          });
        },
      });
  }

  handleBulkResolveDisputes(event: {
    disputes: DisputeRecord[];
    resolution: 'RESOLVED' | 'REJECTED';
  }): void {
    if (!event.disputes || event.disputes.length === 0) return;
    const requests = event.disputes.map((d) =>
      this.evalService.resolveDispute(
        d.id,
        event.resolution,
        `Dispute ${event.resolution.toLowerCase()} by evaluator desk (bulk action)`
      )
    );
    forkJoin(requests).subscribe({
      next: () => {
        this.evalService.loadDisputes().subscribe();
      },
    });
  }
}
