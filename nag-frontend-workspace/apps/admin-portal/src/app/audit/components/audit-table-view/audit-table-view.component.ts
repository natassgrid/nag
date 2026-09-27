import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuditRecord } from '../../models/audit-log.model';

@Component({
  selector: 'nag-audit-table-view',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './audit-table-view.component.html',
  styleUrl: './audit-table-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditTableViewComponent {
  readonly records = input<AuditRecord[]>([]);
  readonly loading = input<boolean>(false);

  readonly inspectRecord = output<AuditRecord>();

  copyHash(hash: string): void {
    navigator.clipboard?.writeText(hash);
  }
}
