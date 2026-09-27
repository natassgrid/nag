import {
  Component,
  ChangeDetectionStrategy,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-exam-kpi-cards',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './exam-kpi-cards.component.html',
  styleUrl: './exam-kpi-cards.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamKpiCardsComponent {
  totalCount = input<number>(0);
  publishedCount = input<number>(0);
  draftCount = input<number>(0);
}
