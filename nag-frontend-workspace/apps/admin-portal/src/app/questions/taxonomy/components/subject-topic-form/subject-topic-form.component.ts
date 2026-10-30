import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { CreateTopicDto } from '../../models';

@Component({
  selector: 'nag-subject-topic-form',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './subject-topic-form.component.html',
  styleUrl: './subject-topic-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubjectTopicFormComponent {
  readonly subjectId = input.required<number>();
  readonly subjectName = input<string>('');
  readonly creating = input<boolean>(false);

  readonly closeForm = output<void>();
  readonly saveTopic = output<CreateTopicDto>();

  readonly validationError = signal<string | null>(null);

  newTopicName = '';
  newTopicDescription = '';

  onSave(): void {
    this.validationError.set(null);
    if (!this.newTopicName.trim()) {
      this.validationError.set('Topic name is required.');
      return;
    }
    this.saveTopic.emit({
      name: this.newTopicName.trim(),
      description: this.newTopicDescription.trim() || undefined,
    });
    this.newTopicName = '';
    this.newTopicDescription = '';
  }

  onClose(): void {
    this.validationError.set(null);
    this.newTopicName = '';
    this.newTopicDescription = '';
    this.closeForm.emit();
  }
}
