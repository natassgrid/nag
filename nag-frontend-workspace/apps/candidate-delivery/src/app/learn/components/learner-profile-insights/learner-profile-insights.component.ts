import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { LearnerProfile } from '../../models';

@Component({
  selector: 'app-learner-profile-insights',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatProgressBarModule, MatDividerModule, MatChipsModule],
  templateUrl: './learner-profile-insights.component.html',
  styleUrl: './learner-profile-insights.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LearnerProfileInsightsComponent {
  profile = input.required<LearnerProfile>();

  protected getTopicAccuracyList(topicMap: Record<string, { accuracy: number; attempts: number }>) {
    return Object.entries(topicMap || {}).map(([topic, data]) => ({ topic, ...data }));
  }
}
