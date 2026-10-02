import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { Recommendation } from '../../models';
import { WeakTopicsPanelComponent } from '../weak-topics-panel/weak-topics-panel.component';
import { StudyPlanPanelComponent } from '../study-plan-panel/study-plan-panel.component';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-recommendation-card',
  standalone: true,
  imports: [
    CommonModule, 
    MatCardModule, 
    MatIconModule, 
    MatTabsModule,
    MatButtonModule,
    RouterModule,
    WeakTopicsPanelComponent, 
    StudyPlanPanelComponent
  ],
  templateUrl: './recommendation-card.component.html',
  styleUrl: './recommendation-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecommendationCardComponent {
  recommendation = input.required<Recommendation>();
}
