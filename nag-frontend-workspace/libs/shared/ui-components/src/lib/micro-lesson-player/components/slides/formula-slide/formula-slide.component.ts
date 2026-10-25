import {
  Component,
  ChangeDetectionStrategy,
  computed,
  input,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MathRendererComponent } from '../../../../math-renderer.component';
import {
  PlayerSubtopicMicroLesson,
  PlayerFormulaEntry,
  formatLatex,
} from '../../../micro-lesson-player.model';

@Component({
  selector: 'nag-formula-slide, app-formula-slide',
  standalone: true,
  imports: [CommonModule, MatIconModule, MathRendererComponent],
  templateUrl: './formula-slide.component.html',
  styleUrl: './formula-slide.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormulaSlideComponent {
  readonly lesson = input<PlayerSubtopicMicroLesson | null>(null);
  readonly formulaRequired = input<boolean>(true);

  readonly activeFormulaIndex = signal<number>(0);
  readonly formatLatex = formatLatex;

  readonly formulas = computed<PlayerFormulaEntry[]>(() => {
    const sheet =
      this.lesson()?.content?.formulaCheatSheet ||
      (this.lesson() as any)?.formulaCheatSheet;

    if (Array.isArray(sheet)) {
      const valid = sheet
        .map((f: any, idx: number): PlayerFormulaEntry | null => {
          if (!f) return null;
          if (typeof f === 'string') {
            const trimmed = f.trim();
            const colonIdx = trimmed.indexOf(':');
            const dollarIdx = trimmed.indexOf('$');
            const slashIdx = trimmed.indexOf('\\');
            const hasColon = colonIdx > 0;
            const colonBeforeDollar = dollarIdx === -1 || colonIdx < dollarIdx;
            const colonBeforeSlash = slashIdx === -1 || colonIdx < slashIdx;
            let name = `Formula ${idx + 1}`;
            let latex = trimmed;
            if (hasColon && colonBeforeDollar && colonBeforeSlash) {
              const namePart = trimmed.substring(0, colonIdx).trim();
              const latexPart = trimmed.substring(colonIdx + 1).trim();
              if (namePart && !namePart.includes('$') && !namePart.includes('\\')) {
                name = namePart;
                latex = latexPart;
              }
            }
            return {
              name,
              latex,
              description: '',
              variables: [],
            };
          }
          return {
            name: f.name || f.title || f.namePart || `Formula ${idx + 1}`,
            latex: f.latex || f.formula || f.expression || f.value || '',
            description: f.description || f.note || f.usage || '',
            variables: f.variables || [],
          };
        })
        .filter(
          (f): f is PlayerFormulaEntry =>
            !!f && (f.latex.trim().length > 0 || f.name.trim().length > 0)
        );

      if (valid.length > 0) return valid;
    }
    if (typeof sheet === 'string' && sheet.trim()) {
      return [
        {
          name: 'Core Governing Formula',
          latex: sheet.trim(),
          description: 'Primary mathematical identity for this subtopic.',
        },
      ];
    }
    if (!this.formulaRequired()) {
      return [];
    }
    const takeaways = this.lesson()?.content?.keyTakeaways || [];
    return takeaways.slice(0, 2).map((t, i) => ({
      name: `Key Principle ${i + 1}`,
      latex: t,
      description: 'Core conceptual rule for exam application.',
    }));
  });
}
