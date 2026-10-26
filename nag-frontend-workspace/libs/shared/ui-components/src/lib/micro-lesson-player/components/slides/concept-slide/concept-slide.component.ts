import {
  Component,
  ChangeDetectionStrategy,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MathRendererComponent } from '../../../../math-renderer.component';
import {
  PlayerSubtopicMicroLesson,
  PlayerTrapAlert,
} from '../../../micro-lesson-player.model';

@Component({
  selector: 'nag-concept-slide, app-concept-slide',
  standalone: true,
  imports: [CommonModule, MatIconModule, MathRendererComponent],
  templateUrl: './concept-slide.component.html',
  styleUrl: './concept-slide.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConceptSlideComponent {
  readonly lesson = input.required<PlayerSubtopicMicroLesson>();

  asTrapAlert(trap: unknown): PlayerTrapAlert | null {
    return typeof trap === 'object' && trap !== null && 'title' in trap
      ? (trap as PlayerTrapAlert)
      : null;
  }

  asString(val: unknown): string {
    return typeof val === 'string' ? val : '';
  }
}
