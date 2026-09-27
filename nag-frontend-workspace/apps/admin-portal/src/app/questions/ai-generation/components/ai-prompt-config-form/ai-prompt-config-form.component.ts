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
  subjects = input<string[]>([]);
  difficulties = input<string[]>([]);
  cognitiveLevels = input<SelectOption[]>([]);
  questionTypes = input<SelectOption[]>([]);

  subjectChange = output<string>();
  generateQuestions = output<void>();
  addToBatch = output<void>();

  onSubjectSelect(event: Event): void {
    const target = event.target as HTMLSelectElement;
    if (target?.value) {
      this.subjectChange.emit(target.value);
    }
  }

  onGenerate(): void {
    this.generateQuestions.emit();
  }

  onAddToBatch(): void {
    this.addToBatch.emit();
  }
}
