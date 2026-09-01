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

import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnChanges,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';
import {
  PaperService,
  PaperGenerationRequest,
  BlueprintTemplateResponse,
  BlueprintRule
} from './paper.service';
import {
  ExamManagementService,
  ExaminationResponse
} from '../exam/exam-manage/exam-management.service';
import { RightDrawerComponent } from '../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-paper-generate-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatSnackBarModule,
    RightDrawerComponent
  ],
  templateUrl: './paper-generate-dialog.component.html',
  styleUrls: ['./paper-generate-dialog.component.scss']
})
export class PaperGenerateDialogComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() examId?: string;
  @Input() preselectedTemplate?: BlueprintTemplateResponse;
  @Output() close = new EventEmitter<PaperGenerationRequest | null>();

  form!: FormGroup;
  exams: ExaminationResponse[] = [];
  templates: BlueprintTemplateResponse[] = [];
  selectedTemplate: BlueprintTemplateResponse | null = null;
  loadingTemplates = false;
  showCustomRules = false;

  private readonly UUID_PATTERN =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  constructor(
    private fb: FormBuilder,
    private paperService: PaperService,
    private examService: ExamManagementService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadExams();
    this.loadTemplates();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.initForm();
      this.loadExams();
      this.loadTemplates();
    }
    if (changes['preselectedTemplate'] && this.preselectedTemplate) {
      this.applyTemplate(this.preselectedTemplate);
    }
  }

  loadExams(): void {
    this.examService
      .getExams(0, 100)
      .pipe(catchError(() => of([])))
      .subscribe((res) => {
        this.exams = res;
      });
  }

  initForm(): void {
    this.form = this.fb.group({
      examId: [
        this.examId ?? '',
        [Validators.required]
      ],
      shiftId: ['', [Validators.required, Validators.maxLength(50)]],
      templateId: [''],
      blueprintRules: this.fb.array([])
    });

    if (this.preselectedTemplate) {
      this.applyTemplate(this.preselectedTemplate);
    } else {
      this.showCustomRules = false;
    }
  }

  get rules(): FormArray {
    return this.form.get('blueprintRules') as FormArray;
  }

  get totalQuestions(): number {
    return this.rules.controls.reduce(
      (sum, c) => sum + (Number(c.get('questionCount')?.value) || 0),
      0
    );
  }

  loadTemplates(): void {
    this.loadingTemplates = true;
    const currentExamId = this.form.get('examId')?.value?.trim();
    this.paperService
      .getTemplates(
        currentExamId && this.UUID_PATTERN.test(currentExamId) ? currentExamId : undefined
      )
      .pipe(
        catchError(() => of([])),
        finalize(() => (this.loadingTemplates = false))
      )
      .subscribe((list) => {
        this.templates = list;
        if (this.preselectedTemplate) {
          const match = list.find((t) => t.id === this.preselectedTemplate?.id);
          if (match) this.applyTemplate(match);
        }
      });
  }

  onExamSelectionChange(examId: string): void {
    this.loadTemplates();
  }

  onTemplateSelectionChange(templateId: string): void {
    if (!templateId) {
      this.selectedTemplate = null;
      this.clearRules();
      return;
    }
    const tpl = this.templates.find((t) => t.id === templateId);
    if (tpl) {
      this.applyTemplate(tpl);
    }
  }

  applyTemplate(tpl: BlueprintTemplateResponse): void {
    this.selectedTemplate = tpl;
    this.form.get('templateId')?.setValue(tpl.id);
    if (tpl.examId && !this.form.get('examId')?.value) {
      this.form.get('examId')?.setValue(tpl.examId);
    }
    this.clearRules();
    (tpl.rules ?? []).forEach((r) => this.addRule(r));
  }

  addRule(rule?: BlueprintRule): void {
    this.rules.push(
      this.fb.group({
        subject: [rule?.subject ?? '', Validators.required],
        topic: [rule?.topic ?? '', Validators.required],
        difficulty: [rule?.difficulty ?? ''],
        cognitiveLevel: [rule?.cognitiveLevel ?? ''],
        questionCount: [
          rule?.questionCount ?? 5,
          [Validators.required, Validators.min(1)]
        ]
      })
    );
  }

  removeRule(index: number): void {
    this.rules.removeAt(index);
  }

  private clearRules(): void {
    while (this.rules.length) this.rules.removeAt(0);
  }

  toggleCustomRules(): void {
    this.showCustomRules = !this.showCustomRules;
    if (this.showCustomRules && this.rules.length === 0) {
      this.addRule();
    }
  }

  goToBlueprintManagement(): void {
    this.close.emit(null);
    this.router.navigate(['/papers/blueprints']);
  }

  getDistinctSubjects(): string[] {
    const raw = (this.rules.value as any[]).map((r) => r.subject).filter(Boolean);
    return Array.from(new Set(raw));
  }

  cancel(): void {
    this.close.emit(null);
  }

  submit(): void {
    if (this.form.invalid || this.rules.length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.value;
    const request: PaperGenerationRequest = {
      examId: v.examId.trim(),
      shiftId: v.shiftId.trim(),
      blueprintRules: (this.rules.value as any[]).map((r) => ({
        subject: r.subject.trim(),
        topic: r.topic.trim(),
        difficulty: r.difficulty || '',
        cognitiveLevel: r.cognitiveLevel || '',
        questionCount: Number(r.questionCount)
      }))
    };

    this.close.emit(request);
  }
}
