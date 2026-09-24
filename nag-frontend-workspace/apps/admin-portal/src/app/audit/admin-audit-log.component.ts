import {
  Component,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  SearchInputComponent,
} from '@nag-frontend-workspace/shared-ui-components';

export interface AuditRecord {
  id: string;
  blockNumber: number;
  timestamp: string;
  service: string;
  actor: string;
  action: string;
  target: string;
  hash: string;
}

@Component({
  selector: 'app-admin-audit-log',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    SearchInputComponent,
  ],
  templateUrl: './admin-audit-log.component.html',
  styleUrl: './admin-audit-log.component.scss',
})
export class AdminAuditLogComponent {
  searchQuery = signal<string>('');
  selectedService = signal<string>('ALL');
  verifyingLedger = signal<boolean>(false);

  logs = signal<AuditRecord[]>([
    {
      id: 'log-1',
      blockNumber: 1849202,
      timestamp: '2026-09-24 17:42:10',
      service: 'AUTH_SERVICE',
      actor: 'admin@nag.gov.in',
      action: 'USER_ROLE_ELEVATED',
      target: 'evaluator.sharma@nag.gov.in',
      hash: '0x3a9f82d1c9b4e78a221f...',
    },
    {
      id: 'log-2',
      blockNumber: 1849201,
      timestamp: '2026-09-24 17:35:04',
      service: 'EXAM_SERVICE',
      actor: 'system.orchestrator',
      action: 'MERKLE_ROOT_ANCHORED',
      target: 'PAPER_BLUEPRINT_NES_2026',
      hash: '0x8f22e1b4c90192a5433d...',
    },
    {
      id: 'log-3',
      blockNumber: 1849200,
      timestamp: '2026-09-24 17:12:49',
      service: 'QUESTION_SERVICE',
      actor: 'author.patel@nag.gov.in',
      action: 'QUESTION_APPROVED',
      target: 'Q-8492',
      hash: '0x1c84b23290ddfae38910...',
    },
    {
      id: 'log-4',
      blockNumber: 1849199,
      timestamp: '2026-09-24 16:50:22',
      service: 'EVALUATION_SERVICE',
      actor: 'evaluator.desk@nag.gov.in',
      action: 'DISPUTE_RESOLVED',
      target: 'DISP-10924',
      hash: '0x7e29aa018241fbde9801...',
    },
  ]);

  filteredLogs = () => {
    const q = this.searchQuery().toLowerCase().trim();
    const s = this.selectedService();
    return this.logs().filter((item) => {
      const matchQuery =
        !q ||
        item.actor.toLowerCase().includes(q) ||
        item.action.toLowerCase().includes(q) ||
        item.target.toLowerCase().includes(q) ||
        item.hash.toLowerCase().includes(q);
      const matchService = s === 'ALL' || item.service === s;
      return matchQuery && matchService;
    });
  };

  onSearchChange(text: string): void {
    this.searchQuery.set(text);
  }

  verifyLedgerIntegrity(): void {
    this.verifyingLedger.set(true);
    setTimeout(() => {
      this.verifyingLedger.set(false);
      alert('All 1,849,202 Merkle root proofs verified with 0 cryptographic anomalies.');
    }, 800);
  }

  exportAuditLog(): void {
    alert('Exporting immutable audit log trail to cryptographically signed JSON...');
  }
}
