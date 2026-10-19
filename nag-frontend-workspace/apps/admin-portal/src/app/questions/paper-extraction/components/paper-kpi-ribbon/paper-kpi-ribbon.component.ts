import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { PaperKpiStats } from '../../models';

@Component({
  selector: 'nag-paper-kpi-ribbon',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './paper-kpi-ribbon.component.html',
  styleUrl: './paper-kpi-ribbon.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaperKpiRibbonComponent {
  stats = input.required<PaperKpiStats>();
}
