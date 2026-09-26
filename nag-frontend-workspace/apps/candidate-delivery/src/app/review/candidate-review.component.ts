import {
  ChangeDetectionStrategy,
  Component,
  computed,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { PageHeaderComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  ReviewStatusFilter,
  ReviewQuestionItem,
  ReviewStats,
} from './models';
import {
  ReviewSummaryStatsComponent,
  ReviewFilterBarComponent,
  ReviewQuestionCardComponent,
  ReviewQuestionPaletteComponent,
} from './components';

@Component({
  selector: 'app-candidate-review',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    PageHeaderComponent,
    ReviewSummaryStatsComponent,
    ReviewFilterBarComponent,
    ReviewQuestionCardComponent,
    ReviewQuestionPaletteComponent,
  ],
  templateUrl: './candidate-review.component.html',
  styleUrl: './candidate-review.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateReviewComponent {
  readonly statusFilter = signal<ReviewStatusFilter>('ALL');
  readonly currentIndex = signal<number>(0);

  readonly questions = signal<ReviewQuestionItem[]>([
    {
      id: 'q-1',
      subject: 'Algorithms & Data Structures',
      content:
        'What is the tight asymptotic time complexity of finding the strongly connected components (SCC) of a directed graph $G = (V, E)$ using Tarjan’s algorithm?\\n\\n$$\\Theta(|V| + |E|)$$',
      explanation:
        'Tarjan’s algorithm performs a single depth-first search (DFS) pass over the graph, maintaining discovery times and low-link values on a stack. Its run time is linear: $\\mathcal{O}(|V| + |E|)$.',
      options: [
        { id: '1', text: '$\\mathcal{O}(|V| \\cdot |E|)$', isCorrect: false },
        { id: '2', text: '$\\mathcal{O}(|V| + |E|)$', isCorrect: true },
        { id: '3', text: '$\\mathcal{O}(|V|^2)$', isCorrect: false },
        { id: '4', text: '$\\mathcal{O}(|E| \\log |V|)$', isCorrect: false },
      ],
      userChoice: '2',
      isCorrect: true,
    },
    {
      id: 'q-2',
      subject: 'Computer Architecture',
      content:
        'In a 5-stage classic RISC pipeline (IF, ID, EX, MEM, WB), which hazard is caused by data dependencies between consecutive instructions when operand forwarding is NOT enabled?',
      explanation:
        'Read-After-Write (RAW) data hazard occurs when an instruction depends on the result of an earlier instruction that is still in the execution or memory pipeline stage.',
      options: [
        { id: '1', text: 'Structural Hazard', isCorrect: false },
        { id: '2', text: 'Control Hazard (Branch Delay)', isCorrect: false },
        { id: '3', text: 'RAW Data Hazard', isCorrect: true },
        { id: '4', text: 'WAR Anti-dependency Hazard', isCorrect: false },
      ],
      userChoice: '1',
      isCorrect: false,
    },
    {
      id: 'q-3',
      subject: 'Linear Algebra',
      content:
        'Consider a square matrix $A \\in \\mathbb{R}^{3 \\times 3}$ with eigenvalues $\\lambda_1 = 2, \\lambda_2 = 3, \\lambda_3 = -1$. What is the determinant of $A^2$?\\n\\n$$\\det(A^2) = (\\det A)^2$$',
      explanation:
        'The determinant of $A$ is the product of its eigenvalues: $\\det(A) = 2 \\times 3 \\times (-1) = -6$. Therefore $\\det(A^2) = (\\det A)^2 = (-6)^2 = 36$.',
      options: [
        { id: '1', text: '$-6$', isCorrect: false },
        { id: '2', text: '$6$', isCorrect: false },
        { id: '3', text: '$36$', isCorrect: true },
        { id: '4', text: '$12$', isCorrect: false },
      ],
      userChoice: '3',
      isCorrect: true,
    },
    {
      id: 'q-4',
      subject: 'Cryptography & DPI',
      content:
        'Which cryptographic property guarantees that even if an adversary obtains the private key of an ephemeral session, past session communications remain secure and cannot be decrypted?',
      explanation:
        'Perfect Forward Secrecy (PFS) ensures that compromise of long-term server private keys does not compromise past session keys generated via ephemeral Diffie-Hellman exchanges.',
      options: [
        { id: '1', text: 'Homomorphic Encryption', isCorrect: false },
        { id: '2', text: 'Perfect Forward Secrecy (PFS)', isCorrect: true },
        { id: '3', text: 'Zero-Knowledge Succinctness', isCorrect: false },
        { id: '4', text: 'Merkle Tree Invariance', isCorrect: false },
      ],
      userChoice: undefined,
      isCorrect: false,
    },
  ]);

  readonly stats = computed<ReviewStats>(() => {
    const list = this.questions();
    const correct = list.filter((q) => q.isCorrect).length;
    const incorrect = list.filter((q) => !q.isCorrect && q.userChoice !== undefined).length;
    const unattempted = list.filter((q) => q.userChoice === undefined).length;
    return { correct, incorrect, unattempted, avgTimePerItem: '1m 18s' };
  });

  readonly filteredQuestions = computed(() => {
    const filter = this.statusFilter();
    return this.questions().filter((q) => {
      if (filter === 'CORRECT') return q.isCorrect;
      if (filter === 'INCORRECT') return !q.isCorrect && q.userChoice !== undefined;
      if (filter === 'UNATTEMPTED') return q.userChoice === undefined;
      return true;
    });
  });

  readonly currentQuestion = computed(() => {
    const list = this.filteredQuestions();
    const idx = this.currentIndex();
    return list[idx] || list[0] || null;
  });

  onFilterChange(filter: ReviewStatusFilter): void {
    this.statusFilter.set(filter);
    this.currentIndex.set(0);
  }

  nextQuestion(): void {
    if (this.currentIndex() < this.filteredQuestions().length - 1) {
      this.currentIndex.update((i) => i + 1);
    }
  }

  prevQuestion(): void {
    if (this.currentIndex() > 0) {
      this.currentIndex.update((i) => i - 1);
    }
  }

  raiseDispute(question: ReviewQuestionItem): void {
    window.alert(`Dispute grievance logged for question reference: ${question.id}`);
  }
}
