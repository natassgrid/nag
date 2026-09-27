import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EducationEntry } from '../../../../models';

@Component({
  selector: 'nag-education-item-card',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './education-item-card.component.html',
  styleUrl: './education-item-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EducationItemCardComponent {
  readonly entry = input.required<EducationEntry>();
  readonly edit = output<EducationEntry>();
  readonly delete = output<EducationEntry>();

  onEdit(): void {
    this.edit.emit(this.entry());
  }

  onDelete(): void {
    this.delete.emit(this.entry());
  }
}
