import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { BlueprintRule } from '@nag-frontend-workspace/examinations-data-access';
import { Subject, SubjectHierarchy } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-blueprint-rule-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule],
  template: `
    <div class=\"space-y-4\">
      <div class=\"flex items-center justify-between\">
        <span class=\"text-xs font-bold text-slate-500 uppercase\">
          Distribution Constraints Matrix ({{ rules().length }} Rules)
        </span>
        <button
          type=\"button\"
          (click)=\"onAddRule()\"
          class=\"text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1\"
        >
          <mat-icon class=\"!text-sm\">add_circle</mat-icon>
          <span>Add Rule</span>
        </button>
      </div>

      <div class=\"space-y-3\">
        @for (rule of rules(); track $index) {
          <div class=\"p-4 bg-slate-50 rounded-xl border border-slate-200 grid grid-cols-1 md:grid-cols-6 gap-3 items-end\">
            <div class=\"md:col-span-2 space-y-1.5\">
              <div>
                <label class=\"block text-[10px] font-bold text-slate-600 uppercase mb-1\">Subject</label>
                @if (taxonomySubjects().length > 0) {
                  <select
                    [(ngModel)]=\"rule.subject\"\n                    (ngModelChange)=\"onSubjectChanged(rule)\"
                    class=\"w-full px-2.5 py-1.5 text-xs border rounded-lg bg-white\"
                  >
                    <option value=\"\">-- Select Subject --</option>
                    @for (subj of taxonomySubjects(); track subj.id || subj.name) {
                      <option [value]=\"subj.name\">{{ subj.name }}</option>
                    }
                  </select>
                } @else {
                  <input
                    type=\"text\"
                    [(ngModel)]=\"rule.subject\"
                    placeholder=\"Subject\"
                    class=\"w-full px-2.5 py-1.5 text-xs border rounded-lg bg-white\"
                  />
                }
              </div>

              <div>
                <label class=\"block text-[10px] font-bold text-slate-600 uppercase mb-1\">Topic / Domain</label>
                @if (getTopicsForSubject(rule.subject).length > 0) {
                  <select
                    [(ngModel)]=\"rule.topic\"
                    class=\"w-full px-2.5 py-1.5 text-xs border rounded-lg bg-white\"
                  >
                    <option value=\"\">-- All Topics in Subject --</option>
                    @for (top of getTopicsForSubject(rule.subject); track top) {
                      <option [value]=\"top\">{{ top }}</option>
                    }
                  </select>
                } @else {
                  <input
                    type=\"text\"
                    [(ngModel)]=\"rule.topic\"
                    placeholder=\"Topic / Chapter\"
                    class=\"w-full px-2.5 py-1.5 text-xs border rounded-lg bg-white\"
                  />
                }
              </div>
            </div>

            <div>
              <label class=\"block text-[10px] font-bold text-slate-600 uppercase mb-1\">Difficulty</label>
              <select
                [(ngModel)]=\"rule.difficulty\"
                class=\"w-full px-2 py-1.5 text-xs border rounded-lg bg-white\"
              >
                <option value=\"EASY\">Easy</option>
                <option value=\"MEDIUM\">Medium</option>
                <option value=\"HARD\">Hard</option>
              </select>
            </div>

            <div>
              <label class=\"block text-[10px] font-bold text-slate-600 uppercase mb-1\">Cognitive</label>
              <select
                [(ngModel)]=\"rule.cognitiveLevel\"
                class=\"w-full px-2 py-1.5 text-xs border rounded-lg bg-white\"
              >
                <option value=\"REMEMBER\">Remember</option>
                <option value=\"UNDERSTAND\">Understand</option>
                <option value=\"APPLY\">Apply</option>
                <option value=\"ANALYZE\">Analyze</option>
                <option value=\"EVALUATE\">Evaluate</option>
              </select>
            </div>

            <div>
              <label class=\"block text-[10px] font-bold text-slate-600 uppercase mb-1\">Draw Count</label>
              <input
                type=\"number\"
                [(ngModel)]=\"rule.targetCount\"
                min=\"1\"
                class=\"w-full px-2.5 py-1.5 text-xs border rounded-lg bg-white font-mono\"
              />
            </div>

            <div class=\"flex items-center justify-end\">
              <button
                type=\"button\"
                (click)=\"onRemoveRule($index)\"
                [disabled]=\"rules().length === 1\"
                class=\"p-1.5 text-rose-500 hover:text-rose-700 disabled:opacity-30\"
              >
                <mat-icon class=\"!text-lg\">delete</mat-icon>
              </button>
            </div>
          </div>
        }
      </div>
    </div>
  `,
})
export class BlueprintRuleBuilderComponent {
  rules = input.required<BlueprintRule[]>();
  taxonomySubjects = input<Subject[]>([]);
  taxonomyHierarchy = input<SubjectHierarchy[]>([]);

  addRule = output<void>();
  removeRule = output<number>();
  ruleSubjectChange = output<BlueprintRule>();

  getTopicsForSubject(subjectName: string): string[] {
    const node = this.taxonomyHierarchy().find(
      (h) => h.name.toLowerCase() === (subjectName || '').toLowerCase()
    );
    if (node && node.topics && node.topics.length > 0) {
      return node.topics.map((t) => t.name);
    }
    return [];
  }

  onSubjectChanged(rule: BlueprintRule): void {
    const topics = this.getTopicsForSubject(rule.subject);
    if (topics.length > 0) {
      rule.topic = topics[0];
    } else {
      rule.topic = '';
    }
    this.ruleSubjectChange.emit(rule);
  }

  onAddRule(): void {
    this.addRule.emit();
  }

  onRemoveRule(index: number): void {
    this.removeRule.emit(index);
  }
}
