import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { WeakTopicRecommendation } from '../../models';

@Component({
  selector: 'app-weak-topics-panel',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatChipsModule],
  templateUrl: './weak-topics-panel.component.html',
  styleUrl: './weak-topics-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WeakTopicsPanelComponent {
  topics = input.required<WeakTopicRecommendation[]>();
}
