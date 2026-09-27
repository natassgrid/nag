import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-auth-brand-header',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="text-center space-y-2">
      <div class="inline-flex items-center gap-3">
        <div class="w-12 h-12 rounded-2xl bg-indigo-600 text-white flex items-center justify-center font-black text-2xl shadow-lg">
          NAG
        </div>
        <div class="text-left">
          <h1 class="text-2xl font-black text-white tracking-tight">{{ title }}</h1>
          <p class="text-xs text-indigo-300 uppercase tracking-widest font-semibold">{{ subtitle }}</p>
        </div>
      </div>
      <p class="text-slate-400 text-xs">
        {{ description }}
        @if (highlightTarget) {
          <b class="text-white font-mono ml-1">{{ highlightTarget }}</b>
        }
      </p>
    </div>
  `,
})
export class AuthBrandHeaderComponent {
  @Input() title = 'National Assessment Grid';
  @Input() subtitle = 'Candidate Portal';
  @Input() description = '';
  @Input() highlightTarget?: string;
}
