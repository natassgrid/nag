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
  template: `
    <app-right-drawer
      [isOpen]="isOpen"
      title="Generate Question Paper"
      width="580px"
      (close)="cancel()"
    >
      <div drawer-body>
        <mat-tab-group [(selectedIndex)]="activeTab" animationDuration="150ms">

          <mat-tab label="Build Blueprint">
            <form [formGroup]="form" class="generate-form" autocomplete="off">

              <div class="section-heading">Paper Identity</div>
              <div class="form-row">
                <mat-form-field appearance="outline" class="flex-2">
                  <mat-label>Exam ID</mat-label>
                  <input matInput formControlName="examId" placeholder="UUID of the examination" />
                  <mat-error *ngIf="form.get('examId')?.hasError('required')">Required</mat-error>
                  <mat-error *ngIf="form.get('examId')?.hasError('pattern')">Must be a valid UUID</mat-error>
                </mat-form-field>
                <mat-form-field appearance="outline" class="flex-1">
                  <mat-label>Shift ID</mat-label>
                  <input matInput formControlName="shiftId" placeholder="e.g. SHIFT-A" />
                  <mat-error *ngIf="form.get('shiftId')?.hasError('required')">Required</mat-error>
                </mat-form-field>
              </div>

              <mat-divider class="section-divider"></mat-divider>

              <div class="section-header">
                <div class="section-heading">Blueprint Rules</div>
                <div class="section-actions">
                  <button mat-stroked-button type="button" (click)="activeTab = 1"
                          matTooltip="Load a saved template">
                    <mat-icon>folder_open</mat-icon> Load Template
                  </button>
                  <button mat-stroked-button color="primary" type="button"
                          (click)="addRule()" [disabled]="rules.length >= 20">
                    <mat-icon>add</mat-icon> Add Rule
                  </button>
                </div>
              </div>

              <p class="hint-text" *ngIf="rules.length === 0">
                Add at least one rule, or load a saved template from the Templates tab.
              </p>

              <div formArrayName="blueprintRules" class="rules-list" *ngIf="rules.length > 0">
                <div *ngFor="let rule of rules.controls; let i = index"
                     [formGroupName]="i" class="rule-card">

                  <div class="rule-header">
                    <span class="rule-index">Rule {{ i + 1 }}</span>
                    <button mat-icon-button color="warn" type="button"
                            (click)="removeRule(i)" matTooltip="Remove rule"
                            aria-label="Remove blueprint rule">
                      <mat-icon>delete_outline</mat-icon>
                    </button>
                  </div>

                  <div class="form-row">
                    <mat-form-field appearance="outline" class="flex-1">
                      <mat-label>Subject</mat-label>
                      <input matInput formControlName="subject" placeholder="e.g. Mathematics" />
                      <mat-error *ngIf="rule.get('subject')?.hasError('required')">Required</mat-error>
                    </mat-form-field>
                    <mat-form-field appearance="outline" class="flex-1">
                      <mat-label>Topic</mat-label>
                      <input matInput formControlName="topic" placeholder="e.g. Algebra" />
                      <mat-error *ngIf="rule.get('topic')?.hasError('required')">Required</mat-error>
                    </mat-form-field>
                  </div>

                  <div class="form-row">
                    <mat-form-field appearance="outline" class="flex-1">
                      <mat-label>Difficulty</mat-label>
                      <mat-select formControlName="difficulty">
                        <mat-option value="">Any</mat-option>
                        <mat-option value="EASY">Easy</mat-option>
                        <mat-option value="MEDIUM">Medium</mat-option>
                        <mat-option value="HARD">Hard</mat-option>
                      </mat-select>
                    </mat-form-field>
                    <mat-form-field appearance="outline" class="flex-1">
                      <mat-label>Cognitive Level</mat-label>
                      <mat-select formControlName="cognitiveLevel">
                        <mat-option value="">Any</mat-option>
                        <mat-option value="REMEMBER">Remember</mat-option>
                        <mat-option value="UNDERSTAND">Understand</mat-option>
                        <mat-option value="APPLY">Apply</mat-option>
                        <mat-option value="ANALYZE">Analyze</mat-option>
                        <mat-option value="EVALUATE">Evaluate</mat-option>
                        <mat-option value="CREATE">Create</mat-option>
                      </mat-select>
                    </mat-form-field>
                    <mat-form-field appearance="outline" class="flex-shrink">
                      <mat-label>Count</mat-label>
                      <input matInput type="number" formControlName="questionCount" min="1" max="200" />
                      <mat-error *ngIf="rule.get('questionCount')?.hasError('min')">Min 1</mat-error>
                    </mat-form-field>
                  </div>
                </div>
              </div>

              <div class="summary" *ngIf="rules.length > 0">
                <mat-icon class="summary-icon">info_outline</mat-icon>
                <span>{{ totalQuestions }} question(s) across {{ rules.length }} rule(s)</span>
              </div>

              <ng-container *ngIf="rules.length > 0">
                <mat-divider class="section-divider"></mat-divider>
                <div class="section-heading">Save as Template (optional)</div>
                <div class="form-row save-template-row">
                  <mat-form-field appearance="outline" class="flex-2">
                    <mat-label>Template Name</mat-label>
                    <input matInput [formControl]="templateNameCtrl"
                           placeholder="e.g. JEE Mains 2026 Blueprint" />
                  </mat-form-field>
                  <mat-form-field appearance="outline" class="flex-2">
                    <mat-label>Description (optional)</mat-label>
                    <input matInput [formControl]="templateDescCtrl"
                           placeholder="Short note about this template" />
                  </mat-form-field>
                  <button mat-stroked-button color="accent" type="button"
                          [disabled]="!templateNameCtrl.value?.trim() || savingTemplate"
                          (click)="saveCurrentAsTemplate()">
                    <mat-spinner diameter="16" *ngIf="savingTemplate" class="btn-spinner-sm"></mat-spinner>
                    <mat-icon *ngIf="!savingTemplate">save</mat-icon>
                    {{ savingTemplate ? 'Saving…' : 'Save Template' }}
                  </button>
                </div>
              </ng-container>

            </form>
          </mat-tab>

          <mat-tab>
            <ng-template mat-tab-label>
              Saved Templates
              <span class="tab-badge" *ngIf="templates.length > 0">{{ templates.length }}</span>
            </ng-template>

            <div class="templates-tab">
              <div class="templates-loading" *ngIf="loadingTemplates">
                <mat-spinner diameter="32"></mat-spinner>
                <span>Loading templates…</span>
              </div>

              <div class="templates-empty" *ngIf="!loadingTemplates && templates.length === 0">
                <mat-icon>description</mat-icon>
                <p>No saved templates yet.</p>
                <p class="hint-text">
                  Build your rules in the Build Blueprint tab, then save them as a template for reuse.
                </p>
              </div>

              <div class="template-list" *ngIf="!loadingTemplates && templates.length > 0">
                <div *ngFor="let tpl of templates" class="template-item"
                     [class.template-item--selected]="selectedTemplateId === tpl.id">

                  <div class="template-info">
                    <div class="template-name">{{ tpl.name }}</div>
                    <div class="template-meta" *ngIf="tpl.description">{{ tpl.description }}</div>
                    <div class="template-chips">
                      <mat-chip-set>
                        <mat-chip class="chip-count">
                          {{ tpl.totalQuestions ?? countRules(tpl) }} questions
                        </mat-chip>
                        <mat-chip class="chip-rules">
                          {{ tpl.rules.length }} rule(s)
                        </mat-chip>
                        <mat-chip *ngIf="tpl.examId" class="chip-exam"
                                  matTooltip="Pinned to exam {{ tpl.examId }}">
                          Exam-specific
                        </mat-chip>
                      </mat-chip-set>
                    </div>
                  </div>

                  <div class="template-actions">
                    <button mat-stroked-button color="primary" type="button"
                            (click)="loadTemplate(tpl)" matTooltip="Load rules into the builder">
                      <mat-icon>download</mat-icon> Use
                    </button>
                    <button mat-icon-button color="warn" type="button"
                            (click)="deleteTemplate(tpl)" matTooltip="Delete template"
                            aria-label="Delete template">
                      <mat-spinner diameter="18" *ngIf="deletingId === tpl.id"></mat-spinner>
                      <mat-icon *ngIf="deletingId !== tpl.id">delete_outline</mat-icon>
                    </button>
                  </div>
                </div>
              </div>

              <div class="templates-footer">
                <button mat-stroked-button type="button" (click)="loadTemplates()"
                        [disabled]="loadingTemplates">
                  <mat-icon>refresh</mat-icon> Refresh
                </button>
              </div>
            </div>
          </mat-tab>

        </mat-tab-group>
      </div>

      <div drawer-footer>
        <button mat-button (click)="cancel()" type="button">Cancel</button>
        <button mat-raised-button color="primary" type="button"
                [disabled]="form.invalid || rules.length === 0"
                (click)="submit()">
          <mat-icon>auto_awesome</mat-icon>
          Generate Paper
        </button>
      </div>
    </app-right-drawer>
  `,
  styles: [`
    .generate-form {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding: 16px 0 8px;
    }
    .section-heading {
      font-size: 12px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: #616161;
    }
    .section-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
    }
    .section-actions {
      display: flex;
      gap: 8px;
    }
    .section-divider { margin: 6px 0 14px; }
    .form-row {
      display: flex;
      gap: 12px;
      align-items: flex-start;
    }
    .flex-1 { flex: 1; min-width: 0; }
    .flex-2 { flex: 2; min-width: 0; }
    .flex-shrink { flex: 0 0 100px; }
    .hint-text { color: #9e9e9e; font-size: 13px; margin: 4px 0; }
    .rules-list { display: flex; flex-direction: column; gap: 10px; }
    .rule-card {
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      padding: 10px 14px 2px;
      background: #fafafa;
    }
    .rule-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;
    }
    .rule-index { font-size: 12px; font-weight: 700; color: #1976d2; }
    .summary {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: #388e3c;
      background: #f1f8e9;
      border-radius: 6px;
      padding: 8px 12px;
    }
    .summary-icon { font-size: 18px; height: 18px; width: 18px; }
    .save-template-row { align-items: center; flex-wrap: wrap; }
    .btn-spinner-sm {
      display: inline-block;
      margin-right: 4px;
      vertical-align: middle;
    }
    .templates-tab {
      padding: 16px 0;
      min-height: 260px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .templates-loading, .templates-empty {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 10px;
      color: #9e9e9e;
      padding: 32px;
    }
    .templates-empty mat-icon {
      font-size: 48px; height: 48px; width: 48px;
    }
    .template-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .template-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      background: #fff;
      transition: border-color 0.15s, background 0.15s;
    }
    .template-item:hover { border-color: #90caf9; background: #f5f9ff; }
    .template-item--selected { border-color: #1976d2; background: #e3f2fd; }
    .template-info { flex: 1; min-width: 0; }
    .template-name {
      font-weight: 600;
      font-size: 14px;
      color: #212121;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .template-meta {
      font-size: 12px;
      color: #757575;
      margin: 2px 0 6px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .template-chips mat-chip-set { display: flex; gap: 4px; }
    ::ng-deep .chip-count { background: #e3f2fd !important; color: #1565c0 !important; font-size: 11px !important; }
    ::ng-deep .chip-rules { background: #f3e5f5 !important; color: #6a1b9a !important; font-size: 11px !important; }
    ::ng-deep .chip-exam  { background: #fff8e1 !important; color: #f57f17 !important; font-size: 11px !important; }
    .template-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
    .templates-footer { display: flex; justify-content: flex-end; padding-top: 4px; }
    .tab-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      background: #1976d2;
      color: white;
      border-radius: 10px;
      min-width: 18px;
      height: 18px;
      font-size: 11px;
      font-weight: 600;
      padding: 0 5px;
      margin-left: 6px;
    }
  `]
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
