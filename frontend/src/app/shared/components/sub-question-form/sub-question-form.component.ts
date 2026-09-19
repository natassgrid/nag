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
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ChangeDetectionStrategy,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormsModule,
  FormGroup,
  FormArray,
  FormBuilder
} from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import { ExamEditorComponent } from '../exam-editor/exam-editor.component';
import { MathRendererComponent } from '../math-renderer/math-renderer.component';
import { AssetPickerDialogComponent } from '../../../features/assets/asset-picker-dialog.component';
import { ImagePasteDialogComponent } from '../../../features/questions/image-paste-dialog.component';
import { AssetResponse } from '../../../features/assets/asset.model';

/**
 * Shared sub-question form component used in:
 *  - PassageFormDialogComponent (showMetadataRow=true): renders sub-question prompt,
 *    Difficulty -> Cognitive Level -> Question Type dropdown row, options with image/asset support,
 *    add/delete options, KaTeX live preview, and explanation editor.
 *  - QuestionFormDialogComponent (showMetadataRow=false): metadata row is managed by parent's
 *    top-level metadata grid, while content prompt, options builder, answer key, and explanation
 *    are rendered here.
 */
@Component({
  selector: 'app-sub-question-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatTooltipModule,
    MatSlideToggleModule,
    MatDialogModule,
    ExamEditorComponent,
    MathRendererComponent
  ],
  templateUrl: './sub-question-form.component.html',
  styleUrls: ['./sub-question-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubQuestionFormComponent implements OnChanges, OnDestroy {
  /**
   * The question / sub-question FormGroup.
   * Required controls: content, explanation, options (FormArray).
   * Option item controls: { text, isCorrect, imageUrl, imageAltText, isImageOnly } reservoirs.
   * When showMetadataRow=true, also requires: difficulty, cognitiveLevel, questionType.
   */
  @Input() formGroup!: FormGroup;

  /**
   * When defined (0-based), a "Question N" stem label is shown at the top of the
   * content editor (passage mode).
   */
  @Input() index?: number;

  /**
   * Whether to display the Difficulty -> Cognitive Level -> Question Type metadata dropdown row.
   * True for passage sub-questions; False for standalone question dialog (since it's in the top grid).
   */
  @Input() showMetadataRow = false;

  /** Whether rich mode is enabled for the editor toolbar. */
  @Input() richMode = false;

  /** Question type options passed from parent. */
  @Input() questionTypes: { label: string; value: string }[] = [];

  /** Difficulty options passed from parent. */
  @Input() difficultyOptions: string[] = [];

  /** Cognitive level options passed from parent. */
  @Input() cognitiveLevels: string[] = [];

  /**
   * Option validation error string set by the parent on save attempt.
   * Displayed beneath the options list.
   */
  @Input() optionError = '';

  /* ── Preview toggle state ── */
  showContentPreview = false;
  showExplanationPreview = false;
  showOptionPreviews = false;

  readonly optionIds = ['A', 'B', 'C', 'D', 'E', 'F'];

  private qtSub?: Subscription;

  constructor(
    private fb: FormBuilder,
    private dialog: MatDialog,
    private sanitizer: DomSanitizer,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['formGroup'] && this.formGroup) {
      this.qtSub?.unsubscribe();
      this.qtSub = this.formGroup.get('questionType')?.valueChanges
        .subscribe(val => this.handleQuestionTypeChange(val));
    }
  }

  ngOnDestroy(): void {
    this.qtSub?.unsubscribe();
  }

  /* ── Getters ── */

  get optionsArray(): FormArray {
    return this.formGroup.get('options') as FormArray;
  }

  get currentQuestionType(): string {
    return this.formGroup.get('questionType')?.value || '';
  }

  get contentValue(): string {
    return this.formGroup.get('content')?.value || '';
  }

  get explanationValue(): string {
    return this.formGroup.get('explanation')?.value || '';
  }

  isMcqOrMsq(): boolean {
    const qt = this.currentQuestionType;
    return qt === 'SINGLE_MCQ' || qt === 'MULTI_MCQ';
  }

  isMcq(): boolean {
    return this.currentQuestionType === 'SINGLE_MCQ';
  }

  /* ── Question type change handling ── */

  private handleQuestionTypeChange(value: string): void {
    const isMcq = value === 'SINGLE_MCQ' || value === 'MULTI_MCQ';
    if (!isMcq) {
      this.optionsArray.clear();
    } else if (this.optionsArray.length === 0) {
      this.optionsArray.push(this.createOptionGroup());
      this.optionsArray.push(this.createOptionGroup());
    }
    this.cdr.markForCheck();
  }

  /* ── FormGroup factory helpers ── */

  private createOptionGroup(text = '', isCorrect = false, imageUrl = '', imageAltText = '', isImageOnly = false): FormGroup {
    return this.fb.group({
      text: [text],
      isCorrect: [isCorrect],
      imageUrl: [imageUrl],
      imageAltText: [imageAltText],
      isImageOnly: [isImageOnly]
    });
  }

  /* ── Correct-answer selection ── */

  /** MCQ single-select deselects all other options when one is checked. */
  onMcqCorrectChange(checkedIndex: number): void {
    if (this.isMcq()) {
      this.optionsArray.controls.forEach((c, i) => {
        if (i !== checkedIndex) {
          c.get('isCorrect')?.setValue(false, { emitEvent: false });
        }
      });
      this.cdr.markForCheck();
    }
  }

  /* ── Option management ── */

  addOption(): void {
    if (this.optionsArray.length < 5) {
      this.optionsArray.push(this.createOptionGroup());
      this.cdr.markForCheck();
    }
  }

  removeOption(optIdx: number): void {
    if (this.optionsArray.length > 2) {
      this.optionsArray.removeAt(optIdx);
      this.cdr.markForCheck();
    }
  }

  /** Side-effect handler for isImageOnly checkbox change (formControlName handles the value). */
  toggleImageOnly(optIdx: number, isImageOnly: boolean): void {
    const optGroup = this.optionsArray.at(optIdx);
    if (isImageOnly) {
      optGroup.get('text')?.setValue('');
      if (!optGroup.get('imageUrl')?.value) {
        this.openOptionDataUriDialog(optIdx);
      }
    }
    this.cdr.markForCheck();
  }

  clearOptionImage(optIdx: number): void {
    const optGroup = this.optionsArray.at(optIdx);
    optGroup.get('imageUrl')?.setValue('');
    optGroup.get('imageAltText')?.setValue('');
    optGroup.get('isImageOnly')?.setValue(false);
    this.cdr.markForCheck();
  }

  getSafeImageUrl(url?: string | null): SafeUrl | string {
    if (!url) return '';
    if (url.startsWith('data:') || url.startsWith('blob:')) {
      return this.sanitizer.bypassSecurityTrustUrl(url);
    }
    return url;
  }

  /* ── Content asset / Data-URI ── */

  openContentAssetPicker(): void {
    const ref = this.dialog.open(AssetPickerDialogComponent, {
      width: '800px',
      data: { assetType: 'IMAGE', title: 'Insert Image into Content' }
    });
    ref.afterClosed().subscribe((asset: AssetResponse) => {
      if (asset) {
        const alt = asset.altText || asset.title || asset.originalFilename || 'Diagram';
        const url = `/api/v1/assets/${asset.id}/download`;
        this.appendToField('content', `\n![${alt}](${url})\n`);
      }
    });
  }

  openContentDataUriDialog(): void {
    const ref = this.dialog.open(ImagePasteDialogComponent, {
      width: '560px',
      data: { title: 'Insert Base64 / Data URI Image into Content', showAltText: true }
    });
    ref.afterClosed().subscribe((res: any) => {
      if (res) {
        this.appendToField('content', `\n![${res.altText || 'Diagram'}](${res.imageUrl})\n`);
      }
    });
  }

  /* ── Explanation asset / Data-URI ── */

  openExplanationAssetPicker(): void {
    const ref = this.dialog.open(AssetPickerDialogComponent, {
      width: '800px',
      data: { assetType: 'IMAGE', title: 'Insert Image into Explanation' }
    });
    ref.afterClosed().subscribe((asset: AssetResponse) => {
      if (asset) {
        const alt = asset.altText || asset.title || asset.originalFilename || 'Explanation Diagram';
        const url = `/api/v1/assets/${asset.id}/download`;
        this.appendToField('explanation', `\n![${alt}](${url})\n`);
      }
    });
  }

  openExplanationDataUriDialog(): void {
    const ref = this.dialog.open(ImagePasteDialogComponent, {
      width: '560px',
      data: { title: 'Insert Base64 / Data URI Image into Explanation', showAltText: true }
    });
    ref.afterClosed().subscribe((res: any) => {
      if (res) {
        this.appendToField('explanation', `\n![${res.altText || 'Explanation Diagram'}](${res.imageUrl})\n`);
      }
    });
  }

  /* ── Option asset / Data-URI ── */

  openOptionAssetPicker(optIdx: number): void {
    const optGroup = this.optionsArray.at(optIdx);
    const optId = this.optionIds[optIdx];
    const ref = this.dialog.open(AssetPickerDialogComponent, {
      width: '800px',
      data: { assetType: 'IMAGE', title: `Select Image for Option ${optId}` }
    });
    ref.afterClosed().subscribe((asset: AssetResponse) => {
      if (asset) {
        optGroup.get('imageUrl')?.setValue(`/api/v1/assets/${asset.id}/download`);
        if (!optGroup.get('imageAltText')?.value) {
          optGroup.get('imageAltText')?.setValue(
            asset.altText || asset.title || `Option ${optId}`
          );
        }
        this.cdr.markForCheck();
      }
    });
  }

  openOptionDataUriDialog(optIdx: number): void {
    const optGroup = this.optionsArray.at(optIdx);
    const optId = this.optionIds[optIdx];
    const ref = this.dialog.open(ImagePasteDialogComponent, {
      width: '560px',
      data: {
        title: `Paste Base64 / Data URI for Option ${optId}`,
        currentUrl: optGroup.get('imageUrl')?.value,
        altText: optGroup.get('imageAltText')?.value || `Option ${optId}`
      }
    });
    ref.afterClosed().subscribe((res: any) => {
      if (res) {
        optGroup.get('imageUrl')?.setValue(res.imageUrl);
        optGroup.get('imageAltText')?.setValue(res.altText || `Option ${optId}`);
        this.cdr.markForCheck();
      }
    });
  }

  /* ── Helpers ── */

  private appendToField(field: 'content' | 'explanation', mdImage: string): void {
    const ctrl = this.formGroup.get(field);
    ctrl?.setValue((ctrl.value || '') + mdImage);
    ctrl?.markAsDirty();
    ctrl?.markAsTouched();
    this.cdr.markForCheck();
  }
}
