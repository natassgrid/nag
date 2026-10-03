import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { StudyPlanItem } from '../../models';

@Component({
  selector: 'app-study-plan-panel',
  standalone: true,
  imports: [CommonModule, MatListModule, MatIconModule],
  templateUrl: './study-plan-panel.component.html',
  styleUrl: './study-plan-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StudyPlanPanelComponent {
  plan = input.required<StudyPlanItem[]>();
}
