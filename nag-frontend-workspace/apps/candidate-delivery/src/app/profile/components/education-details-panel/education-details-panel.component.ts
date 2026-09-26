import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EducationEntry } from '../../models';

@Component({
  selector: 'nag-education-details-panel',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './education-details-panel.component.html',
  styleUrl: './education-details-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EducationDetailsPanelComponent {
  readonly education = input.required<EducationEntry[]>();
  readonly add = output<void>();
  readonly remove = output<number>();

  onAdd(): void {
    this.add.emit();
  }

  onRemove(index: number): void {
    this.remove.emit(index);
  }
}
