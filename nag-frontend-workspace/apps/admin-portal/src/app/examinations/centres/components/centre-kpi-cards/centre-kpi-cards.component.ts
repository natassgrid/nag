import {
  Component,
  ChangeDetectionStrategy,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-centre-kpi-cards',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './centre-kpi-cards.component.html',
  styleUrl: './centre-kpi-cards.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CentreKpiCardsComponent {
  totalCentresCount = input<number>(0);
  totalCapacitySum = input<number>(0);
  activeCentresCount = input<number>(0);
}
