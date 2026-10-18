import { Component, ChangeDetectionStrategy, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { MicroLessonReviewItem } from '../../../../models/micro-lesson.model';
import { MicroLessonTranslationPanelComponent } from '../micro-lesson-translation-panel/micro-lesson-translation-panel.component';

@Component({
  selector: 'app-micro-lesson-detail-drawer',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MathRendererComponent,
    MicroLessonTranslationPanelComponent,
  ],
  templateUrl: './micro-lesson-detail-drawer.component.html',
  styleUrl: './micro-lesson-detail-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonDetailDrawerComponent {
  readonly lesson = input<MicroLessonReviewItem | null>(null);
  readonly isEditingContent = input<boolean>(false);
  readonly isSubmitting = input<boolean>(false);
  readonly reviewerNotes = input<string>('');
  readonly editedCoreConcept = input<string>('');
  readonly editedTrapAlert = input<string>('');
  readonly editedFormulaCheatSheet = input<string>('');

  readonly activeTab = signal<'CONTENT' | 'TRANSLATION'>('CONTENT');


  readonly close = output<void>();
  readonly startEdit = output<MicroLessonReviewItem>();
  readonly openStudioEdit = output<MicroLessonReviewItem>();
  readonly cancelEdit = output<void>();
  readonly saveEdit = output<void>();
  readonly delete = output<MicroLessonReviewItem>();
  readonly submitReview = output<'APPROVE' | 'REJECT' | 'PUBLISH'>();
  readonly reviewerNotesChange = output<string>();
  readonly coreConceptChange = output<string>();
  readonly trapAlertChange = output<string>();
  readonly formulaCheatSheetChange = output<string>();

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'APPROVED':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'PUBLISHED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'REJECTED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'IN_REVIEW':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      default:
        return 'bg-purple-50 text-purple-700 border-purple-200';
    }
  }
}
