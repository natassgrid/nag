/**
 * Candidate Mobile Review & Answer Key Challenge Controller
 * Manages post-exam recorded response sheet inspection against provisional answer key,
 * candidate response verification, question filtering, and formal objection/challenge submissions.
 */

import {
  ReviewableExamAttempt,
  ReviewQuestionItem,
  ReviewOption,
  ReviewStatusFilter,
  ReviewSummaryStats,
  ReviewState,
  AnswerKeyChallengePayload,
  SubmittedChallengeRecord,
  ChallengeReasonType,
  StandardBookCitation,
} from './review-models';

export {
  ReviewableExamAttempt,
  ReviewQuestionItem,
  ReviewOption,
  ReviewStatusFilter,
  ReviewSummaryStats,
  ReviewState,
  AnswerKeyChallengePayload,
  SubmittedChallengeRecord,
  ChallengeReasonType,
  StandardBookCitation,
};

export const DEFAULT_REVIEW_QUESTIONS: ReviewQuestionItem[] = [
  {
    id: 'q-1',
    questionNumber: 1,
    subject: 'Algorithms & Data Structures',
    topic: 'Graph Algorithms',
    content:
      'What is the tight asymptotic time complexity of finding the strongly connected components (SCC) of a directed graph $$G = (V, E)$$ using Tarjan’s algorithm?\n\n$$\\Theta(|V| + |E|)$$',
    explanation:
      'Tarjan’s algorithm performs a single depth-first search (DFS) pass over the graph, maintaining discovery times and low-link values on a stack. Its run time is linear: $$\\mathcal{O}(|V| + |E|)$$.',
    options: [
      { id: '1', text: '$$\\mathcal{O}(|V| \\cdot |E|)$$', isCorrect: false },
      { id: '2', text: '$$\\mathcal{O}(|V| + |E|)$$', isCorrect: true },
      { id: '3', text: '$$\\mathcal{O}(|V|^2)$$', isCorrect: false },
      { id: '4', text: '$$\\mathcal{O}(|E| \\log |V|)$$', isCorrect: false },
    ],
    userChoice: '2',
    provisionalAnswerKey: '2',
    isCorrect: true,
    marksAwarded: 4.0,
    negativeMarks: 1.0,
  },
  {
    id: 'q-2',
    questionNumber: 2,
    subject: 'Computer Architecture',
    topic: 'Instruction Pipelining',
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
    provisionalAnswerKey: '3',
    isCorrect: false,
    marksAwarded: -1.0,
    negativeMarks: 1.0,
  },
  {
    id: 'q-3',
    questionNumber: 3,
    subject: 'Linear Algebra',
    topic: 'Eigenvalues & Determinants',
    content:
      'Consider a square matrix $$A \\in \\mathbb{R}^{3 \\times 3}$$ with eigenvalues $$\\lambda_1 = 2, \\lambda_2 = 3, \\lambda_3 = -1$$. What is the determinant of $$A^2$$?\n\n$$\\det(A^2) = (\\det A)^2$$',
    explanation:
      'The determinant of $$A$$ is the product of its eigenvalues: $$\\det(A) = 2 \\times 3 \\times (-1) = -6$$. Therefore $$\\det(A^2) = (\\det A)^2 = (-6)^2 = 36$$.',
    options: [
      { id: '1', text: '$$-6$$', isCorrect: false },
      { id: '2', text: '$$6$$', isCorrect: false },
      { id: '3', text: '$$36$$', isCorrect: true },
      { id: '4', text: '$$12$$', isCorrect: false },
    ],
    userChoice: '3',
    provisionalAnswerKey: '3',
    isCorrect: true,
    marksAwarded: 4.0,
    negativeMarks: 1.0,
  },
  {
    id: 'q-4',
    questionNumber: 4,
    subject: 'Cryptography & DPI',
    topic: 'Forward Secrecy',
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
    provisionalAnswerKey: '2',
    isCorrect: false,
    marksAwarded: 0.0,
    negativeMarks: 1.0,
  },
  {
    id: 'q-5',
    questionNumber: 5,
    subject: 'Chemical Engineering & Physical Chemistry',
    topic: 'Chemical Equilibrium & Le Chatelier Principle',
    content:
      'For the exothermic Haber synthesis of ammonia:\n\n$$\\ce{N2(g) + 3H2(g) <=> 2NH3(g)} \\quad (\\Delta H < 0)$$\n\nWhich combination of conditions maximizes the equilibrium molar yield of $$\\ce{NH3}$$ according to Le Chatelier’s principle?',
    explanation:
      'Since the forward reaction is exothermic and results in a decrease in gas moles ($$4 \\rightarrow 2$$), high pressure and moderate/lower temperature shift equilibrium forward.',
    options: [
      { id: '1', text: 'High pressure and low temperature', isCorrect: true },
      { id: '2', text: 'Low pressure and high temperature', isCorrect: false },
      { id: '3', text: 'Low pressure and low temperature', isCorrect: false },
      { id: '4', text: 'High pressure and extremely high temperature', isCorrect: false },
    ],
    userChoice: '1',
    provisionalAnswerKey: '1',
    isCorrect: true,
    marksAwarded: 4.0,
    negativeMarks: 1.0,
  },
];

export const DEFAULT_EXAM_ATTEMPT: ReviewableExamAttempt = {
  attemptId: 'att-nes-2026-s1-849202',
  examId: 'exam-1',
  examTitle: 'National Eligibility Screening (Computer Science & AI)',
  examCode: 'NES-2026-S1',
  examDate: '2026-09-28',
  shiftName: 'Shift 1 (09:00 AM - 12:00 PM)',
  rollNumber: '849202',
  candidateName: 'Aryan Sharma',
  challengeWindowClosesAt: '2026-10-05T18:00:00Z',
  challengeFeePerQuestion: 200,
  questions: DEFAULT_REVIEW_QUESTIONS,
  challenges: [],
};

export class ReviewController {
  private state: ReviewState;
  private readonly listeners = new Set<(state: ReviewState) => void>();

  constructor(initialAttempt: ReviewableExamAttempt = DEFAULT_EXAM_ATTEMPT) {
    const attempt = {
      ...initialAttempt,
      questions: initialAttempt.questions.map((q) => ({
        ...q,
        options: q.options.map((o) => ({ ...o })),
      })),
      challenges: [...(initialAttempt.challenges || [])],
    };

    const stats = this.computeStats(attempt.questions, attempt.challenges);

    this.state = {
      attempt,
      statusFilter: 'ALL',
      currentIndex: 0,
      selectedQuestionId: attempt.questions[0]?.id || null,
      isChallengeModalOpen: false,
      activeChallengeQuestion: null,
      submittedChallenges: [...attempt.challenges],
      stats,
      searchQuery: '',
    };
  }

  public getState(): ReviewState {
    return {
      ...this.state,
      attempt: {
        ...this.state.attempt,
        questions: this.state.attempt.questions.map((q) => ({
          ...q,
          options: q.options.map((o) => ({ ...o })),
        })),
        challenges: [...this.state.submittedChallenges],
      },
      submittedChallenges: [...this.state.submittedChallenges],
      stats: { ...this.state.stats },
    };
  }

  public subscribe(listener: (state: ReviewState) => void): () => void {
    this.listeners.add(listener);
    listener(this.getState());
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notify(): void {
    const currentState = this.getState();
    for (const listener of this.listeners) {
      listener(currentState);
    }
  }

  public computeStats(
    questions: ReviewQuestionItem[],
    challenges: SubmittedChallengeRecord[]
  ): ReviewSummaryStats {
    const totalQuestions = questions.length;
    let correct = 0;
    let incorrect = 0;
    let unattempted = 0;
    let rawScore = 0;
    let maxMarks = 0;

    for (const q of questions) {
      maxMarks += 4;
      if (q.userChoice === undefined) {
        unattempted++;
      } else if (q.isCorrect) {
        correct++;
        rawScore += q.marksAwarded;
      } else {
        incorrect++;
        rawScore += q.marksAwarded;
      }
    }

    const totalAttempted = correct + incorrect;
    const accuracyRate =
      totalAttempted > 0 ? Math.round((correct / totalAttempted) * 100) : 0;

    return {
      totalQuestions,
      totalAttempted,
      correct,
      incorrect,
      unattempted,
      challengedCount: challenges.length,
      rawScore: Math.round(rawScore * 100) / 100,
      maxMarks,
      accuracyRate,
      avgTimePerItem: '1m 24s',
    };
  }

  public setStatusFilter(filter: ReviewStatusFilter): void {
    this.state.statusFilter = filter;
    this.state.currentIndex = 0;
    const filtered = this.getFilteredQuestions();
    this.state.selectedQuestionId = filtered[0]?.id || null;
    this.notify();
  }

  public getFilteredQuestions(): ReviewQuestionItem[] {
    const { questions } = this.state.attempt;
    const { statusFilter } = this.state;

    switch (statusFilter) {
      case 'CORRECT':
        return questions.filter((q) => q.isCorrect);
      case 'INCORRECT':
        return questions.filter((q) => !q.isCorrect && q.userChoice !== undefined);
      case 'UNATTEMPTED':
        return questions.filter((q) => q.userChoice === undefined);
      case 'CHALLENGED':
        return questions.filter((q) => q.isChallenged);
      case 'ALL':
      default:
        return questions;
    }
  }

  public getCurrentQuestion(): ReviewQuestionItem | null {
    const filtered = this.getFilteredQuestions();
    const idx = this.state.currentIndex;
    return filtered[idx] || filtered[0] || null;
  }

  public selectQuestionIndex(index: number): void {
    const filtered = this.getFilteredQuestions();
    if (index >= 0 && index < filtered.length) {
      this.state.currentIndex = index;
      this.state.selectedQuestionId = filtered[index].id;
      this.notify();
    }
  }

  public selectQuestionById(id: string): void {
    const filtered = this.getFilteredQuestions();
    const idx = filtered.findIndex((q) => q.id === id);
    if (idx !== -1) {
      this.state.currentIndex = idx;
      this.state.selectedQuestionId = id;
      this.notify();
    }
  }

  public nextQuestion(): void {
    const filtered = this.getFilteredQuestions();
    if (this.state.currentIndex < filtered.length - 1) {
      this.state.currentIndex++;
      this.state.selectedQuestionId = filtered[this.state.currentIndex].id;
      this.notify();
    }
  }

  public prevQuestion(): void {
    if (this.state.currentIndex > 0) {
      this.state.currentIndex--;
      const filtered = this.getFilteredQuestions();
      this.state.selectedQuestionId = filtered[this.state.currentIndex].id;
      this.notify();
    }
  }

  public openChallengeModal(questionId: string): void {
    const question = this.state.attempt.questions.find((q) => q.id === questionId);
    if (!question) return;

    this.state.activeChallengeQuestion = question;
    this.state.isChallengeModalOpen = true;
    this.notify();
  }

  public closeChallengeModal(): void {
    this.state.isChallengeModalOpen = false;
    this.state.activeChallengeQuestion = null;
    this.notify();
  }

  public calculateChallengeFee(questionCount: number): number {
    const feePerItem = this.state.attempt.challengeFeePerQuestion || 200;
    return Math.max(0, questionCount) * feePerItem;
  }

  public submitChallenge(
    payload: AnswerKeyChallengePayload
  ): { success: boolean; record?: SubmittedChallengeRecord; error?: string } {
    if (!payload.reasonType) {
      return { success: false, error: 'Objection reason type is mandatory.' };
    }

    if (!payload.justificationText || payload.justificationText.trim().length < 10) {
      return {
        success: false,
        error: 'Academic justification must be at least 10 characters long.',
      };
    }

    if (
      !payload.bookCitation ||
      !payload.bookCitation.bookTitle?.trim() ||
      !payload.bookCitation.pageNumber?.trim()
    ) {
      return {
        success: false,
        error: 'Standard textbook title and page reference citation are required.',
      };
    }

    const question = this.state.attempt.questions.find((q) => q.id === payload.questionId);
    if (!question) {
      return { success: false, error: 'Referenced question does not exist.' };
    }

    const challengeId = `CHL-${Date.now()}`;
    const txnRef = `TXN-OBJ-${Math.floor(100000 + Math.random() * 900000)}`;

    const record: SubmittedChallengeRecord = {
      challengeId,
      questionId: payload.questionId,
      questionNumber: question.questionNumber,
      reasonType: payload.reasonType,
      claimedCorrectOptionId: payload.claimedCorrectOptionId,
      justificationText: payload.justificationText,
      bookCitation: payload.bookCitation,
      feePaid: payload.feeAmount || this.calculateChallengeFee(1),
      transactionReference: txnRef,
      submittedAt: new Date().toISOString(),
      status: 'SUBMITTED',
    };

    question.isChallenged = true;
    question.challengeDetails = record;

    this.state.submittedChallenges = [record, ...this.state.submittedChallenges];
    this.state.attempt.challenges = [...this.state.submittedChallenges];
    this.state.stats = this.computeStats(
      this.state.attempt.questions,
      this.state.submittedChallenges
    );
    this.state.isChallengeModalOpen = false;
    this.state.activeChallengeQuestion = null;

    this.notify();
    return { success: true, record };
  }

  public getChallenges(): SubmittedChallengeRecord[] {
    return [...this.state.submittedChallenges];
  }
}
