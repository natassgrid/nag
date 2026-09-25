import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  BlueprintTemplateResponse,
  BlueprintRule,
} from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-blueprint-grid-list',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './blueprint-grid-list.component.html',
  styleUrl: './blueprint-grid-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BlueprintGridListComponent {
  templates = input<BlueprintTemplateResponse[]>([]);
  loading = input<boolean>(false);
  deletingId = input<string | null>(null);

  createBlueprint = output<void>();
  editBlueprint = output<BlueprintTemplateResponse>();
  deleteBlueprint = output<BlueprintTemplateResponse>();
  auditSufficiency = output<BlueprintTemplateResponse>();

  calculateTotalQuestions(rules?: BlueprintRule[]): number {
    if (!rules || rules.length === 0) return 0;
    return rules.reduce((acc, r) => acc + (r.questionCount || r.targetCount || 0), 0);
  }
}
