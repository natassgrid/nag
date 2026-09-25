import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  PaperDetail,
  PaperTranslateResponse,
} from '@nag-frontend-workspace/examinations-data-access';

export interface IndicLanguageOption {
  code: string;
  label: string;
  native: string;
}

@Component({
  selector: 'nag-paper-inspection-drawer',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './paper-inspection-drawer.component.html',
  styleUrl: './paper-inspection-drawer.component.scss',
})
export class PaperInspectionDrawerComponent {
  open = input<boolean>(false);
  loading = input<boolean>(false);
  paperDetail = input<PaperDetail | null>(null);
  paperId = input<string | null>(null);
  isTranslating = input<boolean>(false);
  activeTranslationJob = input<PaperTranslateResponse | null>(null);
  supportedLanguages = input<IndicLanguageOption[]>([]);

  close = output<void>();
  startTranslation = output<{ targetLanguage: string; overwriteExisting: boolean }>();

  targetLanguage = 'hi';
  overwriteExisting = false;

  onClose(): void {
    this.close.emit();
  }

  onStartTranslation(): void {
    this.startTranslation.emit({
      targetLanguage: this.targetLanguage,
      overwriteExisting: this.overwriteExisting,
    });
  }
}
