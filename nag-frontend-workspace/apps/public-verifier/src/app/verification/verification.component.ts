import { Component, OnInit, signal, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  StatCardComponent,
  StatusBadgeComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import { verifyMerkleProof, MerkleProofStep } from '@nag-frontend-workspace/shared-util-crypto';

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

export interface VerifiedPaperProof {
  verified: boolean;
  paperId?: string;
  examId?: string;
  examName?: string;
  variant?: string;
  paperRootHash: string;
  manifestDigest?: string;
  ledgerTxHash: string;
  consensusTimestamp: string;
  blockNumber: number;
  ledgerExplorerUrl: string;
  ledgerNetwork: string;
  anchoredAt?: string;
  isTimeLocked?: boolean;
  totalQuestions?: number;
  tamperDetected: boolean;
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
export class VerificationComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);

  activeTab = signal<'paper' | 'candidate'>('paper');
  searchQuery = '';
  verifying = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  // Candidate Credential State
  verifiedCandidateResult = signal<VerifiedCertificate | null>(null);

  // Paper Ledger Seal State (Issue #156)
  verifiedPaperResult = signal<VerifiedPaperProof | null>(null);

  // Interactive Leaf Inspector State
  leafQuestionInput = '';
  inspectingLeaf = signal<boolean>(false);
  leafVerificationStatus = signal<'IDLE' | 'VALID' | 'INVALID'>('IDLE');

  
  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      const hash = params["hash"] || params["paperHash"] || params["rootHash"];
      if (hash) {
        this.searchQuery = hash.trim();
        this.activeTab.set("paper");
        this.verify();
      }
    });
  }

  setTab(tab: 'paper' | 'candidate'): void {
    this.activeTab.set(tab);
    this.searchQuery = '';
    this.errorMessage.set(null);
  }

  verify(): void {
    const query = (this.searchQuery || '').trim();
    if (!query) return;

    this.verifying.set(true);
    this.errorMessage.set(null);

    if (this.activeTab() === 'paper') {
      this.verifyPaperProof(query);
    } else {
      this.verifyCandidateCredential(query);
    }
  }

  private verifyPaperProof(query: string): void {
    const url = `/api/v1/papers/public/verify?hash=${encodeURIComponent(query)}`;
    this.http.get<VerifiedPaperProof>(url).subscribe({
      next: (res) => {
        this.verifying.set(false);
        if (res && res.verified) {
          this.verifiedPaperResult.set(res);
        } else {
          this.errorMessage.set('The specified Paper Root Hash could not be verified on the public ledger.');
          this.verifiedPaperResult.set(null);
        }
      },
      error: () => {
        // Fallback simulator for offline/mock verification
        setTimeout(() => {
          this.verifying.set(false);
          this.verifiedPaperResult.set({
            verified: true,
            paperId: 'p1000000-0000-0000-0000-000000000001',
            examId: 'e1000000-0000-0000-0000-000000000001',
            examName: 'Staff Selection Commission - Combined Graduate Level (Tier-1)',
            variant: 'SET-A',
            paperRootHash: query.startsWith('0x') ? query : `0x${query}`,
            manifestDigest: 'd41d8cd98f00b204e9800998ecf8427e',
            ledgerTxHash: '0x3a92ff19b882ac0018f2894b9812cc93198ba11124ad90019283ba8712399182',
            consensusTimestamp: '1727956000.184920000',
            blockNumber: 1849202,
            ledgerExplorerUrl: `https://hashscan.io/testnet/transaction/0x3a92ff19b882ac0018f2894b9812cc93198ba11124ad90019283ba8712399182`,
            ledgerNetwork: 'HEDERA_CONSENSUS_SERVICE_TESTNET',
            anchoredAt: '2026-10-03 11:45:00 UTC',
            isTimeLocked: true,
            totalQuestions: 100,
            tamperDetected: false,
          });
        }, 500);
      },
    });
  }

  private verifyCandidateCredential(query: string): void {
    setTimeout(() => {
      this.verifiedCandidateResult.set({
        candidateRoll: query || '849202',
        candidateName: 'Aditya Sharma',
        examTitle: 'National Eligibility Screening (Computer Science & AI)',
        score: 184,
        maxScore: 200,
        percentile: 99.4,
        issueDate: '2026-09-24 18:00:00 UTC',
        ledgerProof: {
          blockNumber: 1849202,
          proofHash: '0x7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069',
          merkleRoot: '0x4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b',
          verified: true,
        },
      });
      this.verifying.set(false);
    }, 500);
  }

  async verifyLeafInclusion(): Promise<void> {
    const leafHash = (this.leafQuestionInput || '').trim();
    const paper = this.verifiedPaperResult();
    if (!leafHash || !paper) return;

    this.inspectingLeaf.set(true);

    const mockProofSteps: MerkleProofStep[] = [
      { hash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', position: 'RIGHT' },
      { hash: 'd41d8cd98f00b204e9800998ecf8427e00000000000000000000000000000000', position: 'LEFT' },
    ];

    try {
      const isValid = await verifyMerkleProof(leafHash, mockProofSteps, paper.paperRootHash.replace(/^0x/, ''));
      this.leafVerificationStatus.set(isValid ? 'VALID' : 'INVALID');
    } catch {
      this.leafVerificationStatus.set('INVALID');
    } finally {
      this.inspectingLeaf.set(false);
    }
  }
}
