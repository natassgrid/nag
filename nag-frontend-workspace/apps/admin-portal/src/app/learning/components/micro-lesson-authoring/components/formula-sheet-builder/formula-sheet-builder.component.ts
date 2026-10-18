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
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { FormulaSymbolPaletteComponent } from '@nag-frontend-workspace/questions-feature-authoring';
import { FormulaEntry } from '../../../../models/micro-lesson.model';
import { FormulaApplicabilityMode } from '../../models/micro-lesson-authoring.model';

@Component({
  selector: 'app-formula-sheet-builder',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MathRendererComponent,
    FormulaSymbolPaletteComponent,
  ],
  templateUrl: './formula-sheet-builder.component.html',
  styleUrl: './formula-sheet-builder.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormulaSheetBuilderComponent {
  readonly formulas = input<FormulaEntry[]>([]);
  readonly required = input<boolean>(true);
  readonly applicabilityMode = input<FormulaApplicabilityMode>('MANDATORY');
  readonly applicabilityBadge = input<string>('Required (Math / Science / Numerical)');
  readonly applicabilityReason = input<string>('');
  readonly formulasChange = output<FormulaEntry[]>();

  readonly showPalette = signal<boolean>(false);
  readonly activeIndex = signal<number>(0);

  togglePalette(): void {
    this.showPalette.update((v) => !v);
  }

  addFormula(): void {
    const list = this.formulas() || [];
    const next: FormulaEntry = {
      name: `Formula ${list.length + 1}`,
      latex: '',
      description: '',
      variables: [],
    };
    this.formulasChange.emit([...list, next]);
    this.activeIndex.set(list.length);
  }

  removeFormula(index: number): void {
    const list = this.formulas() || [];
    if (this.required() && list.length <= 1) return;
    const updated = list.filter((_, idx) => idx !== index);
    this.formulasChange.emit(updated);
    if (this.activeIndex() >= updated.length) {
      this.activeIndex.set(Math.max(0, updated.length - 1));
    }
  }

  clearAllFormulas(): void {
    if (this.required()) return;
    this.formulasChange.emit([]);
    this.activeIndex.set(0);
    this.showPalette.set(false);
  }

  updateFormula(index: number, field: keyof FormulaEntry, value: string): void {
    const list = this.formulas() || [];
    const updated = list.map((item, idx) =>
      idx === index ? { ...item, [field]: value } : item
    );
    this.formulasChange.emit(updated);
  }

  insertSymbol(symbol: string): void {
    const list = this.formulas() || [];
    const idx = Math.min(this.activeIndex(), Math.max(0, list.length - 1));
    if (!list[idx]) return;
    const cur = list[idx].latex || '';
    const nextLatex = cur ? `${cur} ${symbol}` : symbol;
    this.updateFormula(idx, 'latex', nextLatex);
  }

  formatPreviewLatex(latex: string): string {
    const trimmed = (latex || '').trim();
    if (!trimmed) return '';
    if (trimmed.includes('$$') || trimmed.includes('$')) return trimmed;
    return `$$${trimmed}$$`;
  }
}
