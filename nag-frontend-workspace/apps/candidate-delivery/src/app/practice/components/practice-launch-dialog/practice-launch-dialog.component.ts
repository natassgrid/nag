import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  I18nService,
  SUPPORTED_LANGUAGES,
  SupportedLanguage,
} from '@nag-frontend-workspace/shared-util-i18n';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { PracticeService } from '../../services/practice.service';
import { PracticeSet, PracticeSessionMode } from '../../models';

@Component({
  selector: 'app-practice-launch-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatRadioModule,
    MatProgressSpinnerModule,
    MatIconModule,
    FormsModule
  ],
  templateUrl: './practice-launch-dialog.component.html',
  styleUrl: './practice-launch-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeLaunchDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<PracticeLaunchDialogComponent>);
  private readonly data = inject<{ set: PracticeSet }>(MAT_DIALOG_DATA);
  private readonly practiceService = inject(PracticeService);
  private readonly router = inject(Router);
  private readonly i18nService = inject(I18nService);
  private readonly authService = inject(AuthService, { optional: true });

  readonly set = this.data.set;
  readonly selectedMode = signal<PracticeSessionMode>('TIMED');
  readonly isStarting = signal(false);

  readonly supportedLanguages = SUPPORTED_LANGUAGES;
  readonly selectedLanguage = signal<string>(this.resolveInitialLanguage());

  private resolveInitialLanguage(): string {
    const user = this.authService?.currentUser();
    if (user?.preferredLanguage) {
      return user.preferredLanguage;
    }
    const current = this.i18nService?.currentLanguage();
    return current || 'en';
  }

  startSession(): void {
    if (this.isStarting()) return;
    this.isStarting.set(true);

    const lang = this.selectedLanguage();
    if (this.i18nService && typeof (this.i18nService as any).setLanguage === 'function') {
      this.i18nService.setLanguage(lang as SupportedLanguage);
    }

    this.practiceService.startSession({
      practiceSetId: this.set.id,
      mode: this.selectedMode(),
      preferredLanguage: lang,
    }).subscribe({
      next: (session) => {
        this.dialogRef.close();
        this.router.navigate(['/delivery'], {
          queryParams: {
            mode: 'PRACTICE',
            paperId: this.set.id,
            sessionId: session.id,
            lang: lang,
          }
        });
      },
      error: () => {
        this.isStarting.set(false);
      }
    });
  }
}
