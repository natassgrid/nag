import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-bank-semantic-search-drawer',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './bank-semantic-search-drawer.component.html',
  styleUrl: './bank-semantic-search-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BankSemanticSearchDrawerComponent {
  readonly open = input<boolean>(false);
  readonly searching = input<boolean>(false);

  readonly queryPrompt = signal<string>('');

  readonly close = output<void>();
  readonly search = output<string>();

  onExecuteSearch(): void {
    const prompt = this.queryPrompt().trim();
    if (!prompt) return;
    this.search.emit(prompt);
  }

  onClose(): void {
    this.close.emit();
  }
}
