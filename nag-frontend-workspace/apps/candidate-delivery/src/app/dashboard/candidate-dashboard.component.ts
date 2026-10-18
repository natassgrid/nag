import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { EnrolledExam, DigitalAdmitCard, PracticePaperSummary } from './models';
import { CandidateDashboardService } from './services';
import {
  DashboardWelcomeBannerComponent,
  DashboardKpiStatsComponent,
  EnrolledAssessmentCardComponent,
  CandidateAdmitCardDialogComponent,
  PracticePaperPickerDialogComponent,
} from './components';
import { LearningPathService } from '../learn/services/learning-path.service';
import { GatekeeperDialogComponent } from '../learn/components/gatekeeper-dialog/gatekeeper-dialog.component';
import { GatekeeperStatusResponse } from '../learn/models/learning-path.model';

export * from './models';
export * from './services';
export * from './components';

@Component({
  selector: 'app-candidate-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    DashboardWelcomeBannerComponent,
    DashboardKpiStatsComponent,
    EnrolledAssessmentCardComponent,
    CandidateAdmitCardDialogComponent,
    PracticePaperPickerDialogComponent,
  ],
  templateUrl: './candidate-dashboard.component.html',
  styleUrl: './candidate-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateDashboardComponent implements OnInit {
  readonly authService = inject(AuthService);
  readonly dashboardService = inject(CandidateDashboardService);
  private readonly learningPathService = inject(LearningPathService, { optional: true });
  private readonly dialog = inject(MatDialog);

  readonly activeFilter = signal<'ALL' | 'LIVE' | 'UPCOMING' | 'PRACTICE' | 'COMPLETED'>('ALL');
  readonly selectedAdmitCard = signal<DigitalAdmitCard | null>(null);
  readonly router = inject(Router);
  readonly selectedPracticePapers = signal<{ exam: EnrolledExam; papers: PracticePaperSummary[] } | null>(null);

  readonly candidateName = computed(() => {
    return this.authService.currentUser()?.username || 'Aryan Sharma';
  });

  readonly filteredExams = computed(() => {
    const filter = this.activeFilter();
    const list = this.dashboardService.enrolledExams();
    if (filter === 'ALL') return list;
    if (filter === 'PRACTICE') return list.filter((e) => e.isPractice || e.practiceAvailable);
    if (filter === 'UPCOMING') return list.filter((e) => !e.isPractice && (e.status === 'UPCOMING' || e.status === 'SCHEDULED'));
    return list.filter((e) => e.status === filter);
  });

  ngOnInit(): void {
    this.dashboardService.loadEnrolledExams().subscribe();
  }

  setFilter(filter: 'ALL' | 'LIVE' | 'UPCOMING' | 'PRACTICE' | 'COMPLETED'): void {
    this.activeFilter.set(filter);
  }

  openAdmitCard(exam: EnrolledExam): void {
    this.dashboardService
      .getAdmitCard(exam, this.candidateName())
      .subscribe((card) => this.selectedAdmitCard.set(card));
  }

  closeAdmitCard(): void {
    this.selectedAdmitCard.set(null);
  }

  printAdmitCard(): void {
    window.print();
  }

  onLaunchPractice(exam: EnrolledExam): void {
    // Intercept with Gatekeeper prerequisite check (Issue #375)
    if (this.learningPathService) {
      this.learningPathService.checkGatekeeperStatus().subscribe({
        next: (status: GatekeeperStatusResponse) => {
          const isBlocked = (status?.gatekeeperRequired && !status?.clearedForFullMock) || status?.allowed === false;
          if (status && isBlocked) {
            this.dialog.open(GatekeeperDialogComponent, {
              data: {
                statusResponse: status,
                mockExamTitle: exam.title,
              },
              width: '520px',
              panelClass: 'nag-dialog-clean',
            });
            return;
          }
          this.executeLaunchPractice(exam);
        },
        error: () => {
          this.executeLaunchPractice(exam);
        },
      });
      return;
    }

    this.executeLaunchPractice(exam);
  }

  private executeLaunchPractice(exam: EnrolledExam): void {
    this.dashboardService.getPracticePapers(exam.id).subscribe((papers) => {
      if (papers && papers.length > 1) {
        this.selectedPracticePapers.set({ exam, papers });
      } else if (papers && papers.length === 1) {
        this.router.navigate(['/delivery'], {
          queryParams: {
            examId: exam.id,
            paperId: papers[0].paperId,
            mode: 'PRACTICE',
          },
        });
      } else {
        // Direct launch with default fallback mock
        this.router.navigate(['/delivery'], {
          queryParams: {
            examId: exam.id,
            mode: 'PRACTICE',
          },
        });
      }
    });
  }

  onSelectPracticePaper(paper: PracticePaperSummary): void {
    const current = this.selectedPracticePapers();
    const examId = current?.exam.id || paper.examId;
    this.selectedPracticePapers.set(null);
    this.router.navigate(['/delivery'], {
      queryParams: {
        examId,
        paperId: paper.paperId,
        mode: 'PRACTICE',
      },
    });
  }

  closePracticePaperDialog(): void {
    this.selectedPracticePapers.set(null);
  }

  retryLoad(): void {
    this.dashboardService.loadEnrolledExams().subscribe();
  }
}
