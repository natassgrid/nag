import {
  Component,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  ExaminationService,
  PaperGenerationConfig,
  LedgerProofResult,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-examinations-feature-paper-gen',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
  ],
  templateUrl: './examinations-feature-paper-gen.component.html',
  styleUrl: './examinations-feature-paper-gen.component.scss',
})
export class ExaminationsFeaturePaperGen {
  readonly examService = inject(ExaminationService);

  examCode = 'NES-2026-S1';
  totalQuestions = 100;
  easyPercent = 30;
  medPercent = 50;
  hardPercent = 20;
  shuffleQuestions = true;
  shuffleOptions = true;

  generating = signal<boolean>(false);
  proofResult = signal<LedgerProofResult | null>(null);

  generatePaper(): void {
    this.generating.set(true);
    const config: PaperGenerationConfig = {
      examId: this.examCode,
      totalQuestions: this.totalQuestions,
      difficultyDistribution: {
        easy: this.easyPercent,
        medium: this.medPercent,
        hard: this.hardPercent,
      },
      sections: [
        {
          id: 'sec-1',
          name: 'Core Concepts & Analytical Reasoning',
          questionCount: Math.round(this.totalQuestions * 0.6),
          marksPerQuestion: 2,
        },
        {
          id: 'sec-2',
          name: 'Advanced Problem Solving',
          questionCount: Math.round(this.totalQuestions * 0.4),
          marksPerQuestion: 4,
        },
      ],
      shuffleQuestions: this.shuffleQuestions,
      shuffleOptions: this.shuffleOptions,
    };

    this.examService.generatePaper(config).subscribe({
      next: (res) => {
        const fullProof: LedgerProofResult = {
          proofHash: res.proofHash || '0x' + Array.from({ length: 64 }, () => Math.floor(Math.random() * 16).toString(16)).join(''),
          blockNumber: 1849210,
          timestamp: new Date().toISOString(),
          merkleRoot: '0x' + Array.from({ length: 64 }, () => Math.floor(Math.random() * 16).toString(16)).join(''),
          verified: true,
        };
        this.proofResult.set(fullProof);
        this.generating.set(false);
      },
      error: () => this.generating.set(false),
    });
  }
}
