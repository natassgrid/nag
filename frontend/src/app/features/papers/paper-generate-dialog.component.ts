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
  SimpleChanges,
  ChangeDetectionStrategy,
  ChangeDetectorRef
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
import { catchError, finalize, switchMap, map } from 'rxjs/operators';
import { forkJoin, of, Observable } from 'rxjs';
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
import { SchedulingService, ShiftResponse } from '../exam/scheduling/scheduling.service';
import { RightDrawerComponent } from '../../shared/components/right-drawer/right-drawer.component';

export interface ShiftOption {
  id: string;
  label: string;
  scheduleName?: string;
  shiftNumber: number;
  shiftName?: string;
  timing?: string;
}

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
  changeDetection: ChangeDetectionStrategy.Default,
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
  mappedTemplates: BlueprintTemplateResponse[] = [];
  universalTemplates: BlueprintTemplateResponse[] = [];
  selectedTemplate: BlueprintTemplateResponse | null = null;
  loadingTemplates = false;
  showCustomRules = false;

  // Shift selection & pre-population
  availableShifts: ShiftOption[] = [];
  loadingShifts = false;
  isManualShift = false;

  private readonly UUID_PATTERN =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  constructor(
    private fb: FormBuilder,
    private paperService: PaperService,
    private examService: ExamManagementService,
    private schedulingService: SchedulingService,
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
        this.cdr.markForCheck();
      });
  }

  initForm(): void {
    const initialExamId = this.examId ?? '';
    this.form = this.fb.group({
      examId: [
        initialExamId,
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

    if (initialExamId) {
      this.loadShiftsForExam(initialExamId);
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
        this.categorizeTemplates();
        if (this.preselectedTemplate) {
          const match = list.find((t) => t.id === this.preselectedTemplate?.id);
          if (match) this.applyTemplate(match);
        }
        this.cdr.markForCheck();
      });
  }

  categorizeTemplates(): void {
    const currentExamId = this.form.get('examId')?.value?.trim();
    if (currentExamId) {
      this.mappedTemplates = this.templates.filter((t) => t.examId === currentExamId);
      this.universalTemplates = this.templates.filter((t) => !t.examId);
    } else {
      this.mappedTemplates = this.templates.filter((t) => !!t.examId);
      this.universalTemplates = this.templates.filter((t) => !t.examId);
    }
  }

  loadShiftsForExam(examId: string): void {
    if (!examId || !this.UUID_PATTERN.test(examId.trim())) {
      this.availableShifts = [];
      this.loadingShifts = false;
      this.isManualShift = true;
      this.cdr.markForCheck();
      return;
    }

    this.loadingShifts = true;
    this.schedulingService
      .listSchedules(examId.trim())
      .pipe(
        switchMap((schedules) => {
          if (!schedules || schedules.length === 0) {
            return of([] as ShiftOption[]);
          }
          const shiftRequests: Observable<ShiftOption[]>[] = schedules.map((s) =>
            this.schedulingService.listShifts(examId.trim(), s.id).pipe(
              map((shifts: ShiftResponse[]) =>
                (shifts || []).map((sh: ShiftResponse): ShiftOption => {
                  const shiftName = sh.shiftName || `Shift ${sh.shiftNumber}`;
                  const timing =
                    sh.examStartTime && sh.examEndTime
                      ? ` (${sh.examStartTime.substring(0, 5)} - ${sh.examEndTime.substring(0, 5)})`
                      : '';
                  return {
                    id: sh.id,
                    label: `#${sh.shiftNumber}: ${shiftName}${timing}`,
                    scheduleName: s.scheduleName,
                    shiftNumber: sh.shiftNumber,
                    shiftName: sh.shiftName,
                    timing
                  };
                })
              ),
              catchError(() => of([] as ShiftOption[]))
            )
          );
          return forkJoin(shiftRequests).pipe(map((res: ShiftOption[][]) => res.flat()));
        }),
        catchError(() => of([] as ShiftOption[])),
        finalize(() => {
          this.loadingShifts = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe((shifts: ShiftOption[]) => {
        this.availableShifts = shifts;
        if (shifts.length > 0) {
          // Pre-populate shift with first available shift mapped to this exam
          this.form.get('shiftId')?.setValue(shifts[0].id);
          this.isManualShift = false;
        } else {
          this.isManualShift = true;
        }
        this.cdr.markForCheck();
      });
  }

  onExamSelectionChange(examId: string): void {
    if (this.selectedTemplate && this.selectedTemplate.examId && this.selectedTemplate.examId !== examId) {
      this.selectedTemplate = null;
      this.form.get('templateId')?.setValue('');
      this.clearRules();
    }
    this.loadTemplates();
    this.loadShiftsForExam(examId);
  }

  onShiftSelectChange(val: string): void {
    if (val === '__manual__') {
      this.isManualShift = true;
      this.form.get('shiftId')?.setValue('');
    } else {
      this.isManualShift = false;
      this.form.get('shiftId')?.setValue(val);
    }
  }

  toggleManualShift(): void {
    this.isManualShift = !this.isManualShift;
    if (!this.isManualShift && this.availableShifts.length > 0) {
      this.form.get('shiftId')?.setValue(this.availableShifts[0].id);
    } else if (this.isManualShift) {
      this.form.get('shiftId')?.setValue('');
    }
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
      this.loadShiftsForExam(tpl.examId);
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
