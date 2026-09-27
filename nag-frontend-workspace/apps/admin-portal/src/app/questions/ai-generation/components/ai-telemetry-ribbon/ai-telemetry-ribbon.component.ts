import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { QuestionGenerationResponse } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-ai-telemetry-ribbon',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './ai-telemetry-ribbon.component.html',
  styleUrl: './ai-telemetry-ribbon.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiTelemetryRibbonComponent {
  readonly response = input.required<QuestionGenerationResponse>();
  readonly saveAllValid = output<void>();
}
