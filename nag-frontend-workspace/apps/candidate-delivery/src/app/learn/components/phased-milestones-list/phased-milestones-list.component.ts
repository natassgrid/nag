import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MilestonePhase, MilestoneItem } from '../../models/learning-path.model';

@Component({
  selector: 'app-phased-milestones-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatChipsModule,
    MatTooltipModule,
  ],
  templateUrl: './phased-milestones-list.component.html',
  styleUrl: './phased-milestones-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PhasedMilestonesListComponent {
  readonly phases = input<MilestonePhase[]>([]);
  readonly currentPhaseNumber = input<number>(1);
  readonly actionInProgress = input<boolean>(false);

  readonly studyLesson = output<MilestoneItem>();
  readonly startQuiz = output<MilestonePhase>();
  readonly startGatekeeper = output<MilestonePhase>();

  isItemCompleted(item: MilestoneItem): boolean {
    return item.completed === true || item.status === 'COMPLETED';
  }

  isPhaseActive(phase: MilestonePhase): boolean {
    return (
      phase.status === 'ACTIVE' ||
      phase.status === 'IN_PROGRESS' ||
      phase.phaseNumber === this.currentPhaseNumber()
    );
  }

  isPhaseLocked(phase: MilestonePhase): boolean {
    return phase.status === 'LOCKED' && phase.phaseNumber > this.currentPhaseNumber();
  }

  onStudyLesson(item: MilestoneItem): void {
    this.studyLesson.emit(item);
  }

  onStartQuiz(phase: MilestonePhase): void {
    this.startQuiz.emit(phase);
  }

  onStartGatekeeper(phase: MilestonePhase): void {
    this.startGatekeeper.emit(phase);
  }
}
