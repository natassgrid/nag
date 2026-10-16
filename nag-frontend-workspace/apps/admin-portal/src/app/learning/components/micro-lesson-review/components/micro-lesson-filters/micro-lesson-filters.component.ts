import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-micro-lesson-filters',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './micro-lesson-filters.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonFiltersComponent {
  readonly searchQuery = input<string>('');
  readonly selectedSubject = input<string>('ALL');
  readonly selectedTopic = input<string>('ALL');
  readonly statusFilter = input<string>('ALL');
  readonly distinctSubjects = input<string[]>([]);
  readonly distinctTopics = input<string[]>([]);
  readonly statusOptions = input<{ label: string; value: string }[]>([]);

  readonly searchChange = output<string>();
  readonly subjectChange = output<string>();
  readonly topicChange = output<string>();
  readonly statusChange = output<string>();
}
