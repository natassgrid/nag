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
import { MicroLessonStep } from '../../../../models/micro-lesson.model';

@Component({
  selector: 'app-step-breakdown-builder',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MathRendererComponent,
    FormulaSymbolPaletteComponent,
  ],
  templateUrl: './step-breakdown-builder.component.html',
  styleUrl: './step-breakdown-builder.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StepBreakdownBuilderComponent {
  readonly problemStatement = input<string>('');
  readonly steps = input<MicroLessonStep[]>([]);
  readonly finalAnswer = input<string>('');

  readonly problemStatementChange = output<string>();
  readonly stepsChange = output<MicroLessonStep[]>();
  readonly finalAnswerChange = output<string>();

  readonly showPalette = signal<boolean>(false);
  readonly activeTarget = signal<'problem' | 'finalAnswer' | 'stepExplanation' | 'stepFormula'>('problem');
  readonly activeStepIndex = signal<number>(0);

  togglePalette(): void {
    this.showPalette.update((v) => !v);
  }

  addStep(): void {
    const current = this.steps() || [];
    const nextNum = current.length + 1;
    const nextStep: MicroLessonStep = {
      stepNumber: nextNum,
      title: `Step ${nextNum}`,
      explanation: '',
      formula: '',
      tip: '',
    };
    this.stepsChange.emit([...current, nextStep]);
    this.activeStepIndex.set(current.length);
  }

  removeStep(index: number): void {
    const current = this.steps() || [];
    if (current.length <= 1) return;
    const filtered = current
      .filter((_, idx) => idx !== index)
      .map((s, idx) => ({ ...s, stepNumber: idx + 1 }));
    this.stepsChange.emit(filtered);
  }

  moveStep(index: number, direction: -1 | 1): void {
    const current = [...(this.steps() || [])];
    const targetIdx = index + direction;
    if (targetIdx < 0 || targetIdx >= current.length) return;
    const temp = current[index];
    current[index] = current[targetIdx];
    current[targetIdx] = temp;
    const reindexed = current.map((s, idx) => ({ ...s, stepNumber: idx + 1 }));
    this.stepsChange.emit(reindexed);
  }

  updateStep(index: number, field: keyof MicroLessonStep, value: string): void {
    const current = this.steps() || [];
    const updated = current.map((step, idx) =>
      idx === index ? { ...step, [field]: value } : step
    );
    this.stepsChange.emit(updated);
  }

  insertSymbol(symbol: string): void {
    const target = this.activeTarget();
    if (target === 'problem') {
      const cur = this.problemStatement() || '';
      this.problemStatementChange.emit(cur ? `${cur} ${symbol}` : symbol);
      return;
    }
    if (target === 'finalAnswer') {
      const cur = this.finalAnswer() || '';
      this.finalAnswerChange.emit(cur ? `${cur} ${symbol}` : symbol);
      return;
    }
    const list = this.steps() || [];
    const idx = Math.min(this.activeStepIndex(), Math.max(0, list.length - 1));
    if (!list[idx]) return;
    if (target === 'stepFormula') {
      const cur = list[idx].formula || '';
      this.updateStep(idx, 'formula', cur ? `${cur} ${symbol}` : symbol);
    } else {
      const cur = list[idx].explanation || '';
      this.updateStep(idx, 'explanation', cur ? `${cur} ${symbol}` : symbol);
    }
  }
}
