import { Component, Input, Output, EventEmitter } from '@angular/core';
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
  template: `
    <div class="question-display" *ngIf="question">
      <!-- Question Header -->
      <div class="question-header">
        <span class="question-number">Question {{ question.sequenceNumber }}</span>
        <span class="question-type-badge">{{ question.questionType }}</span>
      </div>

      <!-- Question Content -->
      <div class="question-content" [innerHTML]="question.content" aria-label="Question content"></div>

      <!-- MCQ Options (Radio) -->
      <div class="options-list" *ngIf="question.questionType === 'MCQ' && question.options?.length">
        <mat-radio-group [value]="selectedOptionIds[0] || ''" (change)="onRadioChange($event.value)"
                         aria-label="Select one answer">
          <mat-radio-button *ngFor="let option of question.options; let i = index"
                            [value]="option.id"
                            class="option-item"
                            [attr.aria-label]="'Option ' + (i + 1) + ': ' + option.text">
            <span class="option-text">{{ option.text }}</span>
          </mat-radio-button>
        </mat-radio-group>
      </div>

      <!-- MSQ Options (Checkbox) -->
      <div class="options-list" *ngIf="question.questionType === 'MSQ' && question.options?.length">
        <div *ngFor="let option of question.options; let i = index" class="option-item">
          <mat-checkbox [checked]="selectedOptionIds.includes(option.id)"
                        (change)="onCheckboxChange(option.id, $event.checked)"
                        [attr.aria-label]="'Option ' + (i + 1) + ': ' + option.text">
            <span class="option-text">{{ option.text }}</span>
          </mat-checkbox>
        </div>
      </div>

      <!-- Numerical Input -->
      <div class="text-input-area" *ngIf="question.questionType === 'NUMERICAL'">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Enter numerical answer</mat-label>
          <input matInput type="number" [ngModel]="enteredValue"
                 (ngModelChange)="onValueChange($event)"
                 aria-label="Enter numerical answer">
        </mat-form-field>
      </div>

      <!-- Descriptive Input -->
      <div class="text-input-area" *ngIf="question.questionType === 'DESCRIPTIVE'">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Enter your answer</mat-label>
          <textarea matInput rows="6" [ngModel]="enteredValue"
                    (ngModelChange)="onValueChange($event)"
                    aria-label="Enter descriptive answer"></textarea>
        </mat-form-field>
      </div>
    </div>
  `,
  styles: [`
    .question-display {
      padding: 24px;
      max-width: 800px;
    }
    .question-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
    }
    .question-number {
      font-size: 1.1rem;
      font-weight: 600;
      color: #333;
    }
    .question-type-badge {
      font-size: 0.75rem;
      padding: 2px 8px;
      border-radius: 12px;
      background: #e3f2fd;
      color: #1565c0;
      font-weight: 500;
    }
    .question-content {
      font-size: 1rem;
      line-height: 1.7;
      margin-bottom: 24px;
      color: #212121;
    }
    .options-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .option-item {
      padding: 12px 16px;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      transition: background 0.15s, border-color 0.15s;
    }
    .option-item:hover {
      background: #f5f5f5;
    }
    .option-text {
      font-size: 0.95rem;
    }
    .text-input-area {
      max-width: 500px;
    }
    .full-width {
      width: 100%;
    }
    mat-radio-button {
      display: block;
      padding: 12px 16px;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      margin-bottom: 8px;
    }
    mat-radio-button:hover {
      background: #f5f5f5;
    }
  `]
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
