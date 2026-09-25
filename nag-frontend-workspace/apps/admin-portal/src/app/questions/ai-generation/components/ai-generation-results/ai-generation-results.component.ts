import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  QuestionGenerationResponse,
  GeneratedQuestion,
} from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-ai-generation-results',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './ai-generation-results.component.html',
  styleUrl: './ai-generation-results.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiGenerationResultsComponent {
  generating = input<boolean>(false);
  response = input<QuestionGenerationResponse | null>(null);
  generationError = input<string>('');
  savedQuestionIds = input<Set<number>>(new Set());
  savingIndices = input<Set<number>>(new Set());
  count = input<number>(3);
  subject = input<string>('Mathematics');
  topic = input<string>('Linear Algebra & Matrices');

  saveQuestion = output<{ question: GeneratedQuestion; index: number }>();
  saveAllValid = output<void>();
  openAuthoring = output<GeneratedQuestion>();

  onSaveQuestion(question: GeneratedQuestion, index: number): void {
    this.saveQuestion.emit({ question, index });
  }

  onSaveAllValid(): void {
    this.saveAllValid.emit();
  }

  onOpenAuthoring(question: GeneratedQuestion): void {
    this.openAuthoring.emit(question);
  }
}
