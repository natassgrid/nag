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
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  FormControl
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatListModule } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';
import {
  PaperService,
  PaperGenerationRequest,
  BlueprintTemplateResponse
} from './paper.service';
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
    MatTabsModule,
    MatListModule,
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
  @Output() close = new EventEmitter<PaperGenerationRequest | null>();

  form!: FormGroup;
  templateNameCtrl = new FormControl('');
  templateDescCtrl = new FormControl('');

  activeTab = 0;
  templates: BlueprintTemplateResponse[] = [];
  loadingTemplates = false;
  savingTemplate = false;
  deletingId: string | null = null;
  selectedTemplateId: string | null = null;

  private readonly UUID_PATTERN =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  constructor(
    private fb: FormBuilder,
    private paperService: PaperService,
    private snackBar: MatSnackBar
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadTemplates();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.initForm();
      this.loadTemplates();
    }
  }

  initForm(): void {
    this.form = this.fb.group({
      examId: [
        this.examId ?? '',
        [Validators.required, Validators.pattern(this.UUID_PATTERN)]
      ],
      shiftId: ['', [Validators.required, Validators.maxLength(50)]],
      blueprintRules: this.fb.array([])
    });
    this.addRule();
  }

  get rules(): FormArray {
    return this.form.get('blueprintRules') as FormArray;
  }

  get totalQuestions(): number {
    return this.rules.controls.reduce(
      (sum, c) => sum + (Number(c.get('questionCount')?.value) || 0), 0
    );
  }

  addRule(): void {
    this.rules.push(this.fb.group({
      subject: ['', Validators.required],
      topic: ['', Validators.required],
      difficulty: [''],
      cognitiveLevel: [''],
      questionCount: [5, [Validators.required, Validators.min(1)]]
    }));
  }

  removeRule(i: number): void { this.rules.removeAt(i); }

  private clearRules(): void {
    while (this.rules.length) this.rules.removeAt(0);
  }

  loadTemplates(): void {
    this.loadingTemplates = true;
    const currentExamId = this.form.get('examId')?.value?.trim();
    this.paperService
      .getTemplates(currentExamId && this.UUID_PATTERN.test(currentExamId) ? currentExamId : undefined)
      .pipe(
        catchError(() => of([])),
        finalize(() => (this.loadingTemplates = false))
      )
      .subscribe(list => (this.templates = list));
  }

  loadTemplate(tpl: BlueprintTemplateResponse): void {
    this.clearRules();
    (tpl.rules ?? []).forEach(r => {
      this.rules.push(this.fb.group({
        subject: [r.subject, Validators.required],
        topic: [r.topic, Validators.required],
        difficulty: [r.difficulty ?? ''],
        cognitiveLevel: [r.cognitiveLevel ?? ''],
        questionCount: [r.questionCount ?? 5, [Validators.required, Validators.min(1)]]
      }));
    });
    this.selectedTemplateId = tpl.id;
    this.templateNameCtrl.setValue(tpl.name);
    this.templateDescCtrl.setValue(tpl.description ?? '');
    this.activeTab = 0;
    this.snackBar.open(`Template "${tpl.name}" loaded`, 'OK', { duration: 2500 });
  }

  saveCurrentAsTemplate(): void {
    const name = this.templateNameCtrl.value?.trim();
    if (!name || this.rules.length === 0) return;

    const currentExamId = this.form.get('examId')?.value?.trim();
    const request = {
      name,
      description: this.templateDescCtrl.value?.trim() || undefined,
      examId: currentExamId && this.UUID_PATTERN.test(currentExamId) ? currentExamId : undefined,
      rules: this.rulesValue()
    };

    this.savingTemplate = true;

    const existing = this.templates.find(t => t.name === name);
    const op$ = existing
      ? this.paperService.updateTemplate(existing.id, request)
      : this.paperService.createTemplate(request);

    op$
      .pipe(
        catchError(err => {
          const msg = err?.error?.detail ?? err?.error?.message ?? 'Failed to save template';
          this.snackBar.open(msg, 'Dismiss', { duration: 4000 });
          return of(null);
        }),
        finalize(() => (this.savingTemplate = false))
      )
      .subscribe(saved => {
        if (!saved) return;
        this.snackBar.open(`Template "${saved.name}" saved`, 'OK', { duration: 2500 });
        this.loadTemplates();
      });
  }

  deleteTemplate(tpl: BlueprintTemplateResponse): void {
    this.deletingId = tpl.id;
    this.paperService
      .deleteTemplate(tpl.id)
      .pipe(
        catchError(() => {
          this.snackBar.open('Failed to delete template', 'Dismiss', { duration: 3000 });
          return of(null);
        }),
        finalize(() => (this.deletingId = null))
      )
      .subscribe(() => {
        if (this.selectedTemplateId === tpl.id) this.selectedTemplateId = null;
        this.templates = this.templates.filter(t => t.id !== tpl.id);
        this.snackBar.open(`Template "${tpl.name}" deleted`, 'OK', { duration: 2000 });
      });
  }

  countRules(tpl: BlueprintTemplateResponse): number {
    return (tpl.rules ?? []).reduce((s, r) => s + (r.questionCount ?? 0), 0);
  }

  cancel(): void {
    this.close.emit(null);
  }

  submit(): void {
    if (this.form.invalid || this.rules.length === 0) return;
    const v = this.form.value;
    const request: PaperGenerationRequest = {
      examId: v.examId,
      shiftId: v.shiftId,
      blueprintRules: this.rulesValue()
    };
    this.close.emit(request);
  }

  private rulesValue() {
    return (this.rules.value as any[]).map(r => ({
      subject: r.subject,
      topic: r.topic,
      difficulty: r.difficulty || null,
      cognitiveLevel: r.cognitiveLevel || null,
      questionCount: Number(r.questionCount)
    }));
  }
}
