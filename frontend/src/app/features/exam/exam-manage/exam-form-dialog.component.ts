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

import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ExaminationResponse, CreateExamRequest } from './exam-management.service';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-exam-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatCheckboxModule,
    RightDrawerComponent
  ],
  templateUrl: './exam-form-dialog.component.html',
  styleUrls: ['./exam-form-dialog.component.scss']
})
export class ExamFormDialogComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() exam?: ExaminationResponse;
  @Output() close = new EventEmitter<CreateExamRequest | null>();

  form!: FormGroup;

  constructor(private fb: FormBuilder) {
    this.initForm();
  }

  ngOnInit(): void {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.initForm();
    }
  }

  initForm(): void {
    const exam = this.exam;
    this.form = this.fb.group({
      name: [exam?.name || '', Validators.required],
      code: [exam?.code || ''],
      conductingAuthority: [exam?.conductingAuthority || ''],
      category: [exam?.category || ''],
      examinationType: [exam?.examinationType || ''],
      academicYear: [exam?.academicYear || ''],
      examinationMode: [exam?.examinationMode || ''],
      durationMinutes: [exam?.durationMinutes || 60, [Validators.required, Validators.min(1)]],
      totalMarks: [exam?.totalMarks || 100, [Validators.required, Validators.min(1)]],
      negativeMarkingEnabled: [exam?.negativeMarkingEnabled || false],
      negativeMarkingValue: [exam?.negativeMarkingValue || 0],
      navigationPolicy: [exam?.navigationPolicy || 'FLEXIBLE', Validators.required],
      calculatorPolicy: [exam?.calculatorPolicy || 'NONE', Validators.required],
      reviewFlagEnabled: [exam?.reviewFlagEnabled || false]
    });
  }

  cancel(): void {
    this.close.emit(null);
  }

  save(): void {
    if (this.form.valid) {
      const value = this.form.value;
      const sections = this.exam?.sections?.length
        ? this.exam.sections
        : [{ name: 'Section 1', questionCount: value.totalMarks, marksPerQuestion: 1 }];

      this.close.emit({
        ...value,
        sections
      });
    }
  }
}
