import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { QuickCheckItem } from '../../../../models/micro-lesson.model';

@Component({
  selector: 'app-quick-check-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './quick-check-builder.component.html',
  styleUrl: './quick-check-builder.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuickCheckBuilderComponent {
  readonly keyTakeaways = input<string[]>([]);
  readonly quickCheckItems = input<QuickCheckItem[]>([]);
  readonly reviewerNotes = input<string>('');

  readonly keyTakeawaysChange = output<string[]>();
  readonly quickCheckItemsChange = output<QuickCheckItem[]>();
  readonly reviewerNotesChange = output<string>();

  addTakeaway(): void {
    const list = this.keyTakeaways() || [];
    this.keyTakeawaysChange.emit([...list, '']);
  }

  removeTakeaway(index: number): void {
    const list = this.keyTakeaways() || [];
    if (list.length <= 1) return;
    this.keyTakeawaysChange.emit(list.filter((_, idx) => idx !== index));
  }

  updateTakeaway(index: number, value: string): void {
    const list = this.keyTakeaways() || [];
    this.keyTakeawaysChange.emit(
      list.map((item, idx) => (idx === index ? value : item))
    );
  }

  addQuickCheck(): void {
    const list = this.quickCheckItems() || [];
    const next: QuickCheckItem = {
      question: '',
      options: [],
      answer: '',
      correctAnswer: '',
      explanation: '',
    };
    this.quickCheckItemsChange.emit([...list, next]);
  }

  removeQuickCheck(index: number): void {
    const list = this.quickCheckItems() || [];
    if (list.length <= 1) return;
    this.quickCheckItemsChange.emit(list.filter((_, idx) => idx !== index));
  }

  updateQuickCheck(
    index: number,
    field: keyof QuickCheckItem,
    value: string
  ): void {
    const list = this.quickCheckItems() || [];
    const updated = list.map((item, idx) => {
      if (idx !== index) return item;
      if (field === 'answer' || field === 'correctAnswer') {
        return { ...item, answer: value, correctAnswer: value };
      }
      return { ...item, [field]: value };
    });
    this.quickCheckItemsChange.emit(updated);
  }
}
