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
import {
  IndicTranslationLanguage,
  BatchTranslationRequest,
} from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-batch-translation-modal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './batch-translation-modal.component.html',
  styleUrl: './batch-translation-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BatchTranslationModalComponent {
  isOpen = input<boolean>(false);
  languages = input<IndicTranslationLanguage[]>([]);
  availableSubjects = input<string[]>([]);
  submittingBatch = input<boolean>(false);

  closeModal = output<void>();
  triggerJob = output<BatchTranslationRequest>();

  batchSourceLanguage = 'en';
  batchTargetLang = 'hi';
  batchSubject = 'ALL';
  batchOverwrite = false;

  onSubmit(): void {
    this.triggerJob.emit({
      sourceLanguage: this.batchSourceLanguage,
      targetLanguage: this.batchTargetLang,
      subject: this.batchSubject === 'ALL' ? undefined : this.batchSubject,
      overwriteExisting: this.batchOverwrite,
    });
  }
}
