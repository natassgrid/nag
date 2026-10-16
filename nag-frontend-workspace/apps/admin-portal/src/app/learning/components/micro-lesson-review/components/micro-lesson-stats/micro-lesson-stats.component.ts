import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-micro-lesson-stats',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './micro-lesson-stats.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonStatsComponent {
  readonly total = input<number>(0);
  readonly aiGenerated = input<number>(0);
  readonly approved = input<number>(0);
}
