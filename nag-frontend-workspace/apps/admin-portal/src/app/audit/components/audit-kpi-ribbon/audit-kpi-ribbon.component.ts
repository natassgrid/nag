import {
  ChangeDetectionStrategy,
  Component,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

export interface AuditKpiStats {
  totalRecords: number;
  cryptographicIntegrity: number; // e.g. 100
  criticalAlerts: number;
  activeServices: number;
}

@Component({
  selector: 'nag-audit-kpi-ribbon',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './audit-kpi-ribbon.component.html',
  styleUrl: './audit-kpi-ribbon.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditKpiRibbonComponent {
  readonly stats = input.required<AuditKpiStats>();
}
