import { Component, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  PaperDetail,
  PaperTranslateResponse,
} from '@nag-frontend-workspace/examinations-data-access';

export interface IndicLanguageOption {
  code: string;
  label: string;
  native: string;
}

@Component({
  selector: 'nag-paper-inspection-drawer',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
  ],
  template: `
    @if (open()) {
      <div class=\"fixed inset-0 z-50 overflow-hidden bg-slate-900/50 flex justify-end\">
        <div class=\"w-full max-w-3xl bg-white h-full shadow-2xl flex flex-col animate-slide-in\">
          <!-- Drawer Header -->
          <div class=\"px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-900 text-white\">
            <div class=\"flex items-center gap-3\">
              <div class=\"w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center text-white\">
                <mat-icon>description</mat-icon>
              </div>
              <div>
                <h2 class=\"font-bold text-base text-white\">
                  {{ paperDetail()?.name || 'Question Paper Details' }}
                </h2>
                <div class=\"text-[11px] text-indigo-300 font-mono\">
                  ID: {{ paperId() }}
                </div>
              </div>
            </div>

            <button
              type=\"button\"\n              (click)=\"onClose()\"
              class=\"text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition-colors\"
            >
              <mat-icon>close</mat-icon>
            </button>
          </div>

          <!-- Drawer Body -->
          <div class=\"flex-1 overflow-y-auto p-6 space-y-6\">
            @if (loading()) {
              <div class=\"py-20 flex flex-col items-center justify-center gap-3\">
                <mat-spinner diameter=\"36\"></mat-spinner>
                <span class=\"text-xs text-slate-500\">Loading paper definition & questions...</span>
              </div>
            } @else if (paperDetail()) {
              <!-- Status & Cryptographic Key Details -->
              <div class=\"grid grid-cols-2 sm:grid-cols-4 gap-3\">
                <div class=\"p-3 bg-slate-50 rounded-xl border border-slate-200\">
                  <div class=\"text-[10px] uppercase font-bold text-slate-500\">Status</div>
                  <div class=\"text-xs font-bold text-slate-900 mt-1\">{{ paperDetail()?.status }}</div>
                </div>
                <div class=\"p-3 bg-slate-50 rounded-xl border border-slate-200\">
                  <div class=\"text-[10px] uppercase font-bold text-slate-500\">Total Items</div>
                  <div class=\"text-xs font-bold text-slate-900 mt-1\">{{ paperDetail()?.totalQuestions || paperDetail()?.questions?.length || 0 }} Qs</div>
                </div>
                <div class=\"p-3 bg-slate-50 rounded-xl border border-slate-200\">
                  <div class=\"text-[10px] uppercase font-bold text-slate-500\">Total Marks</div>
                  <div class=\"text-xs font-bold text-slate-900 mt-1\">{{ paperDetail()?.totalMarks || 0 }} Marks</div>
                </div>
                <div class=\"p-3 bg-slate-50 rounded-xl border border-slate-200\">
                  <div class=\"text-[10px] uppercase font-bold text-slate-500\">Difficulty Index</div>
                  <div class=\"text-xs font-bold text-slate-900 mt-1 font-mono\">
                    {{ paperDetail()?.difficultyScore ? (paperDetail()?.difficultyScore | number: '1.2-2') : 'Balanced' }}
                  </div>
                </div>
              </div>

              <!-- Cryptographic Package Seal -->
              @if (paperDetail()?.encryptionKeyId || paperDetail()?.encryptedPackageRef) {
                <div class=\"p-4 bg-emerald-50 rounded-xl border border-emerald-200 space-y-1.5\">
                  <div class=\"flex items-center gap-2 text-emerald-800 text-xs font-bold\">
                    <mat-icon class=\"!text-base\">lock</mat-icon>
                    <span>Encrypted Payload Anchored</span>
                  </div>
                  <div class=\"text-[11px] font-mono text-emerald-950 break-all\">
                    Key ID: {{ paperDetail()?.encryptionKeyId }}
                  </div>
                </div>
              }

              <!-- Indic AI Translation Trigger Sub-panel -->
              <div class=\"p-5 bg-indigo-50/60 rounded-2xl border border-indigo-100 space-y-4\">
                <div class=\"flex items-center justify-between\">
                  <div class=\"flex items-center gap-2\">
                    <mat-icon class=\"text-indigo-600\">translate</mat-icon>
                    <h3 class=\"font-bold text-xs text-indigo-950 uppercase tracking-wider\">
                      IndicTrans2 Multilingual Translation
                    </h3>
                  </div>
                  @if (isTranslating()) {
                    <span class=\"text-[11px] text-indigo-600 font-bold animate-pulse\">Processing...</span>
                  }
                </div>

                <div class=\"flex flex-col sm:flex-row gap-3 items-end\">
                  <div class=\"flex-1\">
                    <label for=\"drawer-lang-select\" class=\"block text-[10px] font-bold text-indigo-900 uppercase mb-1\">
                      Target Indic Language
                    </label>
                    <select
                      id=\"drawer-lang-select\"
                      [(ngModel)]=\"targetLanguage\"
                      class=\"w-full py-2 px-3 text-xs border border-indigo-200 rounded-xl bg-white focus:ring-2 focus:ring-indigo-500\"
                    >
                      @for (lang of supportedLanguages(); track lang.code) {
                        <option [value]=\"lang.code\">{{ lang.label }} ({{ lang.native }})</option>
                      }
                    </select>
                  </div>

                  <button
                    type=\"button\"
                    mat-flat-button
                    color=\"primary\"
                    (click)=\"onStartTranslation()\"
                    [disabled]=\"isTranslating()\"
                    class=\"!rounded-xl !text-xs !bg-indigo-600 hover:!bg-indigo-700 !text-white !py-2\"
                  >
                    <mat-icon class=\"!text-sm\">auto_awesome</mat-icon>
                    <span>Queue Batch Translation</span>
                  </button>
                </div>

                @if (activeTranslationJob()) {
                  <div class=\"pt-3 border-t border-indigo-100 space-y-2\">
                    <div class=\"flex items-center justify-between text-xs\">
                      <span class=\"text-indigo-900 font-semibold\">
                        Job {{ activeTranslationJob()?.jobId }}: {{ activeTranslationJob()?.status }}
                      </span>
                      <span class=\"font-mono text-indigo-700\">
                        {{ activeTranslationJob()?.processedQuestions }} / {{ activeTranslationJob()?.totalQuestions }} Qs
                      </span>
                    </div>
                    <mat-progress-bar
                      mode=\"determinate\"
                      [value]=\"activeTranslationJob()?.progressPercentage || 0\"
                    ></mat-progress-bar>
                  </div>
                }
              </div>

              <!-- Questions Breakdown List -->
              <div class=\"space-y-3\">
                <h3 class=\"font-bold text-slate-900 text-xs uppercase tracking-wider flex items-center justify-between\">
                  <span>Included Questions ({{ paperDetail()?.questions?.length || 0 }})</span>
                </h3>

                <div class=\"space-y-3\">
                  @for (q of paperDetail()?.questions || []; track q.questionId; let idx = $index) {
                    <div class=\"p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-2\">
                      <div class=\"flex items-start justify-between gap-3\">
                        <div class=\"flex items-center gap-2\">
                          <span class=\"w-6 h-6 rounded-lg bg-slate-200 text-slate-800 text-xs font-bold flex items-center justify-center font-mono\">
                            {{ idx + 1 }}
                          </span>
                          <span class=\"text-xs font-bold text-slate-700\">{{ q.subject }} &rsaquo; {{ q.topic }}</span>
                        </div>
                        <div class=\"flex items-center gap-1.5\">
                          <span class=\"text-[10px] font-bold px-2 py-0.5 rounded bg-slate-200 text-slate-700\">
                            {{ q.difficulty }}
                          </span>
                          <span class=\"text-[10px] font-bold px-2 py-0.5 rounded bg-indigo-100 text-indigo-700\">
                            {{ q.cognitiveLevel }}
                          </span>
                          <span class=\"text-[10px] font-mono font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200\">
                            +{{ q.marks }} / -{{ q.negativeMarks }}
                          </span>
                        </div>
                      </div>

                      <p class=\"text-xs text-slate-800 leading-relaxed font-sans\">{{ q.content }}</p>

                      @if (q.options?.length) {
                        <div class=\"grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1\">
                          @for (opt of q.options; track opt.id) {
                            <div
                              [ngClass]=\"{\n                                'bg-emerald-50 border-emerald-300 text-emerald-900 font-bold': opt.isCorrect,\n                                'bg-white border-slate-200 text-slate-700': !opt.isCorrect\n                              }\"
                              class=\"p-2 rounded-lg border text-[11px] flex items-center gap-2\"\n                            >
                              <span class=\"w-4 h-4 rounded-full bg-slate-100 text-slate-600 font-mono text-[9px] flex items-center justify-center font-bold\">
                                {{ opt.id }}
                              </span>
                              <span>{{ opt.text }}</span>
                            </div>
                          }
                        </div>
                      }

                      @if (q.explanation) {
                        <div class=\"text-[11px] text-slate-500 bg-white p-2.5 rounded-lg border border-slate-100\">
                          <span class=\"font-bold text-slate-700\">Solution:</span> {{ q.explanation }}
                        </div>
                      }
                    </div>
                  }
                </div>
              </div>
            }
          </div>

          <!-- Drawer Footer -->
          <div class=\"px-6 py-4 border-t border-slate-200 flex items-center justify-between bg-slate-50\">
            <button
              type=\"button\"
              mat-stroked-button
              (click)=\"onClose()\"
              class=\"!rounded-xl !border-slate-300\"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class PaperInspectionDrawerComponent {
  open = input<boolean>(false);
  loading = input<boolean>(false);
  paperDetail = input<PaperDetail | null>(null);
  paperId = input<string | null>(null);
  isTranslating = input<boolean>(false);
  activeTranslationJob = input<PaperTranslateResponse | null>(null);
  supportedLanguages = input<IndicLanguageOption[]>([]);

  close = output<void>();
  startTranslation = output<{ targetLanguage: string; overwriteExisting: boolean }>();

  targetLanguage = 'hi';
  overwriteExisting = false;

  onClose(): void {
    this.close.emit();
  }

  onStartTranslation(): void {
    this.startTranslation.emit({
      targetLanguage: this.targetLanguage,
      overwriteExisting: this.overwriteExisting,
    });
  }
}
