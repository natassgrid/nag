import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import {
  SubjectHierarchy,
} from '@nag-frontend-workspace/questions-data-access';
import { CreateTopicDto, CreateSubtopicDto } from '../../models';
import { SubjectTopicFormComponent } from '../subject-topic-form/subject-topic-form.component';
import { SubjectTopicNodeComponent } from '../subject-topic-node/subject-topic-node.component';

@Component({
  selector: 'nag-subject-tree-node',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    SubjectTopicFormComponent,
    SubjectTopicNodeComponent,
  ],
  templateUrl: './subject-tree-node.component.html',
  styleUrl: './subject-tree-node.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubjectTreeNodeComponent {
  readonly subject = input.required<SubjectHierarchy>();
  readonly isExpanded = input<boolean>(false);
  readonly expandedTopics = input<Set<number>>(new Set());
  readonly addingTopic = input<boolean>(false);
  readonly addingSubtopicTopicId = input<number | null>(null);
  readonly creating = input<boolean>(false);

  readonly toggleSubject = output<number>();
  readonly toggleTopic = output<number>();
  readonly openAddTopic = output<number>();
  readonly closeAddTopic = output<void>();
  readonly saveTopic = output<{ subjectId: number; dto: CreateTopicDto }>();
  readonly openAddSubtopic = output<number>();
  readonly closeAddSubtopic = output<void>();
  readonly saveSubtopic = output<{ subjectId: number; topicId: number; dto: CreateSubtopicDto }>();

  onToggleSubject(): void {
    this.toggleSubject.emit(this.subject().id);
  }

  onToggleTopic(topicId: number): void {
    this.toggleTopic.emit(topicId);
  }

  onOpenAddTopic(): void {
    this.openAddTopic.emit(this.subject().id);
  }

  onCloseAddTopic(): void {
    this.closeAddTopic.emit();
  }

  onSaveTopic(dto: CreateTopicDto): void {
    this.saveTopic.emit({
      subjectId: this.subject().id,
      dto,
    });
  }

  onOpenAddSubtopic(topicId: number): void {
    this.openAddSubtopic.emit(topicId);
  }

  onCloseAddSubtopic(): void {
    this.closeAddSubtopic.emit();
  }

  onSaveSubtopic(event: { topicId: number; dto: CreateSubtopicDto }): void {
    this.saveSubtopic.emit({
      subjectId: this.subject().id,
      topicId: event.topicId,
      dto: event.dto,
    });
  }

  isTopicExpanded(topicId: number): boolean {
    return this.expandedTopics().has(topicId);
  }
}
