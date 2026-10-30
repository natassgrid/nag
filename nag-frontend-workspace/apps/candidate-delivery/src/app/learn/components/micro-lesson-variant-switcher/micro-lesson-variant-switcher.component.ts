import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import {
  MicroTutorialApproach,
  SUPPORTED_MICROLESSON_LANGUAGES,
  MicroLessonLanguage,
  LessonType,
  LessonDifficultyLevel,
} from '../../models/micro-lesson.model';

@Component({
  selector: 'app-micro-lesson-variant-switcher',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatChipsModule],
  templateUrl: './micro-lesson-variant-switcher.component.html',
  styleUrl: './micro-lesson-variant-switcher.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonVariantSwitcherComponent {
  readonly currentType = input<LessonType>('CONCEPT');
  readonly currentDifficulty = input<LessonDifficultyLevel>('CORE');
  readonly approaches = input<MicroTutorialApproach[]>([]);
  readonly activeApproachId = input<string>('');
  readonly selectedLanguage = input<string>('en');

  readonly typeChange = output<LessonType>();
  readonly difficultyChange = output<LessonDifficultyLevel>();
  readonly tryAnotherExplanation = output<void>();
  readonly approachChange = output<MicroTutorialApproach>();
  readonly languageChange = output<string>();

  readonly supportedLanguages: MicroLessonLanguage[] = SUPPORTED_MICROLESSON_LANGUAGES;

  readonly allDifficulties: Array<{ level: LessonDifficultyLevel; label: string }> = [
    { level: 'FOUNDATION', label: 'Foundation' },
    { level: 'CORE', label: 'Exam Core' },
    { level: 'ADVANCED', label: 'Advanced' },
  ];

  onSelectApproach(approach: MicroTutorialApproach): void {
    this.approachChange.emit(approach);
  }

  onSelectType(type: LessonType): void {
    if (this.currentType() !== type) {
      this.typeChange.emit(type);
    }
  }

  onSelectDifficulty(level: LessonDifficultyLevel): void {
    if (this.currentDifficulty() !== level) {
      this.difficultyChange.emit(level);
    }
  }

  onSelectLanguage(langCode: string): void {
    if (this.selectedLanguage() !== langCode) {
      this.languageChange.emit(langCode);
    }
  }

  onTryAnother(): void {
    this.tryAnotherExplanation.emit();
  }
}
