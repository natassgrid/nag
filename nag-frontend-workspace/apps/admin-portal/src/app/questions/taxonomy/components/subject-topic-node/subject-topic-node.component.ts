import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { TopicNode } from '@nag-frontend-workspace/questions-data-access';
import { CreateSubtopicDto } from '../../models';

@Component({
  selector: 'nag-subject-topic-node',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './subject-topic-node.component.html',
  styleUrl: './subject-topic-node.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubjectTopicNodeComponent {
  readonly topic = input.required<TopicNode>();
  readonly isExpanded = input<boolean>(false);
  readonly isAddingSubtopic = input<boolean>(false);
  readonly creating = input<boolean>(false);

  readonly toggleTopic = output<number>();
  readonly openAddSubtopic = output<number>();
  readonly closeAddSubtopic = output<void>();
  readonly saveSubtopic = output<{ topicId: number; dto: CreateSubtopicDto }>();

  newSubtopicName = '';
  newSubtopicDescription = '';

  onSave(): void {
    if (!this.newSubtopicName.trim()) return;
    this.saveSubtopic.emit({
      topicId: this.topic().id,
      dto: {
        name: this.newSubtopicName.trim(),
        description: this.newSubtopicDescription.trim() || undefined,
      },
    });
    this.newSubtopicName = '';
    this.newSubtopicDescription = '';
  }

  onClose(): void {
    this.newSubtopicName = '';
    this.newSubtopicDescription = '';
    this.closeAddSubtopic.emit();
  }
}
