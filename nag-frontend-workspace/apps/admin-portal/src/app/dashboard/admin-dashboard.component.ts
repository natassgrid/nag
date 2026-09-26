import {
  Component,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  StatCardComponent,
} from '@nag-frontend-workspace/shared-ui-components';

export interface SystemServiceHealth {
  name: string;
  status: 'UP' | 'DEGRADED' | 'DOWN';
  latencyMs: number;
  uptime: string;
}

export interface SecurityEvent {
  id: string;
  timestamp: string;
  actor: string;
  action: string;
  resource: string;
  hash: string;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    StatCardComponent,
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent {
  systemServices = signal<SystemServiceHealth[]>([
    { name: 'Monolith Core', status: 'UP', latencyMs: 4, uptime: '99.99%' },
    { name: 'PostgreSQL + Vector', status: 'UP', latencyMs: 2, uptime: '100%' },
    { name: 'Redis Cache', status: 'UP', latencyMs: 1, uptime: '100%' },
    { name: 'HashiCorp Vault', status: 'UP', latencyMs: 3, uptime: '100%' },
    { name: 'Keycloak OIDC', status: 'UP', latencyMs: 5, uptime: '99.95%' },
    { name: 'LiteLLM AI Core', status: 'UP', latencyMs: 18, uptime: '99.9%' },
  ]);

  auditEvents = signal<SecurityEvent[]>([
    {
      id: 'SEC-1092',
      timestamp: '2026-09-24 17:42:10',
      actor: 'system.scheduler',
      action: 'MERKLE_ROOT_MINT',
      resource: 'Exam NES-2026-S1',
      hash: '0x8f22e1b4c90192a5433d849202af019b882371a2384a92c8192a838192a839a',
    },
    {
      id: 'SEC-1091',
      timestamp: '2026-09-24 17:35:00',
      actor: 'admin@nag.gov.in',
      action: 'ROLE_ELEVATION',
      resource: 'User: dr.gupta@nag.gov.in',
      hash: '0x1c84b23290ddfae38910bc49281a8c82910a928410294829103859201938591',
    },
    {
      id: 'SEC-1090',
      timestamp: '2026-09-24 16:50:22',
      actor: 'audit.evaluator',
      action: 'DISPUTE_FINALIZED',
      resource: 'Candidate #849202',
      hash: '0x3a9f82d1c9b4e78a221fbcd9203847291029482910385920193859182910294',
    },
  ]);
}
