import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MathRendererComponent } from '../../../../math-renderer.component';
import {
  PlayerSubtopicMicroLesson,
  LessonSlide,
} from '../../../micro-lesson-player.model';

@Component({
  selector: 'nag-intro-slide, app-intro-slide',
  standalone: true,
  imports: [CommonModule, MatIconModule, MathRendererComponent],
  templateUrl: './intro-slide.component.html',
  styleUrl: './intro-slide.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IntroSlideComponent {
  readonly lesson = input.required<PlayerSubtopicMicroLesson>();
  readonly slides = input<LessonSlide[]>([]);

  readonly startAutoPlay = output<void>();
  readonly nextSlide = output<void>();
  readonly goToSlide = output<number>();
}
