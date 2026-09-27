import { ChangeDetectionStrategy, Component, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-education-empty-state',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './education-empty-state.component.html',
  styleUrl: './education-empty-state.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EducationEmptyStateComponent {
  readonly add = output<void>();

  onAdd(): void {
    this.add.emit();
  }
}
