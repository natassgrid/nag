import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Question } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-question-translation-table',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './question-translation-table.component.html',
  styleUrl: './question-translation-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionTranslationTableComponent {
  questions = input<Question[]>([]);
  availableSubjects = input<string[]>([]);
  searchQuery = input<string>('');
  selectedSubject = input<string>('ALL');
  activeLanguageName = input<string>('');

  searchQueryChange = output<string>();
  selectedSubjectChange = output<string>();
  openTranslation = output<Question>();
}
