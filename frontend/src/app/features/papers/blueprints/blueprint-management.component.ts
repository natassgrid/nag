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

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  FormsModule
} from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

import {
  PaperService,
  BlueprintTemplateResponse,
  BlueprintTemplateRequest,
  BlueprintRule
} from '../paper.service';
import {
  ExamManagementService,
  ExaminationResponse
} from '../../exam/exam-manage/exam-management.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';
import { PaperGenerateDialogComponent } from '../paper-generate-dialog.component';

@Component({
  selector: 'app-blueprint-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDividerModule,
    MatTooltipModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    PageHeaderComponent,
    RightDrawerComponent,
    PaperGenerateDialogComponent
  ],
  templateUrl: './blueprint-management.component.html',
  styleUrls: ['./blueprint-management.component.scss']
})
export class BlueprintManagementComponent implements OnInit {
  templates: BlueprintTemplateResponse[] = [];
  filteredTemplates: BlueprintTemplateResponse[] = [];
  exams: ExaminationResponse[] = [];
  loading = true;
  saving = false;
  deletingId: string | null = null;
  searchQuery = '';
  selectedExamFilter = '';

  // Drawer State
  drawerOpen = false;
  editingTemplate: BlueprintTemplateResponse | null = null;
  form!: FormGroup;

  // Quick Generate Modal
  generateModalOpen = false;
  generateExamId?: string;
  selectedTemplateForGeneration?: BlueprintTemplateResponse;

  constructor(
    private paperService: PaperService,
    private examService: ExamManagementService,
    private fb: FormBuilder,
    private snackBar: MatSnackBar,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadExams();
    this.loadTemplates();
  }

  loadExams(): void {
    this.examService
      .getExams(0, 100)
      .pipe(catchError(() => of([])))
      .subscribe((res) => {
        this.exams = res;
        this.cdr.detectChanges();
      });
  }

  getExamName(examId?: string): string {
    if (!examId) return 'Universal';
    const found = this.exams.find((e) => e.id === examId);
    return found ? found.name : examId.substring(0, 8) + '…';
  }

  initForm(template?: BlueprintTemplateResponse): void {
    this.form = this.fb.group({
      name: [template?.name ?? '', [Validators.required, Validators.maxLength(100)]],
      description: [template?.description ?? '', [Validators.maxLength(255)]],
      examId: [template?.examId ?? ''],
      blueprintRules: this.fb.array([])
    });

    if (template && template.rules && template.rules.length > 0) {
      template.rules.forEach(r => this.addRule(r));
    } else {
      this.addRule();
    }
  }

  get rules(): FormArray {
    return this.form.get('blueprintRules') as FormArray;
  }

  get totalQuestionsInForm(): number {
    return this.rules.controls.reduce(
      (sum, c) => sum + (Number(c.get('questionCount')?.value) || 0),
      0
    );
  }

  addRule(rule?: BlueprintRule): void {
    this.rules.push(
      this.fb.group({
        subject: [rule?.subject ?? '', Validators.required],
        topic: [rule?.topic ?? '', Validators.required],
        difficulty: [rule?.difficulty ?? ''],
        cognitiveLevel: [rule?.cognitiveLevel ?? ''],
        questionCount: [rule?.questionCount ?? 5, [Validators.required, Validators.min(1)]]
      })
    );
  }

  removeRule(index: number): void {
    if (this.rules.length > 1) {
      this.rules.removeAt(index);
    } else {
      this.snackBar.open('A blueprint must have at least one rule', 'OK', { duration: 2500 });
    }
  }

  loadTemplates(): void {
    this.loading = true;
    this.paperService
      .getTemplates()
      .pipe(
        catchError(err => {
          this.snackBar.open('Failed to load blueprint templates', 'Dismiss', { duration: 3000 });
          return of([]);
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe(list => {
        this.templates = list;
        this.applyFilter();
      });
  }

  applyFilter(): void {
    const q = this.searchQuery.trim().toLowerCase();
    this.filteredTemplates = this.templates.filter(t => {
      const matchQuery =
        !q ||
        t.name.toLowerCase().includes(q) ||
        (t.description && t.description.toLowerCase().includes(q)) ||
        (t.examId && t.examId.toLowerCase().includes(q)) ||
        (t.rules && t.rules.some(r => r.subject.toLowerCase().includes(q) || r.topic.toLowerCase().includes(q)));

      const matchExam =
        !this.selectedExamFilter ||
        (this.selectedExamFilter === 'GLOBAL' ? !t.examId : t.examId === this.selectedExamFilter);

      return matchQuery && matchExam;
    });
    this.cdr.detectChanges();
  }

  openCreateDrawer(): void {
    this.editingTemplate = null;
    this.initForm();
    this.drawerOpen = true;
  }

  openEditDrawer(tpl: BlueprintTemplateResponse): void {
    this.editingTemplate = tpl;
    this.initForm(tpl);
    this.drawerOpen = true;
  }

  closeDrawer(): void {
    this.drawerOpen = false;
    this.editingTemplate = null;
  }

  saveBlueprint(): void {
    if (this.form.invalid || this.rules.length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.value;
    const request: BlueprintTemplateRequest = {
      name: v.name.trim(),
      description: v.description?.trim() || undefined,
      examId: v.examId?.trim() ? v.examId.trim() : undefined,
      rules: (this.rules.value as any[]).map(r => ({
        subject: r.subject.trim(),
        topic: r.topic.trim(),
        difficulty: r.difficulty || '',
        cognitiveLevel: r.cognitiveLevel || '',
        questionCount: Number(r.questionCount)
      }))
    };

    this.saving = true;
    const op$ = this.editingTemplate
      ? this.paperService.updateTemplate(this.editingTemplate.id, request)
      : this.paperService.createTemplate(request);

    op$
      .pipe(
        catchError(err => {
          const msg =
            err?.error?.detail ?? err?.error?.message ?? 'Failed to save blueprint template';
          this.snackBar.open(msg, 'Dismiss', { duration: 4000 });
          return of(null);
        }),
        finalize(() => {
          this.saving = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe(res => {
        if (!res) return;
        this.snackBar.open(
          `Blueprint template "${res.name}" ${this.editingTemplate ? 'updated' : 'created'} successfully`,
          'OK',
          { duration: 3000 }
        );
        this.closeDrawer();
        this.loadTemplates();
      });
  }

  duplicateTemplate(tpl: BlueprintTemplateResponse): void {
    const cloneRequest: BlueprintTemplateRequest = {
      name: `${tpl.name} (Copy)`,
      description: tpl.description,
      examId: tpl.examId,
      rules: tpl.rules ? tpl.rules.map(r => ({ ...r })) : []
    };

    this.loading = true;
    this.paperService
      .createTemplate(cloneRequest)
      .pipe(
        catchError(err => {
          this.snackBar.open('Failed to clone template', 'Dismiss', { duration: 3000 });
          return of(null);
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe(res => {
        if (!res) return;
        this.snackBar.open(`Cloned as "${res.name}"`, 'OK', { duration: 2500 });
        this.loadTemplates();
      });
  }

  deleteTemplate(tpl: BlueprintTemplateResponse, event: MouseEvent): void {
    event.stopPropagation();
    if (!confirm(`Are you sure you want to delete blueprint template "${tpl.name}"?`)) return;

    this.deletingId = tpl.id;
    this.paperService
      .deleteTemplate(tpl.id)
      .pipe(
        catchError(() => {
          this.snackBar.open('Failed to delete blueprint template', 'Dismiss', { duration: 3000 });
          return of(null);
        }),
        finalize(() => {
          this.deletingId = null;
          this.cdr.detectChanges();
        })
      )
      .subscribe(() => {
        this.snackBar.open(`Template "${tpl.name}" deleted`, 'OK', { duration: 2000 });
        this.templates = this.templates.filter(t => t.id !== tpl.id);
        this.applyFilter();
      });
  }

  useToGenerate(tpl: BlueprintTemplateResponse): void {
    this.selectedTemplateForGeneration = tpl;
    this.generateExamId = tpl.examId;
    this.generateModalOpen = true;
  }

  onGenerateClose(req: any): void {
    this.generateModalOpen = false;
    this.selectedTemplateForGeneration = undefined;
    if (req) {
      this.snackBar.open('Generating paper...', 'Close', { duration: 2500 });
      this.paperService.generatePaper(req).subscribe({
        next: res => {
          this.snackBar.open(`Paper generation initiated: ${res.paperId}`, 'View Papers', {
            duration: 5000
          }).onAction().subscribe(() => {
            this.router.navigate(['/papers']);
          });
        },
        error: err => {
          const msg = err?.error?.message ?? 'Paper generation failed';
          this.snackBar.open(msg, 'Dismiss', { duration: 4000 });
        }
      });
    }
  }

  getDistinctSubjects(tpl: BlueprintTemplateResponse): string[] {
    if (!tpl.rules) return [];
    return Array.from(new Set(tpl.rules.map(r => r.subject))).filter(Boolean);
  }

  getTotalQuestions(tpl: BlueprintTemplateResponse): number {
    if (tpl.totalQuestions != null) return tpl.totalQuestions;
    return (tpl.rules ?? []).reduce((sum, r) => sum + (r.questionCount ?? 0), 0);
  }

  getDifficultyBreakdown(tpl: BlueprintTemplateResponse): { easy: number; medium: number; hard: number } {
    const res = { easy: 0, medium: 0, hard: 0 };
    (tpl.rules ?? []).forEach(r => {
      const c = r.questionCount || 0;
      if (r.difficulty === 'EASY') res.easy += c;
      else if (r.difficulty === 'MEDIUM') res.medium += c;
      else if (r.difficulty === 'HARD') res.hard += c;
    });
    return res;
  }

  navigateBackToPapers(): void {
    this.router.navigate(['/papers']);
  }
}
