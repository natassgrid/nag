/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
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
  ChangeDetectorRef,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import {
  PaperService,
  BlueprintFeasibilityResponse,
  BlueprintTemplateResponse,
  BlueprintRule,
  BlueprintFeasibilityRequest,
  RuleFeasibilityDetail
} from '../paper.service';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-blueprint-feasibility-modal',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    MatMenuModule,
    RightDrawerComponent
  ],
  templateUrl: './blueprint-feasibility-modal.component.html',
  styleUrls: ['./blueprint-feasibility-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BlueprintFeasibilityModalComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() template?: BlueprintTemplateResponse;
  @Input() customRules?: BlueprintRule[];
  @Input() examId?: string;
  @Input() shiftId?: string;
  @Input() examName?: string;
  @Output() close = new EventEmitter<void>();

  loading = false;
  notifying = false;
  feasibility: BlueprintFeasibilityResponse | null = null;
  error: string | null = null;
  showPromptPreview = false;

  constructor(
    private paperService: PaperService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    if (this.isOpen) {
      this.runAnalysis(false);
    }
  }

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
      (r) => r.status === 'DEFICIT' || (r.deficit && r.deficit > 0)
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
              'Failed to audit template sufficiency';
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

      const request: BlueprintFeasibilityRequest = {
        examId: this.examId || this.template?.examId,
        shiftId: this.shiftId,
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
      (acc, r) => acc + (r.deficit > 0 ? r.deficit : r.needed),
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
      const needed = rule.deficit > 0 ? rule.deficit : rule.needed;
      prompt += `### Rule ${idx + 1}: ${rule.subject} > ${rule.topic}\n`;
      prompt += `- **Subject:** ${rule.subject}\n`;
      prompt += `- **Topic:** ${rule.topic}\n`;
      prompt += `- **Difficulty:** ${rule.difficulty || 'MEDIUM'}\n`;
      prompt += `- **Cognitive Level (Bloom's):** ${rule.cognitiveLevel || 'APPLY'}\n`;
      prompt += `- **Deficit Needed:** ${needed} question(s)\n\n`;
    });

    prompt += `## Quality & Formatting Guidelines\n`;
    prompt += `1. **Standard:** Provide well-calibrated, unambiguous questions with 4 distinct options (A, B, C, D) for MCQs.\n`;
    prompt += `2. **Mathematical / Scientific Formulas:** Use standard LaTeX enclosed in single dollar signs (e.g., \`$E = mc^2$\` or \`$\\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}$\`).\n`;
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
        count: r.deficit > 0 ? r.deficit : r.needed
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
    this.copyToClipboard(
      prompt,
      'AI Question Generation prompt copied to clipboard!'
    );
  }

  copyAIBatchJson(): void {
    const json = this.generateAIBatchJson();
    this.copyToClipboard(
      json,
      'AI Batch Generation JSON payload copied to clipboard!'
    );
  }

  downloadAIPrompt(): void {
    const prompt = this.generateAIPrompt();
    const baseName = this.getSanitizedBaseName();
    this.downloadFile(`${baseName}_ai_question_prompt.md`, prompt, 'text/markdown');
    this.snackBar.open('Downloaded AI Question Generation Prompt (.md)', 'OK', {
      duration: 3000
    });
  }

  downloadAIBatchJson(): void {
    const json = this.generateAIBatchJson();
    const baseName = this.getSanitizedBaseName();
    this.downloadFile(
      `${baseName}_ai_batch_request.json`,
      json,
      'application/json'
    );
    this.snackBar.open('Downloaded AI Batch Generation Request (.json)', 'OK', {
      duration: 3000
    });
  }

  downloadAuditJson(): void {
    const json = this.generateAuditJson();
    const baseName = this.getSanitizedBaseName();
    this.downloadFile(
      `${baseName}_feasibility_audit.json`,
      json,
      'application/json'
    );
    this.snackBar.open('Downloaded Feasibility Audit Report (.json)', 'OK', {
      duration: 3000
    });
  }

  private getSanitizedBaseName(): string {
    const name = this.template?.name || this.examName || 'blueprint';
    return name
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '') || 'blueprint_audit';
  }

  private copyToClipboard(text: string, successMessage: string): void {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard
        .writeText(text)
        .then(() => {
          this.snackBar.open(successMessage, 'OK', { duration: 3500 });
        })
        .catch(() => {
          this.fallbackCopyText(text, successMessage);
        });
    } else {
      this.fallbackCopyText(text, successMessage);
    }
  }

  private fallbackCopyText(text: string, successMessage: string): void {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.opacity = '0';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    try {
      document.execCommand('copy');
      this.snackBar.open(successMessage, 'OK', { duration: 3500 });
    } catch {
      this.snackBar.open('Failed to copy to clipboard', 'Dismiss', {
        duration: 3000
      });
    }
    document.body.removeChild(textArea);
  }

  private downloadFile(
    filename: string,
    content: string,
    contentType: string = 'text/plain'
  ): void {
    const blob = new Blob([content], { type: contentType });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }

  onClose(): void {
    this.close.emit();
  }
}
