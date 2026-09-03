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
import { FormsModule } from '@angular/forms';
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Question } from '../services/exam.service';

@Component({
  selector: 'app-question-display',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatRadioModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './question-display.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./question-display.component.scss']
})
export class QuestionDisplayComponent {
  @Input() question: Question | null = null;
  @Input() selectedOptionIds: string[] = [];
  @Input() enteredValue = '';
  @Output() optionSelected = new EventEmitter<string[]>();
  @Output() valueEntered = new EventEmitter<string>();

  onRadioChange(optionId: string): void {
    this.optionSelected.emit([optionId]);
  }

  onCheckboxChange(optionId: string, checked: boolean): void {
    let updated: string[];
    if (checked) {
      updated = [...this.selectedOptionIds, optionId];
    } else {
      updated = this.selectedOptionIds.filter(id => id !== optionId);
    }
    this.optionSelected.emit(updated);
  }

  onValueChange(value: string): void {
    this.valueEntered.emit(value);
  }
}
