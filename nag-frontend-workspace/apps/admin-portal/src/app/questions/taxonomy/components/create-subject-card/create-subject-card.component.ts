import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { CreateSubjectDto } from '../../models';

@Component({
  selector: 'nag-create-subject-card',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule],
  templateUrl: './create-subject-card.component.html',
  styleUrl: './create-subject-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateSubjectCardComponent {
  isOpen = input<boolean>(false);
  creating = input<boolean>(false);

  closeCard = output<void>();
  saveSubject = output<CreateSubjectDto>();

  name = '';
  code = '';
  description = '';

  onClose(): void {
    this.name = '';
    this.code = '';
    this.description = '';
    this.closeCard.emit();
  }

  onSave(): void {
    if (!this.name.trim()) return;

    this.saveSubject.emit({
      name: this.name.trim(),
      code: this.code.trim() || undefined,
      description: this.description.trim() || undefined,
    });
    this.name = '';
    this.code = '';
    this.description = '';
  }
}
