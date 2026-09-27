import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { SearchInputComponent } from '@nag-frontend-workspace/shared-ui-components';

@Component({
  selector: 'nag-audit-filter-bar',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    SearchInputComponent,
  ],
  templateUrl: './audit-filter-bar.component.html',
  styleUrl: './audit-filter-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditFilterBarComponent {
  readonly searchQuery = input<string>('');
  readonly selectedService = input<string>('ALL');
  readonly selectedSeverity = input<string>('ALL');
  readonly selectedStatus = input<string>('ALL');
  readonly verifyingLedger = input<boolean>(false);

  readonly searchChange = output<string>();
  readonly serviceChange = output<string>();
  readonly severityChange = output<string>();
  readonly statusChange = output<string>();
  readonly verifyLedger = output<void>();
  readonly exportLogs = output<void>();

  readonly services = [
    { value: 'ALL', label: 'All Services' },
    { value: 'AUTH_SERVICE', label: 'Identity & Auth' },
    { value: 'EXAM_SERVICE', label: 'Examinations' },
    { value: 'QUESTION_SERVICE', label: 'Question Bank' },
    { value: 'DELIVERY_SERVICE', label: 'Exam Delivery' },
    { value: 'EVALUATION_SERVICE', label: 'Evaluation' },
    { value: 'ASSET_SERVICE', label: 'Media & Assets' },
    { value: 'AUDIT_SERVICE', label: 'Audit Engine' },
  ];

  readonly severities = ['ALL', 'INFO', 'WARN', 'ERROR', 'CRITICAL'];
  readonly statuses = ['ALL', 'SUCCESS', 'FAILURE'];
}
