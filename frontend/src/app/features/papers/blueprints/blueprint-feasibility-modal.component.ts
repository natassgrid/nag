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
  OnChanges,
  SimpleChanges,
  ChangeDetectionStrategy,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';
import {
  PaperService,
  BlueprintTemplateResponse,
  BlueprintRule,
  BlueprintFeasibilityResponse,
  BlueprintFeasibilityRequest,
  RuleFeasibilityDetail
} from '../paper.service';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-blueprint-feasibility-modal',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatSnackBarModule,
    MatMenuModule,
    MatTooltipModule,
    MatDividerModule,
    RightDrawerComponent
  ],
  templateUrl: './blueprint-feasibility-modal.component.html',
  styleUrls: ['./blueprint-feasibility-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BlueprintFeasibilityModalComponent implements OnChanges {
  @Input() isOpen = false;
  @Input() template?: BlueprintTemplateResponse;
  @Input() customRules?: BlueprintRule[];
  @Input() examId?: string;
  @Input() examName?: string;
  @Input() shiftId?: string;

  @Output() close = new EventEmitter<void>();

  loading = false;
  feasibility?: BlueprintFeasibilityResponse;
  error: string | null = null;
  showPromptPreview = false;

  constructor(
    private paperService: PaperService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.runAnalysis(false);
    }
  }

  get title(): string {
    if (this.template?.name) {
      return `Feasibility Analysis: ${this.template.name}`;
    }
    return 'Blueprint Sufficiency & Feasibility Analysis';
  }

  get hasDeficits(): boolean {
    return (
      (this.feasibility?.gaps && this.feasibility.gaps.length > 0) ||
      (this.feasibility?.deficitRuleCount ? this.feasibility.deficitRuleCount > 0 : false)
    );
  }

  get deficitRules(): RuleFeasibilityDetail[] {
    if (!this.feasibility?.ruleDetails) return [];
    return this.feasibility.ruleDetails.filter(
      (r: RuleFeasibilityDetail) => r.status === 'DEFICIT' || ((r.deficit ?? 0) > 0)
    );
  }

  get targetRulesForAi(): RuleFeasibilityDetail[] {
    const deficits = this.deficitRules;
    if (deficits.length > 0) return deficits;
    return this.feasibility?.ruleDetails || [];
  }

  runAnalysis(notifyAdmin: boolean = false): void {
    this.loading = true;
    this.error = null;
    this.showPromptPreview = false;
    this.cdr.markForCheck();

    if (this.template?.id) {
      this.paperService
        .checkTemplateSufficiency(this.template.id, notifyAdmin)
        .pipe(
          catchError((err) => {
            const msg =
              err?.error?.detail ??
              err?.error?.message ??
              'Failed to verify template sufficiency';
            this.error = msg;
            this.snackBar.open(msg, 'Dismiss', { duration: 4000 });
            return of(null);
          }),
          finalize(() => {
            this.loading = false;
            this.cdr.markForCheck();
          })
        )
        .subscribe((res) => {
          if (res) {
            this.feasibility = res;
            if (notifyAdmin && res.notificationDispatched) {
              this.snackBar.open(
                'Deficit alert dispatched to Exam Administrators and Audit Topic.',
                'OK',
                { duration: 4000 }
              );
            }
          }
        });
    } else {
      const rules = this.customRules || this.template?.rules || [];
      if (rules.length === 0) {
        this.loading = false;
        this.error = 'No blueprint rules defined to check.';
        this.cdr.markForCheck();
        return;
      }

      const request: any = {
        examId: this.examId || this.template?.examId,
        shiftId: this.shiftId,
        rules,
        blueprintRules: rules,
        notifyAdminOnDeficit: notifyAdmin
      };

      this.paperService
        .checkBlueprintSufficiency(request)
        .pipe(
          catchError((err) => {
            const msg =
              err?.error?.detail ??
              err?.error?.message ??
              'Failed to verify blueprint sufficiency';
            this.error = msg;
            this.snackBar.open(msg, 'Dismiss', { duration: 4000 });
            return of(null);
          }),
          finalize(() => {
            this.loading = false;
            this.cdr.markForCheck();
          })
        )
        .subscribe((res) => {
          if (res) {
            this.feasibility = res;
            if (notifyAdmin && res.notificationDispatched) {
              this.snackBar.open(
                'Deficit alert dispatched to Exam Administrators and Audit Topic.',
                'OK',
                { duration: 4000 }
              );
            }
          }
        });
    }
  }

  notifyAdmin(): void {
    this.runAnalysis(true);
  }

  togglePromptPreview(): void {
    this.showPromptPreview = !this.showPromptPreview;
    this.cdr.markForCheck();
  }

  generateAIPrompt(): string {
    const rules = this.targetRulesForAi;
    const blueprintName = this.template?.name || 'Assessment Blueprint';
    const examScope = this.examName || this.examId || 'General Assessment';
    const totalDeficit = rules.reduce(
      (acc, r) => acc + ((r.deficit ?? 0) > 0 ? r.deficit! : (r.needed ?? 0)),
      0
    );

    let prompt = `# Task: Generate Assessment Questions for Question Bank Deficits\n\n`;
    prompt += `You are an expert psychometric assessment and item author for the National Assessment Grid (NAG) DPI Examination Platform.\n`;
    prompt += `Generate high-quality assessment questions satisfying the exact blueprint distribution shortages specified below.\n\n`;

    prompt += `## Context\n`;
    prompt += `- **Blueprint:** ${blueprintName}\n`;
    prompt += `- **Exam Scope:** ${examScope}\n`;
    if (this.shiftId) {
      prompt += `- **Shift:** ${this.shiftId}\n`;
    }
    prompt += `- **Target Deficit / Needed Count:** ${totalDeficit} question(s)\n\n`;

    prompt += `## Required Question Breakdown\n`;
    prompt += `Generate the specified count of items for each rule below:\n\n`;

    rules.forEach((rule, idx) => {
      const needed = (rule.deficit ?? 0) > 0 ? rule.deficit! : (rule.needed ?? 0);
      prompt += `### Rule ${idx + 1}: ${rule.subject} > ${rule.topic}\n`;
      prompt += `- **Subject:** ${rule.subject}\n`;
      prompt += `- **Topic:** ${rule.topic}\n`;
      prompt += `- **Difficulty:** ${rule.difficulty || 'MEDIUM'}\n`;
      prompt += `- **Cognitive Level (Bloom's):** ${rule.cognitiveLevel || 'APPLY'}\n`;
      prompt += `- **Deficit Needed:** ${needed} question(s)\n\n`;
    });

    prompt += `## Quality & Formatting Guidelines\n`;
    prompt += `1. **Standard:** Provide well-calibrated, unambiguous questions with 4 distinct options (A, B, C, D) for MCQs.\n`;
    prompt += `2. **Mathematical / Scientific Formulas:** Use standard LaTeX enclosed in single dollar signs (e.g., \`$E = mc^2\` or \`$\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}$\`).\n`;
    prompt += `3. **Distractors:** Ensure incorrect options represent plausible student misconceptions, not trivial mistakes.\n`;
    prompt += `4. **Explanations:** Include comprehensive, pedagogical step-by-step explanations for the correct answer.\n\n`;

    prompt += `## Expected Output Schema\n`;
    prompt += `Return ONLY a valid JSON array of question objects matching the schema below without enclosing markdown ticks outside JSON:\n\n`;

    const sampleJson = rules.map((r, i) => ({
      subject: r.subject,
      topic: r.topic,
      difficulty: r.difficulty || 'MEDIUM',
      cognitiveLevel: r.cognitiveLevel || 'APPLY',
      questionType: 'SINGLE_MCQ',
      content: `Sample question text for ${r.subject} - ${r.topic} with LaTeX if applicable...`,
      explanation: 'Comprehensive step-by-step solution explaining why A is correct and why other options are wrong.',
      answerKey: 'A',
      options: [
        { id: 'A', text: 'Correct Answer Option', isCorrect: true },
        { id: 'B', text: 'Plausible Distractor 1', isCorrect: false },
        { id: 'C', text: 'Plausible Distractor 2', isCorrect: false },
        { id: 'D', text: 'Plausible Distractor 3', isCorrect: false }
      ]
    }));

    prompt += `\`\`\`json\n${JSON.stringify(sampleJson.slice(0, 2), null, 2)}\n\`\`\`\n`;

    return prompt;
  }

  generateAIBatchJson(): string {
    const rules = this.targetRulesForAi;
    const batchRequest = {
      avoidDuplicates: true,
      items: rules.map((r) => ({
        subject: r.subject,
        topic: r.topic,
        difficulty: r.difficulty || 'MEDIUM',
        cognitiveLevel: r.cognitiveLevel || 'APPLY',
        questionType: 'SINGLE_MCQ',
        count: (r.deficit ?? 0) > 0 ? r.deficit! : (r.needed ?? 0)
      }))
    };
    return JSON.stringify(batchRequest, null, 2);
  }

  generateAuditJson(): string {
    return JSON.stringify(
      {
        templateName: this.template?.name,
        templateId: this.template?.id,
        examId: this.examId || this.template?.examId,
        examName: this.examName,
        shiftId: this.shiftId,
        feasibility: this.feasibility
      },
      null,
      2
    );
  }

  copyAIPrompt(): void {
    const prompt = this.generateAIPrompt();
    navigator.clipboard.writeText(prompt).then(() => {
      this.snackBar.open(
        'AI Generation Prompt copied to clipboard! Paste into ChatGPT/Claude.',
        'OK',
        { duration: 3500 }
      );
    });
  }

  downloadAIPrompt(): void {
    const prompt = this.generateAIPrompt();
    const blob = new Blob([prompt], { type: 'text/markdown;charset=utf-8;' });
    const filename = `ai-question-generation-${this.template?.name ? this.template.name.toLowerCase().replace(/\\s+/g, '-') : 'blueprint'}-${Date.now()}.md`;
    this.downloadFile(blob, filename);
  }

  copyAIBatchJson(): void {
    const json = this.generateAIBatchJson();
    navigator.clipboard.writeText(json).then(() => {
      this.snackBar.open(
        'AI Batch JSON configuration copied to clipboard!',
        'OK',
        { duration: 3500 }
      );
    });
  }

  downloadAIBatchJson(): void {
    const json = this.generateAIBatchJson();
    const blob = new Blob([json], { type: 'application/json;charset=utf-8;' });
    const filename = `ai-batch-request-${this.template?.name ? this.template.name.toLowerCase().replace(/\\s+/g, '-') : 'blueprint'}-${Date.now()}.json`;
    this.downloadFile(blob, filename);
  }

  downloadAuditJson(): void {
    const json = this.generateAuditJson();
    const blob = new Blob([json], { type: 'application/json;charset=utf-8;' });
    const filename = `blueprint-audit-report-${this.template?.id || 'adhoc'}-${Date.now()}.json`;
    this.downloadFile(blob, filename);
  }

  private downloadFile(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
    this.snackBar.open(`Downloaded ${filename}`, 'OK', { duration: 3000 });
  }

  onClose(): void {
    this.close.emit();
  }
}
