import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-micro-lesson-feedback-bar',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './micro-lesson-feedback-bar.component.html',
  styleUrl: './micro-lesson-feedback-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonFeedbackBarComponent {
  readonly isCompleted = input<boolean>(false);
  readonly completing = input<boolean>(false);

  readonly rateHelpful = output<number>();
  readonly markCompleted = output<void>();

  readonly userRating = signal<number | null>(null);

  onRate(val: number): void {
    this.userRating.set(val);
    this.rateHelpful.emit(val);
  }

  onMarkCompleted(): void {
    this.markCompleted.emit();
  }
}
