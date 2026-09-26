import {
  Component,
  input,
  output,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { Subject, Topic, Subtopic } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-taxonomy-selector',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './taxonomy-selector.component.html',
  styleUrl: './taxonomy-selector.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaxonomySelectorComponent {
  readonly subjects = input<Subject[]>([]);
  readonly topics = input<Topic[]>([]);
  readonly subtopics = input<Subtopic[]>([]);
  readonly selectedSubjectId = input<number | null>(null);
  readonly selectedTopicId = input<number | null>(null);
  readonly selectedSubtopicId = input<number | null>(null);

  readonly subjectChange = output<number>();
  readonly topicChange = output<number>();
  readonly subtopicChange = output<number | null>();
  readonly createSubject = output<string>();
  readonly createTopic = output<string>();

  showNewSubject = false;
  showNewTopic = false;
  newSubjectName = '';
  newTopicName = '';

  onSubjectSelect(id: any): void {
    const numId = Number(id);
    if (!isNaN(numId)) {
      this.subjectChange.emit(numId);
    }
  }

  onTopicSelect(id: any): void {
    const numId = Number(id);
    if (!isNaN(numId)) {
      this.topicChange.emit(numId);
    }
  }

  onSubtopicSelect(id: any): void {
    if (id === null || id === 'null' || id === undefined) {
      this.subtopicChange.emit(null);
    } else {
      const numId = Number(id);
      this.subtopicChange.emit(isNaN(numId) ? null : numId);
    }
  }

  onCreateSubject(): void {
    if (!this.newSubjectName.trim()) return;
    this.createSubject.emit(this.newSubjectName.trim());
    this.newSubjectName = '';
    this.showNewSubject = false;
  }

  onCreateTopic(): void {
    if (!this.newTopicName.trim()) return;
    this.createTopic.emit(this.newTopicName.trim());
    this.newTopicName = '';
    this.showNewTopic = false;
  }
}
