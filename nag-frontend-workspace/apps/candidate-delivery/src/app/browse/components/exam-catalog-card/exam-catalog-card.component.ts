import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { StatusBadgeComponent } from '@nag-frontend-workspace/shared-ui-components';
import { CatalogExam } from '../../models';

@Component({
  selector: 'nag-exam-catalog-card',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule, StatusBadgeComponent],
  templateUrl: './exam-catalog-card.component.html',
  styleUrl: './exam-catalog-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamCatalogCardComponent {
  readonly exam = input.required<CatalogExam>();

  readonly viewDetails = output<CatalogExam>();
  readonly applyNow = output<CatalogExam>();

  getCategoryBadge(category: string): { label: string; bg: string; text: string } {
    switch (category) {
      case 'ENGINEERING':
        return { label: 'Engineering & Tech', bg: 'bg-indigo-50 border-indigo-200', text: 'text-indigo-700' };
      case 'CIVIL_SERVICES':
        return { label: 'Civil Services', bg: 'bg-amber-50 border-amber-200', text: 'text-amber-800' };
      case 'BANKING':
        return { label: 'Banking & Finance', bg: 'bg-emerald-50 border-emerald-200', text: 'text-emerald-700' };
      case 'DEFENSE':
        return { label: 'Defense Services', bg: 'bg-rose-50 border-rose-200', text: 'text-rose-700' };
      case 'MEDICAL':
        return { label: 'Medical Sciences', bg: 'bg-teal-50 border-teal-200', text: 'text-teal-700' };
      default:
        return { label: 'National Assessment', bg: 'bg-slate-50 border-slate-200', text: 'text-slate-700' };
    }
  }
}
