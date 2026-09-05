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
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';

import { QuestionResponse } from '../question.service';
import {
  TranslationService,
  TranslationResponse,
  TranslationRequest,
  SUPPORTED_LANGUAGES,
  SupportedLanguage
} from './translation.service';
import { AuthService } from '../../../core/services/auth.service';
import { MathRendererComponent } from '../../../shared/components/math-renderer/math-renderer.component';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-question-translation-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDividerModule,
    MatTabsModule,
    MatSnackBarModule,
    MathRendererComponent,
    RightDrawerComponent
  ],
  templateUrl: './question-translation-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./question-translation-dialog.component.scss']
})
export class QuestionTranslationDialogComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() question?: QuestionResponse;
  @Input() initialLanguageCode?: string;
  @Output() close = new EventEmitter<boolean>();

  languages: SupportedLanguage[] = SUPPORTED_LANGUAGES;
  selectedLanguageCode = 'hi';

  translationsMap = new Map<string, TranslationResponse>();
  activeTranslation?: TranslationResponse;
  loadingTranslations = false;

  form!: FormGroup;
  optionTranslations: { id: string; sourceText: string; text: string; isCorrect: boolean }[] = [];

  saving = false;
  approving = false;
  rejecting = false;
  autoTranslating = false;
  showRejectInput = false;
  rejectComments = '';
  errorMessage = '';
  activeTab = 0; // 0 = Edit Translation, 1 = Side-by-side Preview

  constructor(
    private fb: FormBuilder,
    private translationService: TranslationService,
    private authService: AuthService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.initForm();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      if (this.initialLanguageCode) {
        this.selectedLanguageCode = this.initialLanguageCode;
      }
      this.loadTranslations();
    } else if (changes['question'] && this.isOpen) {
      this.loadTranslations();
    }
  }

  initForm(): void {
    this.form = this.fb.group({
      translatedContent: ['', Validators.required],
      translatedExplanation: ['']
    });
  }

  loadTranslations(): void {
    if (!this.question?.id) return;
    this.loadingTranslations = true;
    this.errorMessage = '';
    this.translationsMap.clear();

    this.translationService.listTranslationsForQuestion(this.question.id).subscribe({
      next: (list) => {
        this.loadingTranslations = false;
        if (Array.isArray(list)) {
          list.forEach(t => this.translationsMap.set(t.languageCode, t));
        }
        this.switchLanguage(this.selectedLanguageCode);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadingTranslations = false;
        this.switchLanguage(this.selectedLanguageCode);
        this.cdr.markForCheck();
      }
    });
  }

  switchLanguage(langCode: string): void {
    this.selectedLanguageCode = langCode;
    this.activeTranslation = this.translationsMap.get(langCode);
    this.showRejectInput = false;
    this.rejectComments = '';
    this.errorMessage = '';

    // Populate options from source question
    const sourceOpts = this.question?.options || [];
    const translatedOpts = this.activeTranslation?.translatedOptions || [];

    this.optionTranslations = sourceOpts.map(so => {
      const matched = translatedOpts.find(to => to.id === so.id);
      return {
        id: so.id,
        sourceText: so.text,
        text: matched ? matched.text : '',
        isCorrect: so.isCorrect
      };
    });

    if (this.activeTranslation) {
      this.form.patchValue({
        translatedContent: this.activeTranslation.translatedContent || '',
        translatedExplanation: this.activeTranslation.translatedExplanation || ''
      });
    } else {
      this.form.patchValue({
        translatedContent: '',
        translatedExplanation: ''
      });
    }
    this.cdr.markForCheck();
  }

  /**
   * Automatically translate the source question (stem, options, and explanation)
   * into the currently selected language using the local IndicTrans2 AI model.
   */
  autoTranslateWithIndicTrans2(): void {
    if (!this.question?.id) return;
    this.autoTranslating = true;
    this.errorMessage = '';

    const targetLangName = this.getSelectedLangObj()?.name || this.selectedLanguageCode;

    this.translationService.autoTranslateQuestion(this.question.id, this.selectedLanguageCode).subscribe({
      next: (res) => {
        this.autoTranslating = false;
        if (res.translatedContent) {
          this.form.patchValue({
            translatedContent: res.translatedContent,
            translatedExplanation: res.translatedExplanation || ''
          });
        }
        if (res.translatedOptions && res.translatedOptions.length > 0) {
          this.optionTranslations.forEach(opt => {
            const found = res.translatedOptions?.find(to => to.id === opt.id);
            if (found) {
              opt.text = found.text;
            }
          });
        }
        this.form.markAsDirty();
        this.snackBar.open(`Auto-translated to ${targetLangName} using IndicTrans2!`, 'Close', { duration: 4000 });
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.autoTranslating = false;
        this.errorMessage = err?.error?.message || err?.message || 'Failed to auto-translate with IndicTrans2';
        this.snackBar.open(`IndicTrans2 translation failed: ${this.errorMessage}`, 'Close', { duration: 5000 });
        this.cdr.markForCheck();
      }
    });
  }

  getTranslationStatusForLang(code: string): string | null {
    const tr = this.translationsMap.get(code);
    return tr ? tr.status : null;
  }

  getSelectedLangObj(): SupportedLanguage | undefined {
    return this.translationService.getLanguage(this.selectedLanguageCode);
  }

  canReview(): boolean {
    const roles = this.authService.getUserRoles();
    return roles.some(r => ['REVIEWER', 'APPROVER', 'EXAM_CONTROLLER', 'SUPER_ADMIN'].includes(r));
  }

  canTranslate(): boolean {
    const roles = this.authService.getUserRoles();
    return roles.some(r => ['TRANSLATOR', 'QUESTION_AUTHOR', 'EXAM_CONTROLLER', 'SUPER_ADMIN'].includes(r));
  }

  isDraft(): boolean {
    return !this.activeTranslation || this.activeTranslation.status === 'DRAFT';
  }

  isApproved(): boolean {
    return this.activeTranslation?.status === 'APPROVED';
  }

  isStale(): boolean {
    return this.activeTranslation?.status === 'STALE';
  }

  hasRejectionComment(): boolean {
    return !!(this.activeTranslation?.status === 'DRAFT' && this.activeTranslation?.reviewComments);
  }

  saveTranslation(): void {
    if (!this.question?.id) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.errorMessage = 'Please provide the translated question content.';
      return;
    }

    if (this.optionTranslations.length > 0) {
      const emptyOpt = this.optionTranslations.find(o => !o.text || !o.text.trim());
      if (emptyOpt) {
        this.errorMessage = `Please provide translated text for Option ${emptyOpt.id}.`;
        return;
      }
    }

    const currentUserId = this.authService.getUserId() || '00000000-0000-0000-0000-000000000001';

    const req: TranslationRequest = {
      questionId: this.question.id,
      languageCode: this.selectedLanguageCode,
      translatorId: currentUserId,
      translatedContent: this.form.value.translatedContent.trim(),
      translatedExplanation: this.form.value.translatedExplanation ? this.form.value.translatedExplanation.trim() : undefined,
      translatedOptions: this.optionTranslations.length > 0
        ? this.optionTranslations.map(o => ({ id: o.id, text: o.text.trim() }))
        : undefined
    };

    this.saving = true;
    this.errorMessage = '';

    const apiCall = this.activeTranslation?.translationId
      ? this.translationService.resubmitTranslation(this.activeTranslation.translationId, req)
      : this.translationService.submitTranslation(req);

    apiCall.subscribe({
      next: () => {
        this.saving = false;
        const msg = this.activeTranslation
          ? 'Translation updated & resubmitted successfully'
          : 'Translation submitted successfully';
        this.snackBar.open(msg, 'Close', { duration: 3000 });
        this.loadTranslations();
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err?.error?.message || err?.message || 'Failed to submit translation';
        this.cdr.markForCheck();
      }
    });
  }

  approveTranslation(): void {
    if (!this.activeTranslation?.translationId) return;
    const reviewerId = this.authService.getUserId() || '00000000-0000-0000-0000-000000000001';

    this.approving = true;
    this.errorMessage = '';

    this.translationService.approveTranslation(this.activeTranslation.translationId, reviewerId).subscribe({
      next: () => {
        this.approving = false;
        this.snackBar.open('Translation approved successfully', 'Close', { duration: 3000 });
        this.loadTranslations();
      },
      error: (err) => {
        this.approving = false;
        this.errorMessage = err?.error?.message || 'Failed to approve translation';
        this.cdr.markForCheck();
      }
    });
  }

  openRejectPrompt(): void {
    this.showRejectInput = true;
    this.rejectComments = '';
  }

  cancelReject(): void {
    this.showRejectInput = false;
    this.rejectComments = '';
  }

  confirmReject(): void {
    if (!this.activeTranslation?.translationId) return;
    if (!this.rejectComments.trim()) {
      this.errorMessage = 'Please provide comments explaining why this translation was rejected.';
      return;
    }

    const reviewerId = this.authService.getUserId() || '00000000-0000-0000-0000-000000000001';

    this.rejecting = true;
    this.errorMessage = '';

    this.translationService.rejectTranslation(
      this.activeTranslation.translationId,
      reviewerId,
      this.rejectComments.trim()
    ).subscribe({
      next: () => {
        this.rejecting = false;
        this.showRejectInput = false;
        this.snackBar.open('Translation rejected and returned to translator with feedback.', 'Close', { duration: 3000 });
        this.loadTranslations();
      },
      error: (err) => {
        this.rejecting = false;
        this.errorMessage = err?.error?.message || 'Failed to reject translation';
        this.cdr.markForCheck();
      }
    });
  }

  onClose(): void {
    this.close.emit(false);
  }
}
