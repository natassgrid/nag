import {
  Component,
  input,
  output,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import {
  Subject,
  Topic,
  Subtopic,
  TaxonomySubjectNode,
  TaxonomyTopicNode,
  TaxonomySubtopicNode,
} from '@nag-frontend-workspace/questions-data-access';

export interface TaxonomySelection {
  subjectId: number;
  topicId: number | null;
  subtopicId: number | null;
}

@Component({
  selector: 'nag-taxonomy-selector',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './taxonomy-selector.component.html',
  styleUrl: './taxonomy-selector.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaxonomySelectorComponent {
  readonly subjects = input<Array<Subject | TaxonomySubjectNode>>([]);
  readonly topics = input<Array<Topic | TaxonomyTopicNode>>([]);
  readonly subtopics = input<Array<Subtopic | TaxonomySubtopicNode>>([]);
  readonly tree = input<TaxonomySubjectNode[] | null>(null);

  readonly selectedSubjectId = input<number | null>(null);
  readonly selectedTopicId = input<number | null>(null);
  readonly selectedSubtopicId = input<number | null>(null);
  readonly subtopicRequired = input<boolean>(true);

  readonly subjectChange = output<number>();
  readonly topicChange = output<number>();
  readonly subtopicChange = output<number | null>();
  readonly selectionChange = output<TaxonomySelection>();
  readonly createSubject = output<string>();
  readonly createTopic = output<string>();

  showNewSubject = false;
  showNewTopic = false;
  newSubjectName = '';
  newTopicName = '';

  readonly effectiveTopics = computed<Array<Topic | TaxonomyTopicNode>>(() => {
    const fullTree = this.tree();
    const subId = this.selectedSubjectId();
    if (fullTree && fullTree.length > 0 && subId != null) {
      const match = fullTree.find((s) => Number(s.id) === Number(subId));
      if (match?.topics) return match.topics;
    }
    return this.topics() || [];
  });

  readonly effectiveSubtopics = computed<Array<Subtopic | TaxonomySubtopicNode>>(() => {
    const fullTree = this.tree();
    const subId = this.selectedSubjectId();
    const topId = this.selectedTopicId();
    if (fullTree && fullTree.length > 0 && subId != null && topId != null) {
      const subMatch = fullTree.find((s) => Number(s.id) === Number(subId));
      const topMatch = subMatch?.topics?.find((t) => Number(t.id) === Number(topId));
      if (topMatch?.subtopics) return topMatch.subtopics;
    }
    return this.subtopics() || [];
  });

  onSubjectSelect(id: any): void {
    const numId = Number(id);
    if (!isNaN(numId)) {
      this.subjectChange.emit(numId);
      this.selectionChange.emit({
        subjectId: numId,
        topicId: null,
        subtopicId: null,
      });
    }
  }

  onTopicSelect(id: any): void {
    if (id === null || id === 'null' || id === undefined || id === '') {
      this.topicChange.emit(0);
      this.selectionChange.emit({
        subjectId: this.selectedSubjectId() ?? 0,
        topicId: null,
        subtopicId: null,
      });
      return;
    }
    const numId = Number(id);
    if (!isNaN(numId)) {
      this.topicChange.emit(numId);
      this.selectionChange.emit({
        subjectId: this.selectedSubjectId() ?? 0,
        topicId: numId,
        subtopicId: null,
      });
    }
  }

  onSubtopicSelect(id: any): void {
    if (id === null || id === 'null' || id === undefined || id === '') {
      this.subtopicChange.emit(null);
      this.selectionChange.emit({
        subjectId: this.selectedSubjectId() ?? 0,
        topicId: this.selectedTopicId(),
        subtopicId: null,
      });
    } else {
      const numId = Number(id);
      const val = isNaN(numId) ? null : numId;
      this.subtopicChange.emit(val);
      this.selectionChange.emit({
        subjectId: this.selectedSubjectId() ?? 0,
        topicId: this.selectedTopicId(),
        subtopicId: val,
      });
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
