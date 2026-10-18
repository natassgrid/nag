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
import { LessonType, LessonDifficultyLevel } from '../../models/learner.model';

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

  readonly typeChange = output<LessonType>();
  readonly difficultyChange = output<LessonDifficultyLevel>();
  readonly tryAnotherExplanation = output<void>();

  readonly allTypes: Array<{ type: LessonType; label: string; icon: string }> = [
    { type: 'CONCEPT', label: 'Core Concept', icon: 'menu_book' },
    { type: 'WORKED_EXAMPLES', label: 'Worked Examples', icon: 'functions' },
    { type: 'TRAPS_AND_PITFALLS', label: 'Traps & Pitfalls', icon: 'warning' },
    { type: 'CHEAT_SHEET', label: 'Cheat Sheet', icon: 'description' },
    { type: 'VISUAL', label: 'Visual Intuition', icon: 'visibility' },
    { type: 'QUICK_RECAP', label: 'Quick Recap', icon: 'bolt' },
  ];

  readonly allDifficulties: Array<{ level: LessonDifficultyLevel; label: string }> = [
    { level: 'FOUNDATION', label: 'Foundation' },
    { level: 'CORE', label: 'Exam Core' },
    { level: 'ADVANCED', label: 'Advanced' },
  ];

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

  onTryAnother(): void {
    this.tryAnotherExplanation.emit();
  }
}
