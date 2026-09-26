import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { PaperTranslateResponse } from '@nag-frontend-workspace/examinations-data-access';
import { LanguageOption } from '@nag-frontend-workspace/shared-util-i18n';

@Component({
  selector: 'nag-paper-translation-subpanel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
  ],
  templateUrl: './paper-translation-subpanel.component.html',
  styleUrl: './paper-translation-subpanel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaperTranslationSubpanelComponent {
  readonly supportedLanguages = input<LanguageOption[]>([]);
  readonly isTranslating = input<boolean>(false);
  readonly activeTranslationJob = input<PaperTranslateResponse | null>(null);

  readonly startTranslation = output<string>();

  targetLanguage = 'hi';

  onStart(): void {
    if (!this.targetLanguage) return;
    this.startTranslation.emit(this.targetLanguage);
  }
}
