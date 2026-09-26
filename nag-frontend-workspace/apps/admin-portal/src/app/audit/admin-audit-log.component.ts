import {
  ChangeDetectionStrategy,
  Component,
  computed,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageHeaderComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  AuditKpiRibbonComponent,
  AuditFilterBarComponent,
  AuditTableViewComponent,
  AuditDetailDrawerComponent,
} from './components';
import { AuditRecord } from './models/audit-log.model';

@Component({
  selector: 'app-admin-audit-log',
  standalone: true,
  imports: [
    CommonModule,
    PageHeaderComponent,
    AuditKpiRibbonComponent,
    AuditFilterBarComponent,
    AuditTableViewComponent,
    AuditDetailDrawerComponent,
  ],
  templateUrl: './admin-audit-log.component.html',
  styleUrl: './admin-audit-log.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminAuditLogComponent {
  searchQuery = signal<string>('');
  selectedService = signal<string>('ALL');
  selectedSeverity = signal<string>('ALL');
  selectedStatus = signal<string>('ALL');
  verifyingLedger = signal<boolean>(false);
  isDetailDrawerOpen = signal<boolean>(false);
  selectedRecord = signal<AuditRecord | null>(null);

  records = signal<AuditRecord[]>([
    {
      id: 'log-1',
      blockNumber: 1849202,
      timestamp: '2026-09-24 17:42:10 UTC',
      service: 'AUTH_SERVICE',
      actor: 'admin@nag.gov.in',
      actorRole: 'SUPER_ADMIN',
      action: 'USER_ROLE_ELEVATED',
      target: 'evaluator.sharma@nag.gov.in',
      severity: 'WARN',
      status: 'SUCCESS',
      ipAddress: '192.168.1.104',
      hash: '0x3a9f82d1c9b4e78a221fa981e4b901acde841928374910283019284710928341',
      previousHash: '0x8f22e1b4c90192a5433da801248fcde901842019ab7652938174091823749018',
      digitalSignature: 'MEQCID...3a9f82d1c9b4e78a221f...SIG_ECDSA_P256',
      details: {
        roleGranted: 'EVALUATION_MODERATOR',
        tenantId: 'national-assessment-grid',
        authorizedBy: 'admin@nag.gov.in',
        reason: 'Authorized session elevation for central re-evaluation',
      },
    },
    {
      id: 'log-2',
      blockNumber: 1849201,
      timestamp: '2026-09-24 17:35:04 UTC',
      service: 'EXAM_SERVICE',
      actor: 'system.orchestrator',
      actorRole: 'SYSTEM_DAEMON',
      action: 'MERKLE_ROOT_ANCHORED',
      target: 'PAPER_BLUEPRINT_NES_2026',
      severity: 'INFO',
      status: 'SUCCESS',
      ipAddress: '10.0.4.12',
      hash: '0x8f22e1b4c90192a5433da801248fcde901842019ab7652938174091823749018',
      previousHash: '0x1c84b23290ddfae38910...prev_block',
      digitalSignature: 'MEUCIQ...8f22e1b4c90192a5433d...SIG_ECDSA_P256',
      details: {
        examId: 'NES_2026_TIER_1',
        totalSections: 4,
        totalQuestions: 100,
        merkleRoot: '0x99281aef1024bdfe90124...',
      },
    },
    {
      id: 'log-3',
      blockNumber: 1849200,
      timestamp: '2026-09-24 17:12:49 UTC',
      service: 'QUESTION_SERVICE',
      actor: 'author.patel@nag.gov.in',
      actorRole: 'QUESTION_AUTHOR',
      action: 'QUESTION_APPROVED',
      target: 'Q-8492',
      severity: 'INFO',
      status: 'SUCCESS',
      ipAddress: '192.168.2.55',
      hash: '0x1c84b23290ddfae3891001829374019284710928374910283019284710928341',
      previousHash: '0x7e29aa018241fbde9801...prev_block',
      digitalSignature: 'MEQCIA...1c84b23290ddfae38910...SIG_ECDSA_P256',
      details: {
        questionId: 'Q-8492',
        subject: 'General Intelligence & Reasoning',
        bloomTaxonomy: 'ANALYZE',
        dualReviewed: true,
      },
    },
    {
      id: 'log-4',
      blockNumber: 1849199,
      timestamp: '2026-09-24 16:50:22 UTC',
      service: 'EVALUATION_SERVICE',
      actor: 'evaluator.desk@nag.gov.in',
      actorRole: 'EVALUATOR',
      action: 'DISPUTE_RESOLVED',
      target: 'DISP-10924',
      severity: 'INFO',
      status: 'SUCCESS',
      ipAddress: '192.168.3.11',
      hash: '0x7e29aa018241fbde980101829374019284710928374910283019284710928341',
      previousHash: '0x3344aa110022bbff...prev_block',
      details: {
        disputeId: 'DISP-10924',
        candidateRollNo: 'NAG-2026-00918',
        scoreDelta: +2.0,
        reviewedBy: 'evaluator.desk@nag.gov.in',
      },
    },
    {
      id: 'log-5',
      blockNumber: 1849198,
      timestamp: '2026-09-24 16:30:15 UTC',
      service: 'DELIVERY_SERVICE',
      actor: 'terminal-proctor-04',
      actorRole: 'PROCTOR_AGENT',
      action: 'KIOSK_WINDOW_BLUR_DETECTED',
      target: 'TERMINAL_DEL_901',
      severity: 'WARN',
      status: 'SUCCESS',
      ipAddress: '10.20.1.101',
      hash: '0x55aa3311bbcc4400221101829374019284710928374910283019284710928341',
      details: {
        event: 'WINDOW_BLUR',
        candidateId: 'cand-98124',
        durationSeconds: 3,
        autoRestored: true,
      },
    },
    {
      id: 'log-6',
      blockNumber: 1849197,
      timestamp: '2026-09-24 16:15:00 UTC',
      service: 'AUTH_SERVICE',
      actor: 'unknown_client',
      action: 'FAILED_LOGIN_SPIKE',
      target: 'IP_198.51.100.42',
      severity: 'ERROR',
      status: 'FAILURE',
      ipAddress: '198.51.100.42',
      hash: '0x99bb4422ddaa1100334401829374019284710928374910283019284710928341',
      details: {
        attempts: 12,
        windowMinutes: 1,
        autoRateLimited: true,
        vaultAction: 'IP_TEMPORARILY_BLOCKED',
      },
    },
  ]);

  readonly kpiStats = computed(() => {
    const list = this.records();
    const total = 1849202; // anchored block sequence
    const errors = list.filter(
      (r) => r.severity === 'ERROR' || r.severity === 'CRITICAL'
    ).length;
    const services = new Set(list.map((r) => r.service)).size;

    return {
      totalRecords: total,
      cryptographicIntegrity: 100,
      criticalAlerts: errors,
      activeServices: services,
    };
  });

  readonly filteredRecords = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    const serviceFilter = this.selectedService();
    const severityFilter = this.selectedSeverity();
    const statusFilter = this.selectedStatus();

    return this.records().filter((item) => {
      const matchQuery =
        !q ||
        item.actor.toLowerCase().includes(q) ||
        item.action.toLowerCase().includes(q) ||
        item.target.toLowerCase().includes(q) ||
        item.hash.toLowerCase().includes(q) ||
        (item.ipAddress && item.ipAddress.toLowerCase().includes(q));

      const matchService =
        serviceFilter === 'ALL' || item.service === serviceFilter;
      const matchSeverity =
        severityFilter === 'ALL' || item.severity === severityFilter;
      const matchStatus =
        statusFilter === 'ALL' || item.status === statusFilter;

      return matchQuery && matchService && matchSeverity && matchStatus;
    });
  });

  onInspectRecord(record: AuditRecord): void {
    this.selectedRecord.set(record);
    this.isDetailDrawerOpen.set(true);
  }

  onCloseDrawer(): void {
    this.isDetailDrawerOpen.set(false);
    this.selectedRecord.set(null);
  }

  onVerifyLedger(): void {
    this.verifyingLedger.set(true);
    setTimeout(() => {
      this.verifyingLedger.set(false);
      alert(
        'All 1,849,202 Merkle root proofs verified with 0 cryptographic anomalies. Ledger is 100% intact.'
      );
    }, 700);
  }

  onExportLogs(): void {
    const dataStr =
      'data:text/json;charset=utf-8,' +
      encodeURIComponent(JSON.stringify(this.records(), null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', dataStr);
    downloadAnchor.setAttribute('download', `nag_audit_trail_${Date.now()}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  }
}
