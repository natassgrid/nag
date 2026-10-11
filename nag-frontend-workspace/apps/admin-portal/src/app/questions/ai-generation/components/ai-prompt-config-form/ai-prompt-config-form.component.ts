import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, Topic, Subtopic } from '@nag-frontend-workspace/questions-data-access';
import { SelectOption } from '../../models';

@Component({
  selector: 'nag-ai-prompt-config-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './ai-prompt-config-form.component.html',
  styleUrl: './ai-prompt-config-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiPromptConfigFormComponent {
  form = input.required<FormGroup>();
  generating = input<boolean>(false);

  /** All subjects from the taxonomy API (loaded by parent). */
  subjects = input<Subject[]>([]);
  /** Topics for the currently selected subject (loaded by parent on subject change). */
  topics = input<Topic[]>([]);
  /** Subtopics for the currently selected topic (loaded by parent on topic change). */
  subtopics = input<Subtopic[]>([]);
  /** Whether the taxonomy data is loading. */
  taxonomyLoading = input<boolean>(false);
  /** Taxonomy load error message (if any). */
  taxonomyError = input<string>('');

  difficulties = input<string[]>([]);
  cognitiveLevels = input<SelectOption[]>([]);
  questionTypes = input<SelectOption[]>([]);

  /** Emits the selected Subject object when the subject dropdown changes. */
  subjectChange = output<Subject>();
  /** Emits the selected Topic object when the topic dropdown changes. */
  topicChange = output<Topic>();
  generateQuestions = output<void>();
  addToBatch = output<void>();
  retryTaxonomy = output<void>();

  onSubjectSelect(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const id = Number(target.value);
    const subject = this.subjects().find((s) => s.id === id);
    if (subject) {
      this.subjectChange.emit(subject);
    }
  }

  onTopicSelect(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const id = Number(target.value);
    const topic = this.topics().find((t) => t.id === id);
    if (topic) {
      this.topicChange.emit(topic);
    }
  }

  onGenerate(): void {
    this.generateQuestions.emit();
  }

  onAddToBatch(): void {
    this.addToBatch.emit();
  }

  onRetryTaxonomy(): void {
    this.retryTaxonomy.emit();
  }
}
