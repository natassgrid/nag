import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';

export type QuestionStatus = 'not-visited' | 'current' | 'answered' | 'marked-for-review';

@Component({
  selector: 'app-navigation-palette',
  standalone: true,
  imports: [CommonModule, MatButtonModule],
  template: `
    <div class="palette-wrapper" role="navigation" aria-label="Question navigation palette">
      <h3 class="palette-title">Questions</h3>

      <div class="question-grid">
        <button *ngFor="let num of questionNumbers"
                class="q-btn"
                [class.current]="num === currentQuestion"
                [class.answered]="answeredSet.has(num)"
                [class.marked]="markedSet.has(num)"
                [class.not-visited]="!visitedSet.has(num) && num !== currentQuestion"
                (click)="onQuestionClick(num)"
                [attr.aria-label]="'Question ' + num + ', ' + getStatusLabel(num)"
                [attr.aria-current]="num === currentQuestion ? 'true' : null">
          {{ num }}
        </button>
      </div>

      <!-- Legend -->
      <div class="legend" aria-label="Status legend">
        <div class="legend-item">
          <span class="legend-dot not-visited"></span>
          <span>Not Visited</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot current"></span>
          <span>Current</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot answered"></span>
          <span>Answered</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot marked"></span>
          <span>Marked for Review</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .palette-wrapper {
      padding: 16px;
      background: #fafafa;
      border-left: 1px solid #e0e0e0;
      height: 100%;
      overflow-y: auto;
    }
    .palette-title {
      font-size: 0.95rem;
      font-weight: 600;
      margin: 0 0 12px 0;
      color: #333;
    }
    .question-grid {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 8px;
    }
    .q-btn {
      width: 40px;
      height: 40px;
      border-radius: 6px;
      border: 2px solid #bdbdbd;
      background: #e0e0e0;
      font-weight: 500;
      font-size: 0.85rem;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.15s;
    }
    .q-btn:hover {
      border-color: #1565c0;
      transform: scale(1.05);
    }
    .q-btn:focus-visible {
      outline: 2px solid #1565c0;
      outline-offset: 2px;
    }
    .q-btn.not-visited {
      background: #e0e0e0;
      border-color: #bdbdbd;
      color: #616161;
    }
    .q-btn.current {
      background: #1565c0;
      border-color: #1565c0;
      color: white;
    }
    .q-btn.answered {
      background: #2e7d32;
      border-color: #2e7d32;
      color: white;
    }
    .q-btn.marked {
      background: #e65100;
      border-color: #e65100;
      color: white;
    }
    .legend {
      margin-top: 20px;
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      font-size: 0.8rem;
      color: #555;
    }
    .legend-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .legend-dot {
      width: 14px;
      height: 14px;
      border-radius: 3px;
    }
    .legend-dot.not-visited {
      background: #e0e0e0;
      border: 1px solid #bdbdbd;
    }
    .legend-dot.current {
      background: #1565c0;
    }
    .legend-dot.answered {
      background: #2e7d32;
    }
    .legend-dot.marked {
      background: #e65100;
    }
  `]
})
export class NavigationPaletteComponent {
  @Input() totalQuestions = 0;
  @Input() currentQuestion = 1;
  @Input() answeredSet = new Set<number>();
  @Input() markedSet = new Set<number>();
  @Input() visitedSet = new Set<number>();
  @Output() questionSelected = new EventEmitter<number>();

  get questionNumbers(): number[] {
    return Array.from({ length: this.totalQuestions }, (_, i) => i + 1);
  }

  onQuestionClick(num: number): void {
    this.questionSelected.emit(num);
  }

  getStatusLabel(num: number): string {
    if (num === this.currentQuestion) return 'current';
    if (this.markedSet.has(num)) return 'marked for review';
    if (this.answeredSet.has(num)) return 'answered';
    if (this.visitedSet.has(num)) return 'visited';
    return 'not visited';
  }
}
