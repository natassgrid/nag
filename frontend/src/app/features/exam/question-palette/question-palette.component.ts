/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

export interface ExamSection {
  sectionId: string;
  name: string;
  questionCount: number;
  marksPerQuestion: number;
}

@Component({
  selector: 'app-question-palette',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './question-palette.component.html',
  styles: [`
    .palette-container {
      position: fixed;
      right: 0;
      top: 80px;
      bottom: 80px;
      width: 280px;
      background: var(--color-surface);
      border-left: 1px solid var(--color-border);
      padding: var(--spacing-md);
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: var(--spacing-md);
    }
    .palette-header { font-weight: 500; font-size: 1rem; margin-bottom: var(--spacing-sm); }
    .section-tabs {
      display: flex; gap: var(--spacing-xs); flex-wrap: wrap;
      margin-bottom: var(--spacing-sm);
    }
    .section-tab {
      padding: var(--spacing-xs) var(--spacing-sm);
      border: 1px solid var(--color-border);
      border-radius: 4px;
      background: transparent;
      cursor: pointer;
      min-width: 44px; min-height: 44px;
    }
    .section-tab.active { background: var(--color-primary); color: white; border-color: var(--color-primary); }
    .question-grid {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: var(--spacing-xs);
    }
    .q-btn {
      width: 44px; height: 44px;
      border-radius: 4px;
      border: 2px solid var(--color-border);
      display: flex; align-items: center; justify-content: center;
      cursor: pointer; font-weight: 500;
      background: white;
      transition: all 0.15s;
    }
    .q-btn:hover { border-color: var(--color-primary); }
    .q-btn:focus-visible { outline: var(--focus-outline); outline-offset: var(--focus-offset); }
    .q-btn.answered { background: #1b5e20; color: white; border-color: #1b5e20; }
    .q-btn.flagged { background: #e65100; color: white; border-color: #e65100; }
    .q-btn.current { border-color: var(--color-primary); box-shadow: 0 0 0 2px var(--color-primary); }
    .legend {
      display: flex; flex-wrap: wrap; gap: var(--spacing-sm);
      margin-top: var(--spacing-md); font-size: 0.85rem;
    }
    .legend-item { display: flex; align-items: center; gap: 4px; }
    .legend-dot { width: 14px; height: 14px; border-radius: 2px; border: 1px solid var(--color-border); }
    .legend-dot.answered { background: #1b5e20; }
    .legend-dot.flagged { background: #e65100; }
    .legend-dot.unanswered { background: white; }
  `]
})
export class QuestionPaletteComponent {
  @Input() totalQuestions = 0;
  @Input() sections: ExamSection[] = [];
  @Input() answeredQuestions = new Set<number>();
  @Input() flaggedQuestions = new Set<number>();
  @Input() currentQuestion = 1;

  @Output() questionSelected = new EventEmitter<number>();
  @Output() reviewToggled = new EventEmitter<number>();

  activeSection: string | null = null;

  get questionNumbers(): number[] {
    return Array.from({ length: this.totalQuestions }, (_, i) => i + 1);
  }

  selectQuestion(seq: number): void {
    this.questionSelected.emit(seq);
  }

  toggleReview(seq: number): void {
    this.reviewToggled.emit(seq);
  }

  selectSection(sectionId: string): void {
    this.activeSection = sectionId;
  }

  getStatusLabel(seq: number): string {
    if (this.flaggedQuestions.has(seq)) return 'flagged for review';
    if (this.answeredQuestions.has(seq)) return 'answered';
    return 'not answered';
  }
}
