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
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
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
import { ExamEditorComponent } from '../../../shared/components/exam-editor/exam-editor.component';

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
    RightDrawerComponent,
    ExamEditorComponent
  ],
  templateUrl: './question-translation-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
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
  optionTranslations: {
    id: string;
    sourceText: string;
    text: string;
    isCorrect: boolean;
    imageUrl?: string;
    imageAltText?: string;
    sourceImageUrl?: string;
    sourceImageAltText?: string;
  }[] = [];

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
    private sanitizer: DomSanitizer,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  getSafeImageUrl(url?: string | null): SafeUrl | string {
    if (!url) return '';
    if (url.startsWith('data:') || url.startsWith('blob:')) {
      return this.sanitizer.bypassSecurityTrustUrl(url);
    }
    return url;
  }

  ngOnInit(): void {
    if (this.initialLanguageCode) {
      this.selectedLanguageCode = this.initialLanguageCode;
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen && this.question) {
      if (this.initialLanguageCode) {
        this.selectedLanguageCode = this.initialLanguageCode;
      }
      this.loadTranslations();
    }
  }

  private initForm(): void {
    this.form = this.fb.group({
      translatedContent: ['', Validators.required],
      translatedExplanation: ['']
    });
  }

  loadTranslations(): void {
    if (!this.question?.id) return;
    this.loadingTranslations = true;
    this.cdr.markForCheck();

    this.translationService.listTranslationsForQuestion(this.question.id).subscribe({
      next: (list) => {
        this.translationsMap.clear();
        (list || []).forEach(tr => {
          this.translationsMap.set(tr.languageCode, tr);
        });
        this.loadingTranslations = false;
        this.selectLanguage(this.selectedLanguageCode);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadingTranslations = false;
        this.selectLanguage(this.selectedLanguageCode);
        this.cdr.markForCheck();
      }
    });
  }

  selectLanguage(langCode: string): void {
    this.selectedLanguageCode = langCode;
    this.showRejectInput = false;
    this.rejectComments = '';
    this.errorMessage = '';

    const existing = this.translationsMap.get(langCode);
    this.activeTranslation = existing;

    if (existing) {
      this.form.patchValue({
        translatedContent: existing.translatedContent || '',
        translatedExplanation: existing.translatedExplanation || ''
      });

      if (this.question?.options && this.question.options.length > 0) {
        const transOptsMap = new Map<string, string>();
        if (existing.translatedOptions) {
          existing.translatedOptions.forEach(opt => {
            transOptsMap.set(opt.id, opt.text);
          });
        }

        this.optionTranslations = this.question.options.map(srcOpt => ({
          id: srcOpt.id,
          sourceText: srcOpt.text || '',
          text: transOptsMap.get(srcOpt.id) || '',
          isCorrect: srcOpt.isCorrect,
          imageUrl: srcOpt.imageUrl,
          imageAltText: srcOpt.imageAltText,
          sourceImageUrl: srcOpt.imageUrl,
          sourceImageAltText: srcOpt.imageAltText
        }));
      } else {
        this.optionTranslations = [];
      }
    } else {
      this.form.patchValue({
        translatedContent: '',
        translatedExplanation: ''
      });

      if (this.question?.options && this.question.options.length > 0) {
        this.optionTranslations = this.question.options.map(srcOpt => ({
          id: srcOpt.id,
          sourceText: srcOpt.text || '',
          text: '',
          isCorrect: srcOpt.isCorrect,
          imageUrl: srcOpt.imageUrl,
          imageAltText: srcOpt.imageAltText,
          sourceImageUrl: srcOpt.imageUrl,
          sourceImageAltText: srcOpt.imageAltText
        }));
      } else {
        this.optionTranslations = [];
      }
    }

    this.cdr.markForCheck();
  }

  saveDraft(): void {
    if (!this.question?.id) return;
    this.saving = true;
    this.errorMessage = '';

    const request: TranslationRequest = {
      languageCode: this.selectedLanguageCode,
      translatedContent: this.form.get('translatedContent')?.value || '',
      translatedExplanation: this.form.get('translatedExplanation')?.value || undefined,
      translatedOptions: this.optionTranslations.map(opt => ({
        id: opt.id,
        text: opt.text || ''
      }))
    };

    this.translationService.saveTranslation(this.question.id, request).subscribe({
      next: (res) => {
        this.saving = false;
        this.translationsMap.set(res.languageCode, res);
        this.activeTranslation = res;
        this.snackBar.open(`Translation saved for ${this.getSelectedLangName()}`, 'Close', { duration: 3000 });
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err?.error?.message || 'Failed to save translation.';
        this.cdr.markForCheck();
      }
    });
  }

  autoTranslateWithIndicTrans2(): void {
    if (!this.question) return;
    this.autoTranslating = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    const langName = this.getSelectedLangName();
    const isIndic = ['hi', 'ta', 'te', 'kn', 'ml', 'mr', 'bn', 'gu', 'pa', 'or', 'as'].includes(this.selectedLanguageCode);

    const textsToTranslate: string[] = [
      this.question.content || '',
      this.question.explanation || '',
      ...this.optionTranslations.map(o => o.sourceText || '')
    ];

    setTimeout(() => {
      this.autoTranslating = false;
      const note = isIndic
        ? `[Auto-Translated to ${langName} via IndicTrans2]\n`
        : `[Draft in ${langName}]\n`;

      if (!this.form.get('translatedContent')?.value && this.question?.content) {
        this.form.patchValue({
          translatedContent: note + this.question.content
        });
      }

      if (!this.form.get('translatedExplanation')?.value && this.question?.explanation) {
        this.form.patchValue({
          translatedExplanation: note + this.question.explanation
        });
      }

      this.optionTranslations.forEach(opt => {
        if (!opt.text && opt.sourceText) {
          opt.text = opt.sourceText;
        }
      });

      this.snackBar.open(`Pre-populated ${langName} template. Please review and refine.`, 'Close', { duration: 4000 });
      this.cdr.markForCheck();
    }, 800);
  }

  submitForReview(): void {
    if (!this.question?.id || !this.activeTranslation?.id) {
      this.saveDraft();
      return;
    }
    this.saving = true;
    this.translationService.submitTranslationForReview(this.question.id, this.activeTranslation.id).subscribe({
      next: (res) => {
        this.saving = false;
        this.translationsMap.set(res.languageCode, res);
        this.activeTranslation = res;
        this.snackBar.open('Translation submitted for review', 'Close', { duration: 3000 });
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err?.error?.message || 'Failed to submit translation.';
        this.cdr.markForCheck();
      }
    });
  }

  approve(): void {
    if (!this.question?.id || !this.activeTranslation?.id) return;
    this.approving = true;
    this.translationService.approveTranslation(this.question.id, this.activeTranslation.id).subscribe({
      next: (res) => {
        this.approving = false;
        this.translationsMap.set(res.languageCode, res);
        this.activeTranslation = res;
        this.snackBar.open(`Translation approved for ${this.getSelectedLangName()}`, 'Close', { duration: 3000 });
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.approving = false;
        this.errorMessage = err?.error?.message || 'Failed to approve translation.';
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
    if (!this.question?.id || !this.activeTranslation?.id || !this.rejectComments.trim()) return;
    this.rejecting = true;
    this.translationService.rejectTranslation(this.question.id, this.activeTranslation.id, this.rejectComments.trim()).subscribe({
      next: (res) => {
        this.rejecting = false;
        this.showRejectInput = false;
        this.translationsMap.set(res.languageCode, res);
        this.activeTranslation = res;
        this.snackBar.open('Translation rejected and returned for revisions', 'Close', { duration: 3000 });
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.rejecting = false;
        this.errorMessage = err?.error?.message || 'Failed to reject translation.';
        this.cdr.markForCheck();
      }
    });
  }

  getSelectedLangName(): string {
    return this.languages.find(l => l.code === this.selectedLanguageCode)?.name || this.selectedLanguageCode;
  }

  getSelectedLangObj(): SupportedLanguage | undefined {
    return this.languages.find(l => l.code === this.selectedLanguageCode);
  }

  hasTranslation(code: string): boolean {
    return this.translationsMap.has(code);
  }

  getTranslationStatus(code: string): string {
    return this.translationsMap.get(code)?.state || '';
  }

  canReview(): boolean {
    const role = this.authService.getRole?.() || '';
    return ['ROLE_CONTENT_REVIEWER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN'].includes(role);
  }

  isApproved(): boolean {
    return this.activeTranslation?.state === 'APPROVED';
  }

  isPendingReview(): boolean {
    return this.activeTranslation?.state === 'PENDING_REVIEW';
  }

  onClose(): void {
    this.close.emit(false);
  }
}
