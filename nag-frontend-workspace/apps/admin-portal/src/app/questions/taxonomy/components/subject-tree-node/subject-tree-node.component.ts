import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  SubjectHierarchy,
  TopicNode,
  SubtopicNode,
} from '@nag-frontend-workspace/questions-data-access';
import { CreateTopicDto, CreateSubtopicDto } from '../../models';

@Component({
  selector: 'nag-subject-tree-node',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './subject-tree-node.component.html',
  styleUrl: './subject-tree-node.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubjectTreeNodeComponent {
  subject = input.required<SubjectHierarchy>();
  isExpanded = input<boolean>(false);
  expandedTopics = input<Set<number>>(new Set());
  addingTopic = input<boolean>(false);
  addingSubtopicTopicId = input<number | null>(null);
  creating = input<boolean>(false);

  toggleSubject = output<number>();
  toggleTopic = output<number>();
  openAddTopic = output<number>();
  closeAddTopic = output<void>();
  saveTopic = output<{ subjectId: number; dto: CreateTopicDto }>();
  openAddSubtopic = output<number>();
  closeAddSubtopic = output<void>();
  saveSubtopic = output<{ subjectId: number; topicId: number; dto: CreateSubtopicDto }>();

  newTopicName = '';
  newTopicDescription = '';
  newSubtopicName = '';
  newSubtopicDescription = '';

  onToggleSubject(): void {
    this.toggleSubject.emit(this.subject().id);
  }

  onToggleTopic(topicId: number): void {
    this.toggleTopic.emit(topicId);
  }

  onOpenAddTopic(): void {
    this.newTopicName = '';
    this.newTopicDescription = '';
    this.openAddTopic.emit(this.subject().id);
  }

  onCloseAddTopic(): void {
    this.newTopicName = '';
    this.newTopicDescription = '';
    this.closeAddTopic.emit();
  }

  onSaveTopic(): void {
    if (!this.newTopicName.trim()) return;
    this.saveTopic.emit({
      subjectId: this.subject().id,
      dto: {
        name: this.newTopicName.trim(),
        description: this.newTopicDescription.trim() || undefined,
      },
    });
    this.newTopicName = '';
    this.newTopicDescription = '';
  }

  onOpenAddSubtopic(topicId: number): void {
    this.newSubtopicName = '';
    this.newSubtopicDescription = '';
    this.openAddSubtopic.emit(topicId);
  }

  onCloseAddSubtopic(): void {
    this.newSubtopicName = '';
    this.newSubtopicDescription = '';
    this.closeAddSubtopic.emit();
  }

  onSaveSubtopic(topicId: number): void {
    if (!this.newSubtopicName.trim()) return;
    this.saveSubtopic.emit({
      subjectId: this.subject().id,
      topicId,
      dto: {
        name: this.newSubtopicName.trim(),
        description: this.newSubtopicDescription.trim() || undefined,
      },
    });
    this.newSubtopicName = '';
    this.newSubtopicDescription = '';
  }

  isTopicExpanded(topicId: number): boolean {
    return this.expandedTopics().has(topicId);
  }
}
