import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BlueprintTemplateResponse } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-paper-templates-panel',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './paper-templates-panel.component.html',
  styleUrl: './paper-templates-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaperTemplatesPanelComponent {
  readonly templates = input<BlueprintTemplateResponse[]>([]);
  readonly loading = input<boolean>(false);

  readonly selectTemplate = output<string>();
}
