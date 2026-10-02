import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PracticeSet } from '../../models';

@Component({
  selector: 'app-practice-set-card',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './practice-set-card.component.html',
  styleUrl: './practice-set-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeSetCardComponent {
  readonly set = input.required<PracticeSet>();
  readonly launch = output<PracticeSet>();

  readonly isOfficialExam = computed(() => this.set().source === 'EXAM_CLONE');
  readonly formattedSubject = computed(() => {
    const slug = this.set().subjectSlug;
    if (!slug) return 'General Aptitude';
    return slug
      .split('-')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  });
}
