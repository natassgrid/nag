import {
  Component,
  input,
  output,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import {
  DifficultyLevel,
  QuestionType,
  QuestionOption,
} from '@nag-frontend-workspace/questions-data-access';
import { StandaloneTaxonomyScoringComponent } from '../standalone-taxonomy-scoring/standalone-taxonomy-scoring.component';
import { StandaloneOptionsEditorComponent } from '../standalone-options-editor/standalone-options-editor.component';
import { StandaloneLivePreviewComponent } from '../standalone-live-preview/standalone-live-preview.component';
import { FormulaSymbolPaletteComponent } from '../formula-symbol-palette/formula-symbol-palette.component';

@Component({
  selector: 'nag-standalone-question-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    StandaloneTaxonomyScoringComponent,
    StandaloneOptionsEditorComponent,
    StandaloneLivePreviewComponent,
    FormulaSymbolPaletteComponent,
  ],
  templateUrl: './standalone-question-form.component.html',
  styleUrl: './standalone-question-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StandaloneQuestionFormComponent {
  readonly cognitiveLevel = input<string>('UNDERSTAND');
  readonly difficulty = input<DifficultyLevel>('MEDIUM');
  readonly type = input<QuestionType>('MULTIPLE_CHOICE');
  readonly marks = input<number>(4);
  readonly negativeMarks = input<number>(1);
  readonly content = input<string>('');
  readonly explanation = input<string>('');
  readonly options = input<QuestionOption[]>([]);

  readonly cognitiveLevelChange = output<string>();
  readonly difficultyChange = output<DifficultyLevel>();
  readonly typeChange = output<QuestionType>();
  readonly marksChange = output<number>();
  readonly negativeMarksChange = output<number>();
  readonly contentChange = output<string>();
  readonly explanationChange = output<string>();
  readonly optionsChange = output<QuestionOption[]>();

  readonly showFormulaPalette = signal<boolean>(false);
  readonly showExplanationPalette = signal<boolean>(false);

  readonly contentInvalid = computed(() => {
    return !this.content() || !this.content().trim();
  });

  toggleFormulaPalette(): void {
    this.showFormulaPalette.update((v) => !v);
  }

  toggleExplanationPalette(): void {
    this.showExplanationPalette.update((v) => !v);
  }

  insertSymbolIntoContent(symbol: string): void {
    const cur = this.content();
    const updated = cur ? `${cur} ${symbol}` : symbol;
    this.contentChange.emit(updated);
  }

  insertSymbolIntoExplanation(symbol: string): void {
    const cur = this.explanation();
    const updated = cur ? `${cur} ${symbol}` : symbol;
    this.explanationChange.emit(updated);
  }
}
