import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { GeneratedQuestion } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-ai-generated-card',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './ai-generated-card.component.html',
  styleUrl: './ai-generated-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiGeneratedCardComponent {
  readonly question = input.required<GeneratedQuestion>();
  readonly index = input.required<number>();
  readonly isSaved = input<boolean>(false);
  readonly isSaving = input<boolean>(false);

  readonly save = output<{ question: GeneratedQuestion; index: number }>();
  readonly openAuthoring = output<GeneratedQuestion>();

  onSave(): void {
    this.save.emit({
      question: this.question(),
      index: this.index(),
    });
  }

  onOpen(): void {
    this.openAuthoring.emit(this.question());
  }
}
