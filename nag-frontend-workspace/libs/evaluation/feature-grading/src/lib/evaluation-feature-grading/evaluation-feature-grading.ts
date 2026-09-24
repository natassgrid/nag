import {
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  StatCardComponent,
  StatusBadgeComponent,
  MathRendererComponent,
  EmptyStateComponent,
  StatusVariant,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  EvaluationService,
  GradingTask,
  DisputeRecord,
} from '@nag-frontend-workspace/evaluation-data-access';

@Component({
  selector: 'nag-evaluation-feature-grading',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    StatCardComponent,
    StatusBadgeComponent,
    MathRendererComponent,
    EmptyStateComponent,
  ],
  templateUrl: './evaluation-feature-grading.component.html',
  styleUrl: './evaluation-feature-grading.component.scss',
})
export class EvaluationFeatureGrading implements OnInit {
  readonly evalService = inject(EvaluationService);

  activeTab = signal<'grading' | 'disputes'>('grading');
  selectedTask = signal<GradingTask | null>(null);

  awardedMarks = 0;
  evaluatorComments = '';
  submittingScore = signal<boolean>(false);

  ngOnInit(): void {
    this.evalService.loadPendingTasks().subscribe((tasks: GradingTask[]) => {
      if (tasks && tasks.length > 0) {
        this.selectedTask.set(tasks[0]);
        this.awardedMarks = tasks[0].maxMarks;
      }
    });
    this.evalService.loadDisputes().subscribe();
  }

  taskStatusVariant(status: string): StatusVariant {
    switch (status) {
      case 'PENDING':
      case 'SUBMITTED':
        return 'warn';
      case 'GRADED':
        return 'success';
      case 'DISPUTED':
      case 'IN_REVIEW':
        return 'primary';
      default:
        return 'neutral';
    }
  }

  disputeStatusVariant(status: string): StatusVariant {
    switch (status) {
      case 'OPEN':
        return 'warn';
      case 'RESOLVED':
        return 'success';
      case 'REJECTED':
        return 'error';
      default:
        return 'neutral';
    }
  }

  submitScore(taskId: string): void {
    this.submittingScore.set(true);
    this.evalService
      .submitGrade(taskId, this.awardedMarks, this.evaluatorComments)
      .subscribe({
        next: () => {
          this.submittingScore.set(false);
          this.selectedTask.set(null);
          this.evaluatorComments = '';
        },
        error: () => this.submittingScore.set(false),
      });
  }

  resolveDispute(d: DisputeRecord, resolution: 'RESOLVED' | 'REJECTED'): void {
    this.evalService
      .resolveDispute(d.id, resolution, `Dispute ${resolution.toLowerCase()} by evaluator desk`)
      .subscribe();
  }
}
