import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { FormulaSymbolPaletteComponent } from '@nag-frontend-workspace/questions-feature-authoring';
import { TrapAlert } from '../../../../models/micro-lesson.model';

@Component({
  selector: 'app-trap-alert-editor',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    FormulaSymbolPaletteComponent,
  ],
  templateUrl: './trap-alert-editor.component.html',
  styleUrl: './trap-alert-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrapAlertEditorComponent {
  readonly trapAlert = input<TrapAlert>({
    title: '',
    description: '',
    commonMistake: '',
    correctApproach: '',
  });
  readonly trapAlertChange = output<TrapAlert>();

  readonly showPalette = signal<boolean>(false);
  readonly activeField = signal<'description' | 'commonMistake' | 'correctApproach'>('description');

  togglePalette(): void {
    this.showPalette.update((v) => !v);
  }

  updateField(field: keyof TrapAlert, value: string): void {
    const current = this.trapAlert() || { title: '', description: '' };
    this.trapAlertChange.emit({
      ...current,
      [field]: value,
    });
  }

  insertSymbol(symbol: string): void {
    const target = this.activeField();
    const current = this.trapAlert() || { title: '', description: '' };
    const existing = (current[target] as string) || '';
    const updated = existing ? `${existing} ${symbol}` : symbol;
    this.updateField(target, updated);
  }
}
