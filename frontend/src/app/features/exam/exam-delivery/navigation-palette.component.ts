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

import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';

export type QuestionStatus = 'not-visited' | 'current' | 'answered' | 'marked-for-review';

@Component({
  selector: 'app-navigation-palette',
  standalone: true,
  imports: [CommonModule, MatButtonModule],
  templateUrl: './navigation-palette.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./navigation-palette.component.scss']
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
