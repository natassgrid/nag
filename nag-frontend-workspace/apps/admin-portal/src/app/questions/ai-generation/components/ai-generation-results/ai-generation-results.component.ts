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
import { AiTelemetryRibbonComponent } from '../ai-telemetry-ribbon/ai-telemetry-ribbon.component';
import { AiGeneratedCardComponent } from '../ai-generated-card/ai-generated-card.component';

@Component({
  selector: 'nag-ai-generation-results',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    AiTelemetryRibbonComponent,
    AiGeneratedCardComponent,
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

  onSaveQuestion(event: { question: GeneratedQuestion; index: number }): void {
    this.saveQuestion.emit(event);
  }

  onSaveAllValid(): void {
    this.saveAllValid.emit();
  }

  onOpenAuthoring(question: GeneratedQuestion): void {
    this.openAuthoring.emit(question);
  }
}
