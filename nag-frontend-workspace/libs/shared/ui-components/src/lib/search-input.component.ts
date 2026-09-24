import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  effect,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-search-input',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './search-input.component.html',
  styleUrl: './search-input.component.scss',
})
export class SearchInputComponent {
  placeholder = input<string>('Search...');
  value = input<string>('');
  searchChange = output<string>();

  internalValue = signal<string>('');
  private debounceTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    effect(
      () => {
        this.internalValue.set(this.value() || '');
      },
      { allowSignalWrites: true }
    );
  }

  onInputChange(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.internalValue.set(val);

    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
    this.debounceTimer = setTimeout(() => {
      this.searchChange.emit(val);
    }, 300);
  }

  clear(): void {
    this.internalValue.set('');
    this.searchChange.emit('');
  }
}
