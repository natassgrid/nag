import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  PaperDetail,
  PaperTranslateResponse,
} from '@nag-frontend-workspace/examinations-data-access';
import { LanguageOption } from '@nag-frontend-workspace/shared-util-i18n';
import { PaperTranslationSubpanelComponent } from '../paper-translation-subpanel/paper-translation-subpanel.component';

@Component({
  selector: 'nag-paper-inspection-drawer',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    PaperTranslationSubpanelComponent,
  ],
  templateUrl: './paper-inspection-drawer.component.html',
  styleUrl: './paper-inspection-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaperInspectionDrawerComponent {
  open = input<boolean>(false);
  loading = input<boolean>(false);
  paperDetail = input<PaperDetail | null>(null);
  paperId = input<string | null>(null);
  isTranslating = input<boolean>(false);
  activeTranslationJob = input<PaperTranslateResponse | null>(null);
  supportedLanguages = input<LanguageOption[]>([]);

  close = output<void>();
  startTranslation = output<{ targetLanguage: string; overwriteExisting: boolean }>();

  onClose(): void {
    this.close.emit();
  }

  onStartTranslation(targetLanguage: string): void {
    this.startTranslation.emit({
      targetLanguage,
      overwriteExisting: false,
    });
  }
}
