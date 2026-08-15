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
  styleUrls: ['./question-palette.component.scss']
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
