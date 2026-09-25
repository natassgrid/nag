import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'nag-paper-feasibility-modal',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
  template: `
    @if (open()) {
      <div class=\"fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4\">
        <div class=\"w-full max-w-2xl bg-white rounded-3xl shadow-2xl p-6 space-y-6 animate-scale-up\">
          <div class=\"flex items-center justify-between pb-4 border-b border-slate-200\">
            <div class=\"flex items-center gap-3\">
              <div class=\"w-10 h-10 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center\">
                <mat-icon>rule</mat-icon>
              </div>
              <div>
                <h3 class=\"font-bold text-base text-slate-900\">Blueprint Inventory Sufficiency Audit</h3>
                <p class=\"text-xs text-slate-500\">Pre-flight verification against database question bank items</p>
              </div>
            </div>
            <button
              type=\"button\"
              (click)=\"onClose()\"
              class=\"text-slate-400 hover:text-slate-600\"
            >
              <mat-icon>close</mat-icon>
            </button>
          </div>

          @if (loading()) {
            <div class=\"py-12 flex flex-col items-center justify-center gap-3\">
              <mat-spinner diameter=\"36\"></mat-spinner>
              <span class=\"text-xs text-slate-500\">Checking inventory depth and constraint satisfaction...</span>
            </div>
          } @else if (result()) {
            <div class=\"space-y-4\">
              <!-- Overall Feasibility Banner -->
              <div
                [ngClass]=\"{\n                  'bg-emerald-50 border-emerald-200 text-emerald-900': result()?.feasible,\n                  'bg-rose-50 border-rose-200 text-rose-900': !result()?.feasible\n                }\"
                class=\"p-4 rounded-2xl border flex items-center gap-3\"
              >
                <mat-icon class=\"!text-2xl\" [ngClass]=\"result()?.feasible ? 'text-emerald-600' : 'text-rose-600'\">
                  {{ result()?.feasible ? 'check_circle' : 'warning' }}
                </mat-icon>
                <div>
                  <div class=\"font-bold text-sm\">
                    {{ result()?.feasible ? 'Question Inventory Sufficient' : 'Insufficient Question Inventory' }}
                  </div>
                  <div class=\"text-xs opacity-90\">
                    {{ result()?.message || (result()?.feasible ? 'All blueprint quotas can be satisfied by the active question bank pool.' : 'Some topic / difficulty quotas exceed available items in the bank.') }}
                  </div>
                </div>
              </div>

              <!-- Gaps Listing -->
              @if (result()?.gaps?.length) {
                <div class=\"space-y-2\">
                  <h4 class=\"text-xs font-bold text-slate-700 uppercase tracking-wider\">Identified Inventory Gaps:</h4>
                  <div class=\"space-y-2 max-h-60 overflow-y-auto pr-1\">
                    @for (gap of result()?.gaps; track $index) {
                      <div class=\"p-3 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between text-xs\">
                        <div>
                          <span class=\"font-bold text-slate-800\">{{ gap.subject }} &rsaquo; {{ gap.topic || 'All Topics' }}</span>
                          <div class=\"text-[11px] text-slate-500\">
                            Difficulty: {{ gap.difficulty || 'ANY' }} &bull; Required: <strong class=\"text-slate-700\">{{ gap.requiredCount }}</strong>, Available: <strong class=\"text-rose-600\">{{ gap.availableCount }}</strong>
                          </div>
                        </div>
                        <span class=\"text-xs font-bold text-rose-600 bg-rose-50 px-2 py-1 rounded-lg border border-rose-200\">
                          Deficit: -{{ gap.deficit || (gap.requiredCount - gap.availableCount) }} Qs
                        </span>
                      </div>
                    }
                  </div>
                </div>
              }
            </div>
          }

          <div class=\"pt-4 border-t border-slate-200 flex items-center justify-end\">
            <button
              type=\"button\"
              mat-stroked-button
              (click)=\"onClose()\"
              class=\"!rounded-xl text-xs\"
            >
              Dismiss Audit
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class PaperFeasibilityModalComponent {
  open = input<boolean>(false);
  loading = input<boolean>(false);
  result = input<any>(null);

  close = output<void>();

  onClose(): void {
    this.close.emit();
  }
}
