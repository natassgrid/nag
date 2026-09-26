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
  selector: 'nag-audit-detail-drawer',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './audit-detail-drawer.component.html',
  styleUrl: './audit-detail-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditDetailDrawerComponent {
  readonly record = input<AuditRecord | null>(null);
  readonly isOpen = input<boolean>(false);

  readonly closeDrawer = output<void>();

  copyToClipboard(text?: string): void {
    if (!text) return;
    navigator.clipboard?.writeText(text);
  }

  copyJson(obj: unknown): void {
    if (!obj) return;
    navigator.clipboard?.writeText(JSON.stringify(obj, null, 2));
  }
}
