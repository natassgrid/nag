import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-journey-stepper-banner',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatButtonModule],
  templateUrl: './journey-stepper-banner.component.html',
  styleUrl: './journey-stepper-banner.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JourneyStepperBannerComponent {
  readonly activeJourneyStep = input<number>(1);
  readonly gatekeeperPassed = input<boolean>(false);
  readonly isColdStart = input<boolean>(false);
  readonly examTitle = input<string | null>(null);
  readonly readinessBaseline = input<number | null>(null);
  readonly baselineLabel = input<string>('Baseline Diagnostic');

  readonly startDiagnostic = output<void>();
  readonly scrollToPlan = output<void>();

  onStartDiagnostic(): void {
    this.startDiagnostic.emit();
  }

  onScrollToPlan(): void {
    this.scrollToPlan.emit();
  }
}
