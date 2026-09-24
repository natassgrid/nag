import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  StatCardComponent,
  StatusBadgeComponent,
} from '@nag-frontend-workspace/shared-ui-components';

export interface VerifiedCertificate {
  candidateRoll: string;
  candidateName: string;
  examTitle: string;
  score: number;
  maxScore: number;
  percentile: number;
  issueDate: string;
  ledgerProof: {
    blockNumber: number;
    proofHash: string;
    merkleRoot: string;
    verified: boolean;
  };
}

@Component({
  selector: 'app-verification',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    StatCardComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './verification.component.html',
  styleUrl: './verification.component.scss',
})
export class VerificationComponent {
  searchQuery = '';
  verifying = signal<boolean>(false);
  verifiedResult = signal<VerifiedCertificate | null>(null);

  verify(): void {
    if (!this.searchQuery.trim()) return;

    this.verifying.set(true);
    setTimeout(() => {
      this.verifiedResult.set({
        candidateRoll: '849202',
        candidateName: 'Aditya Sharma',
        examTitle: 'National Eligibility Screening (Computer Science & AI)',
        score: 184,
        maxScore: 200,
        percentile: 99.4,
        issueDate: '2026-09-24 18:00:00 UTC',
        ledgerProof: {
          blockNumber: 1849202,
          proofHash:
            '0x7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069',
          merkleRoot:
            '0x4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b',
          verified: true,
        },
      });
      this.verifying.set(false);
    }, 600);
  }
}
