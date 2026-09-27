import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { LanguageOption, SupportedLanguage } from '@nag-frontend-workspace/shared-util-i18n';

@Component({
  selector: 'nag-exam-runtime-header',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './exam-runtime-header.component.html',
  styleUrl: './exam-runtime-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamRuntimeHeaderComponent {
  readonly remainingSeconds = input.required<number>();
  readonly formattedTime = input.required<string>();
  readonly currentLanguage = input.required<SupportedLanguage>();
  readonly supportedLanguages = input.required<LanguageOption[]>();

  readonly languageChange = output<SupportedLanguage>();
  readonly submit = output<void>();

  readonly isWarningTimer = computed(() => this.remainingSeconds() < 300);

  onLanguageSelect(langCode: SupportedLanguage): void {
    this.languageChange.emit(langCode);
  }

  onSubmit(): void {
    this.submit.emit();
  }
}
