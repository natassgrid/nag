import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import {
  DifficultyLevel,
  QuestionType,
} from '@nag-frontend-workspace/questions-data-access';
import { COGNITIVE_LEVELS } from '../../models/authoring.model';

@Component({
  selector: 'nag-standalone-taxonomy-scoring',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './standalone-taxonomy-scoring.component.html',
  styleUrl: './standalone-taxonomy-scoring.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StandaloneTaxonomyScoringComponent {
  readonly cognitiveLevels = COGNITIVE_LEVELS;

  readonly cognitiveLevel = input<string>('UNDERSTAND');
  readonly difficulty = input<DifficultyLevel>('MEDIUM');
  readonly type = input<QuestionType>('MULTIPLE_CHOICE');
  readonly marks = input<number>(4);
  readonly negativeMarks = input<number>(1);

  readonly cognitiveLevelChange = output<string>();
  readonly difficultyChange = output<DifficultyLevel>();
  readonly typeChange = output<QuestionType>();
  readonly marksChange = output<number>();
  readonly negativeMarksChange = output<number>();
}
