import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { IndicTranslationLanguage } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-translation-language-strip',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './translation-language-strip.component.html',
  styleUrl: './translation-language-strip.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TranslationLanguageStripComponent {
  languages = input<IndicTranslationLanguage[]>([]);
  selectedLanguage = input<string>('hi');
  activeLanguageName = input<string>('');

  selectLanguage = output<string>();
}
