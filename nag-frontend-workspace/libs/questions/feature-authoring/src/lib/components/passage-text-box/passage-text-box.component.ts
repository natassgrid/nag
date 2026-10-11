import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { FormulaSymbolPaletteComponent } from '../formula-symbol-palette/formula-symbol-palette.component';

@Component({
  selector: 'nag-passage-text-box',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, FormulaSymbolPaletteComponent],
  templateUrl: './passage-text-box.component.html',
  styleUrl: './passage-text-box.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PassageTextBoxComponent {
  readonly passageTitle = input<string>('');
  readonly passageContent = input<string>('');

  readonly passageTitleChange = output<string>();
  readonly passageContentChange = output<string>();

  readonly showFormulaPalette = signal<boolean>(false);

  readonly contentInvalid = computed(() => {
    return !this.passageContent() || !this.passageContent().trim();
  });

  toggleFormulaPalette(): void {
    this.showFormulaPalette.update((v) => !v);
  }

  insertSymbol(symbol: string): void {
    const cur = this.passageContent();
    const updated = cur ? `${cur} ${symbol}` : symbol;
    this.passageContentChange.emit(updated);
  }
}
