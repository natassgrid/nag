import {
  Component,
  ChangeDetectionStrategy,
  inject,
  input,
  output,
  signal,
  computed,
  effect,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { MicroLessonAdminService } from '../../../../services/micro-lesson-admin.service';
import {
  MicroLessonReviewItem,
  MicroLessonTranslation,
  SUPPORTED_MICROLESSON_LANGUAGES,
  MicroLessonLanguage,
} from '../../../../models/micro-lesson.model';

@Component({
  selector: 'app-micro-lesson-translation-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    MathRendererComponent,
  ],
  templateUrl: './micro-lesson-translation-panel.component.html',
  styleUrls: ['./micro-lesson-translation-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonTranslationPanelComponent {
  private readonly microLessonService = inject(MicroLessonAdminService);

  readonly lesson = input<MicroLessonReviewItem | null>(null);
  readonly translationUpdated = output<MicroLessonTranslation>();

  readonly availableLanguages: MicroLessonLanguage[] = SUPPORTED_MICROLESSON_LANGUAGES.filter(
    (l) => l.code !== 'en'
  );

  readonly selectedLanguage = signal<string>('hi');
  readonly translations = signal<MicroLessonTranslation[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isTranslating = signal<boolean>(false);
  readonly isModerating = signal<boolean>(false);
  readonly showBilingualComparison = signal<boolean>(true);
  readonly reviewComments = signal<string>('');
  readonly statusMessage = signal<string | null>(null);

  readonly activeLanguageObj = computed(() => {
    const code = this.selectedLanguage();
    return this.availableLanguages.find((l) => l.code === code) || this.availableLanguages[0];
  });

  readonly activeTranslation = computed<MicroLessonTranslation | null>(() => {
    const code = this.selectedLanguage();
    return this.translations().find((t) => t.languageCode === code) || null;
  });

  constructor() {
    effect(() => {
      const item = this.lesson();
      if (item?.id) {
        this.loadTranslations(item.id);
      } else {
        this.translations.set([]);
      }
    });
  }

  loadTranslations(lessonId: string): void {
    this.isLoading.set(true);
    this.statusMessage.set(null);
    this.microLessonService.getTranslations(lessonId).subscribe({
      next: (list) => {
        this.translations.set(list || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.translations.set([]);
        this.isLoading.set(false);
      },
    });
  }

  selectLanguage(code: string): void {
    this.selectedLanguage.set(code);
    this.statusMessage.set(null);
    const existing = this.activeTranslation();
    if (existing?.reviewComments) {
      this.reviewComments.set(existing.reviewComments);
    } else {
      this.reviewComments.set('');
    }
  }

  triggerAutoTranslate(): void {
    const item = this.lesson();
    if (!item?.id) return;

    this.isTranslating.set(true);
    this.statusMessage.set(null);
    const lang = this.selectedLanguage();

    this.microLessonService.autoTranslateMicroLesson(item.id, lang).subscribe({
      next: (translated) => {
        this.isTranslating.set(false);
        this.translations.update((list) => {
          const filtered = list.filter((t) => t.languageCode !== lang);
          return [...filtered, translated];
        });
        this.translationUpdated.emit(translated);
        this.statusMessage.set(`Translation generated in ${this.activeLanguageObj().name}`);
      },
      error: (err) => {
        this.isTranslating.set(false);
        this.statusMessage.set(err?.error?.message || 'Failed to generate translation.');
      },
    });
  }

  approveCurrentTranslation(): void {
    const trans = this.activeTranslation();
    if (!trans?.id) return;

    this.isModerating.set(true);
    this.microLessonService
      .approveTranslation(trans.id, this.reviewComments() || undefined)
      .subscribe({
        next: (updated) => {
          this.isModerating.set(false);
          this.updateTranslationInList(updated);
          this.translationUpdated.emit(updated);
          this.statusMessage.set('Translation approved successfully.');
        },
        error: (err) => {
          this.isModerating.set(false);
          this.statusMessage.set(err?.error?.message || 'Failed to approve translation.');
        },
      });
  }

  rejectCurrentTranslation(): void {
    const trans = this.activeTranslation();
    if (!trans?.id) return;

    const comments = this.reviewComments() || 'Rejected due to terminology inaccuracy.';
    this.isModerating.set(true);
    this.microLessonService.rejectTranslation(trans.id, comments).subscribe({
      next: (updated) => {
        this.isModerating.set(false);
        this.updateTranslationInList(updated);
        this.translationUpdated.emit(updated);
        this.statusMessage.set('Translation rejected.');
      },
      error: (err) => {
        this.isModerating.set(false);
        this.statusMessage.set(err?.error?.message || 'Failed to reject translation.');
      },
    });
  }

  getTrapContent(item: MicroLessonReviewItem): string {
    const trap = item.content?.trapAlert || item.trap;
    if (typeof trap === 'string') return trap || '';
    if (trap && typeof trap === 'object') return trap.description || '';
    return '';
  }

  getFormulaContent(item: MicroLessonReviewItem): string {
    const formulas = item.content?.formulaCheatSheet;
    if (typeof formulas === 'string') return formulas || '';
    if (Array.isArray(formulas)) {
      return formulas
        .map((f: any) =>
          typeof f === 'string'
            ? f
            : `${f.name ? f.name + ': ' : ''}${f.latex || f.expression || ''}`.trim()
        )
        .join('\n\n');
    }
    if (Array.isArray(item.cheatSheet)) {
      return item.cheatSheet.join('\n\n');
    }
    return '';
  }

  private updateTranslationInList(updated: MicroLessonTranslation): void {
    this.translations.update((list) =>
      list.map((t) => (t.id === updated.id ? updated : t))
    );
  }

  getStatusBadgeClass(status?: string): string {
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
