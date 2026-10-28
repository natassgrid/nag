import {
  Component,
  ChangeDetectionStrategy,
  ElementRef,
  ViewChild,
  computed,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { FormulaSymbolPaletteComponent } from '@nag-frontend-workspace/questions-feature-authoring';

@Component({
  selector: 'app-core-concept-editor',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    FormulaSymbolPaletteComponent,
  ],
  templateUrl: './core-concept-editor.component.html',
  styleUrl: './core-concept-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CoreConceptEditorComponent {
  readonly coreConcept = input<string>('');
  readonly showWizardTrigger = input<boolean>(false);
  readonly coreConceptChange = output<string>();
  readonly openWizard = output<void>();

  @ViewChild('conceptTextarea') conceptTextarea?: ElementRef<HTMLTextAreaElement>;

  readonly showPalette = signal<boolean>(false);

  readonly isInvalid = computed(() => {
    const val = this.coreConcept() || '';
    return !val.trim();
  });

  togglePalette(): void {
    this.showPalette.update((v) => !v);
  }

  insertSymbol(symbol: string): void {
    const current = this.coreConcept() || '';
    const el = this.conceptTextarea?.nativeElement;
    if (el && typeof el.selectionStart === 'number') {
      const start = el.selectionStart;
      const end = el.selectionEnd ?? start;
      const before = current.slice(0, start);
      const after = current.slice(end);
      const next = `${before}${symbol}${after}`;
      this.coreConceptChange.emit(next);
      return;
    }
    const updated = current ? `${current} ${symbol}` : symbol;
    this.coreConceptChange.emit(updated);
  }
}
