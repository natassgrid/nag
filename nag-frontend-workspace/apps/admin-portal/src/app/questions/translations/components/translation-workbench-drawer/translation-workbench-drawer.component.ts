import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Question } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-translation-workbench-drawer',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './translation-workbench-drawer.component.html',
  styleUrl: './translation-workbench-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TranslationWorkbenchDrawerComponent {
  isOpen = input<boolean>(false);
  activeQuestion = input<Question | null>(null);
  activeLanguageName = input<string>('');
  aiTranslating = input<boolean>(false);
  savingTranslation = input<boolean>(false);
  translatedContent = input<string>('');
  translatedOptions = input<{ id: string; text: string }[]>([]);
  translatedExplanation = input<string>('');
  translationStatus = input<string>('DRAFT');

  closeDrawer = output<void>();
  runAiTranslate = output<void>();
  saveDraft = output<void>();
  approveTranslation = output<void>();
  contentChange = output<string>();
  explanationChange = output<string>();
  optionChange = output<{ index: number; text: string }>();

  updateOption(index: number, text: string): void {
    this.optionChange.emit({ index, text });
  }
}
