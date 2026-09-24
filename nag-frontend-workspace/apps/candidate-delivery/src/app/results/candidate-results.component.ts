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
  StatusBadgeComponent,
  QrCodeComponent,
} from '@nag-frontend-workspace/shared-ui-components';

export interface ScorecardRecord {
  examCode: string;
  examTitle: string;
  rollNumber: string;
  candidateName: string;
  totalScore: number;
  maxScore: number;
  percentile: number;
  nationalRank: number;
  totalAppeared: number;
  qualifyingStatus: 'QUALIFIED' | 'DISQUALIFIED';
  ledgerProofHash: string;
  merkleRoot: string;
  blockHeight: number;
  subjectScores: Array<{
    subject: string;
    marksObtained: number;
    maxMarks: number;
    accuracyRate: number;
  }>;
}

@Component({
  selector: 'app-candidate-results',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    StatCardComponent,
    StatusBadgeComponent,
    QrCodeComponent,
  ],
  templateUrl: './candidate-results.component.html',
  styleUrl: './candidate-results.component.scss',
})
export class CandidateResultsComponent {
  scorecard = signal<ScorecardRecord>({
    examCode: 'NES-2026-S1',
    examTitle: 'National Eligibility Screening (Computer Science & AI)',
    rollNumber: '849202',
    candidateName: 'Aditya Sharma',
    totalScore: 184,
    maxScore: 200,
    percentile: 99.4,
    nationalRank: 142,
    totalAppeared: 248900,
    qualifyingStatus: 'QUALIFIED',
    ledgerProofHash: '0x7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069',
    merkleRoot: '0x4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b',
    blockHeight: 1849202,
    subjectScores: [
      { subject: 'Algorithms & Data Structures', marksObtained: 48, maxMarks: 50, accuracyRate: 96 },
      { subject: 'Discrete Mathematics & Logic', marksObtained: 46, maxMarks: 50, accuracyRate: 92 },
      { subject: 'Operating Systems & Networks', marksObtained: 44, maxMarks: 50, accuracyRate: 88 },
      { subject: 'Machine Learning & Cryptography', marksObtained: 46, maxMarks: 50, accuracyRate: 92 },
    ],
  });

  downloadScorecard(): void {
    window.print();
  }
}
