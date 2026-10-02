import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { PracticeSet } from '../../models';

@Component({
  selector: 'app-practice-set-card',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatChipsModule, MatIconModule],
  templateUrl: './practice-set-card.component.html',
  styleUrl: './practice-set-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeSetCardComponent {
  readonly set = input.required<PracticeSet>();
  readonly launch = output<PracticeSet>();
}
