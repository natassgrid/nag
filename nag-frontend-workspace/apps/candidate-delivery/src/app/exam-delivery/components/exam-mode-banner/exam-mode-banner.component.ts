import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { ExamDeliveryMode } from '../../models';

@Component({
  selector: 'nag-exam-mode-banner',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule],
  templateUrl: './exam-mode-banner.component.html',
  styleUrl: './exam-mode-banner.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamModeBannerComponent {
  readonly mode = input.required<ExamDeliveryMode>();
  readonly exit = output<void>();

  onExit(): void {
    this.exit.emit();
  }
}
