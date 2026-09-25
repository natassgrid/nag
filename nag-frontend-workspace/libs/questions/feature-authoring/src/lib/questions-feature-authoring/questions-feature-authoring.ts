import {
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  MathRendererComponent,
  StatusBadgeComponent,
  StatusVariant,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  QuestionBankService,
  PassageService,
  SubjectTopicService,
  Subject,
  Topic,
  Subtopic,
  QuestionOption,
  DifficultyLevel,
  QuestionType,
  PassageRequest,
  SubQuestionRequest,
} from '@nag-frontend-workspace/questions-data-access';

export type AuthoringMode = 'STANDALONE' | 'PASSAGE';

export interface AuthoringSubQuestion {
  id?: string;
  passageOrderIndex: number;
  content: string;
  questionType: QuestionType;
  difficulty: DifficultyLevel;
  cognitiveLevel: string;
  marks: number;
  negativeMarks: number;
  explanation: string;
  options: QuestionOption[];
}

@Component({
  selector: 'nag-questions-feature-authoring',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    MathRendererComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './questions-feature-authoring.component.html',
  styleUrl: './questions-feature-authoring.component.scss',
})
export class QuestionsFeatureAuthoring implements OnInit {
  private readonly questionBank = inject(QuestionBankService);
  private readonly passageService = inject(PassageService);
  private readonly subjectTopicService = inject(SubjectTopicService);
  private readonly router = inject(Router);

  mode = signal<AuthoringMode>('STANDALONE');
  saving = signal<boolean>(false);
  feedback = signal<{ type: 'success' | 'error'; message: string } | null>(null);

  // Taxonomy data
  subjects = signal<Subject[]>([]);
  topics = signal<Topic[]>([]);
  subtopics = signal<Subtopic[]>([]);
  loadingTaxonomy = signal<boolean>(false);

  selectedSubjectId: number | null = null;
  selectedTopicId: number | null = null;
  selectedSubtopicId: number | null = null;

  // Quick Add Taxonomy
  showNewSubject = false;
  showNewTopic = false;
  newSubjectName = '';
  newTopicName = '';

  // Standalone Question Form
  cognitiveLevel = 'UNDERSTAND';
  type: QuestionType = 'MULTIPLE_CHOICE';
  difficulty: DifficultyLevel = 'MEDIUM';
  marks = 4;
  negativeMarks = 1;

  content =
    'Consider a symmetric matrix $A \\in \\mathbb{R}^{n \\times n}$. Which of the following statements is ALWAYS true regarding its eigenvalues and eigenvectors?\n\n$$\\det(A - \\lambda I) = 0$$';
  explanation =
    'By the Spectral Theorem for real symmetric matrices, all eigenvalues are real and eigenvectors corresponding to distinct eigenvalues are orthogonal.';

  options: QuestionOption[] = [
    {
      id: 'A',
      text: 'All eigenvalues are strictly positive and distinct.',
      isCorrect: false,
    },
    {
      id: 'B',
      text: 'All eigenvalues are real and eigenvectors corresponding to distinct eigenvalues are orthogonal.',
      isCorrect: true,
    },
    {
      id: 'C',
      text: 'The matrix must have non-zero determinant.',
      isCorrect: false,
    },
    {
      id: 'D',
      text: 'Eigenvectors are strictly complex conjugate pairs.',
      isCorrect: false,
    },
  ];

  // Passage Comprehension Form
  passageTitle = 'Quantum Mechanics and Wave-Particle Duality in Semiconductors';
  passageContent =
    'In semiconductor physics, electrons exhibit both particle and wave-like properties governed by the de Broglie relation $\\lambda = \\frac{h}{p}$. When electrons are confined to potential wells of dimensions comparable to their de Broglie wavelength, quantum size effects emerge.\n\nThe time-independent Schrödinger equation describes the stationary states:\n\n$$-\\frac{\\hbar^2}{2m^*} \\nabla^2 \\psi(r) + V(r)\\psi(r) = E\\psi(r)$$\n\nwhere $m^*$ is the effective mass and $V(r)$ is the confinement potential.';

  activeSubQuestionIndex = 0;
  subQuestions: AuthoringSubQuestion[] = [
    {
      passageOrderIndex: 1,
      content: 'According to the passage, under what condition do quantum size effects emerge in semiconductors?',
      questionType: 'MULTIPLE_CHOICE',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'UNDERSTAND',
      marks: 4,
      negativeMarks: 1,
      explanation: 'Quantum size effects emerge when confinement dimensions are comparable to the electron de Broglie wavelength.',
      options: [
        { id: 'A', text: 'When the temperature approaches absolute zero ($T \\to 0$ K).', isCorrect: false },
        { id: 'B', text: 'When electrons are confined to dimensions comparable to their de Broglie wavelength.', isCorrect: true },
        { id: 'C', text: 'When the effective mass $m^*$ becomes infinite.', isCorrect: false },
        { id: 'D', text: 'When potential $V(r) = 0$ everywhere.', isCorrect: false },
      ],
    },
    {
      passageOrderIndex: 2,
      content: 'In the Schrödinger formulation presented, what does $m^*$ represent?',
      questionType: 'MULTIPLE_CHOICE',
      difficulty: 'EASY',
      cognitiveLevel: 'REMEMBER',
      marks: 4,
      negativeMarks: 1,
      explanation: 'The parameter $m^*$ denotes the effective mass of the electron in the lattice crystal potential.',
      options: [
        { id: 'A', text: 'Effective mass of the electron in the crystal potential.', isCorrect: true },
        { id: 'B', text: 'Permittivity of the dielectric medium.', isCorrect: false },
        { id: 'C', text: 'Reduced Planck constant squared.', isCorrect: false },
        { id: 'D', text: 'Confinement potential depth.', isCorrect: false },
      ],
    },
  ];

  readonly cognitiveLevels = [
    { value: 'REMEMBER', label: 'Remember / Recall' },
    { value: 'UNDERSTAND', label: 'Understand / Conceptual' },
    { value: 'APPLY', label: 'Apply / Application' },
    { value: 'ANALYZE', label: 'Analyze / Critical Thinking' },
    { value: 'EVALUATE', label: 'Evaluate / Judgment' },
    { value: 'CREATE', label: 'Create / Synthesis' },
  ];

  ngOnInit(): void {
    this.loadTaxonomy();
  }

  loadTaxonomy(): void {
    this.loadingTaxonomy.set(true);
    this.subjectTopicService.getSubjects().subscribe({
      next: (subs) => {
        this.subjects.set(subs || []);
        if (subs && subs.length > 0) {
          this.selectedSubjectId = subs[0].id;
          this.onSubjectChange(subs[0].id);
        }
        this.loadingTaxonomy.set(false);
      },
      error: () => this.loadingTaxonomy.set(false),
    });
  }

  onSubjectChange(subjectId: number): void {
    this.selectedSubjectId = subjectId;
    this.selectedTopicId = null;
    this.selectedSubtopicId = null;
    this.topics.set([]);
    this.subtopics.set([]);

    if (subjectId) {
      this.subjectTopicService.getTopics(subjectId).subscribe({
        next: (tops) => {
          this.topics.set(tops || []);
          if (tops && tops.length > 0) {
            this.selectedTopicId = tops[0].id;
            this.onTopicChange(tops[0].id);
          }
        },
      });
    }
  }

  onTopicChange(topicId: number): void {
    this.selectedTopicId = topicId;
    this.selectedSubtopicId = null;
    this.subtopics.set([]);

    if (this.selectedSubjectId && topicId) {
      this.subjectTopicService.getSubtopics(this.selectedSubjectId, topicId).subscribe({
        next: (subs) => {
          this.subtopics.set(subs || []);
          if (subs && subs.length > 0) {
            this.selectedSubtopicId = subs[0].id;
          }
        },
      });
    }
  }

  createQuickSubject(): void {
    if (!this.newSubjectName.trim()) return;
    this.subjectTopicService.createSubject({ name: this.newSubjectName.trim() }).subscribe({
      next: (sub) => {
        this.subjects.update((list) => [...list, sub]);
        this.selectedSubjectId = sub.id;
        this.newSubjectName = '';
        this.showNewSubject = false;
        this.onSubjectChange(sub.id);
      },
    });
  }

  createQuickTopic(): void {
    if (!this.newTopicName.trim() || !this.selectedSubjectId) return;
    this.subjectTopicService
      .createTopic(this.selectedSubjectId, { name: this.newTopicName.trim() })
      .subscribe({
        next: (top) => {
          this.topics.update((list) => [...list, top]);
          this.selectedTopicId = top.id;
          this.newTopicName = '';
          this.showNewTopic = false;
          this.onTopicChange(top.id);
        },
      });
  }

  setMode(m: AuthoringMode): void {
    this.mode.set(m);
    this.feedback.set(null);
  }

  difficultyVariant(diff: string = this.difficulty): StatusVariant {
    switch (diff) {
      case 'EASY':
        return 'success';
      case 'MEDIUM':
        return 'warn';
      case 'HARD':
      case 'EXPERT':
        return 'error';
      default:
        return 'neutral';
    }
  }

  // --- Standalone Option Helpers ---
  addOption(): void {
    const nextIdx = this.options.length;
    this.options.push({
      id: this.getOptionLetter(nextIdx),
      text: '',
      isCorrect: false,
    });
  }

  removeOption(index: number): void {
    if (this.options.length > 2) {
      this.options.splice(index, 1);
      this.options.forEach((opt, idx) => {
        opt.id = this.getOptionLetter(idx);
      });
    }
  }

  toggleCorrect(index: number): void {
    if (this.type === 'MULTIPLE_CHOICE' || this.type === 'SINGLE_MCQ') {
      this.options.forEach((opt, idx) => {
        opt.isCorrect = idx === index;
      });
    } else {
      this.options[index].isCorrect = !this.options[index].isCorrect;
    }
  }

  // --- Sub-Question Passage Helpers ---
  addSubQuestion(): void {
    if (this.subQuestions.length >= 6) {
      this.feedback.set({
        type: 'error',
        message: 'A comprehension passage can contain at most 6 sub-questions.',
      });
      return;
    }
    const nextIdx = this.subQuestions.length + 1;
    this.subQuestions.push({
      passageOrderIndex: nextIdx,
      content: '',
      questionType: 'MULTIPLE_CHOICE',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'UNDERSTAND',
      marks: 4,
      negativeMarks: 1,
      explanation: '',
      options: [
        { id: 'A', text: '', isCorrect: true },
        { id: 'B', text: '', isCorrect: false },
        { id: 'C', text: '', isCorrect: false },
        { id: 'D', text: '', isCorrect: false },
      ],
    });
    this.activeSubQuestionIndex = this.subQuestions.length - 1;
  }

  removeSubQuestion(index: number): void {
    if (this.subQuestions.length <= 2) {
      this.feedback.set({
        type: 'error',
        message: 'A comprehension passage requires a minimum of 2 sub-questions.',
      });
      return;
    }
    this.subQuestions.splice(index, 1);
    this.subQuestions.forEach((sq, idx) => {
      sq.passageOrderIndex = idx + 1;
    });
    if (this.activeSubQuestionIndex >= this.subQuestions.length) {
      this.activeSubQuestionIndex = this.subQuestions.length - 1;
    }
  }

  addSubQuestionOption(sqIndex: number): void {
    const sq = this.subQuestions[sqIndex];
    if (sq.options.length < 6) {
      const nextIdx = sq.options.length;
      sq.options.push({
        id: this.getOptionLetter(nextIdx),
        text: '',
        isCorrect: false,
      });
    }
  }

  removeSubQuestionOption(sqIndex: number, optIndex: number): void {
    const sq = this.subQuestions[sqIndex];
    if (sq.options.length > 2) {
      sq.options.splice(optIndex, 1);
      sq.options.forEach((opt, idx) => {
        opt.id = this.getOptionLetter(idx);
      });
    }
  }

  toggleSubQuestionCorrect(sqIndex: number, optIndex: number): void {
    const sq = this.subQuestions[sqIndex];
    if (sq.questionType === 'MULTIPLE_CHOICE' || sq.questionType === 'SINGLE_MCQ') {
      sq.options.forEach((opt, idx) => {
        opt.isCorrect = idx === optIndex;
      });
    } else {
      sq.options[optIndex].isCorrect = !sq.options[optIndex].isCorrect;
    }
  }

  getOptionLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }

  resetForm(): void {
    if (this.mode() === 'STANDALONE') {
      this.content = '';
      this.explanation = '';
      this.options = [
        { id: 'A', text: '', isCorrect: true },
        { id: 'B', text: '', isCorrect: false },
        { id: 'C', text: '', isCorrect: false },
        { id: 'D', text: '', isCorrect: false },
      ];
    } else {
      this.passageTitle = '';
      this.passageContent = '';
      this.subQuestions = [
        {
          passageOrderIndex: 1,
          content: '',
          questionType: 'MULTIPLE_CHOICE',
          difficulty: 'MEDIUM',
          cognitiveLevel: 'UNDERSTAND',
          marks: 4,
          negativeMarks: 1,
          explanation: '',
          options: [
            { id: 'A', text: '', isCorrect: true },
            { id: 'B', text: '', isCorrect: false },
            { id: 'C', text: '', isCorrect: false },
            { id: 'D', text: '', isCorrect: false },
          ],
        },
        {
          passageOrderIndex: 2,
          content: '',
          questionType: 'MULTIPLE_CHOICE',
          difficulty: 'MEDIUM',
          cognitiveLevel: 'UNDERSTAND',
          marks: 4,
          negativeMarks: 1,
          explanation: '',
          options: [
            { id: 'A', text: '', isCorrect: true },
            { id: 'B', text: '', isCorrect: false },
            { id: 'C', text: '', isCorrect: false },
            { id: 'D', text: '', isCorrect: false },
          ],
        },
      ];
      this.activeSubQuestionIndex = 0;
    }
    this.feedback.set(null);
  }

  saveItem(): void {
    if (this.mode() === 'STANDALONE') {
      this.saveStandaloneQuestion();
    } else {
      this.savePassage();
    }
  }

  private saveStandaloneQuestion(): void {
    if (!this.content.trim()) {
      this.feedback.set({
        type: 'error',
        message: 'Question problem statement cannot be empty.',
      });
      return;
    }

    const selectedSub = this.subjects().find((s) => s.id === this.selectedSubjectId);
    const selectedTop = this.topics().find((t) => t.id === this.selectedTopicId);
    const selectedSubtop = this.subtopics().find((st) => st.id === this.selectedSubtopicId);

    const formattedOptions = this.options.map((opt, idx) => ({
      id: this.getOptionLetter(idx),
      text: opt.text.trim(),
      isCorrect: opt.isCorrect,
    }));

    const correctLetters = formattedOptions
      .filter((opt) => opt.isCorrect)
      .map((opt) => opt.id);

    if (
      (this.type === 'MULTIPLE_CHOICE' || this.type === 'MULTIPLE_SELECT' || this.type === 'SINGLE_MCQ' || this.type === 'MULTIPLE_MCQ') &&
      correctLetters.length === 0
    ) {
      this.feedback.set({
        type: 'error',
        message: 'Please select at least one correct option before saving.',
      });
      return;
    }

    const backendQuestionType =
      this.type === 'MULTIPLE_CHOICE'
        ? 'SINGLE_MCQ'
        : this.type === 'MULTIPLE_SELECT'
        ? 'MULTI_MCQ'
        : this.type;

    const payload = {
      content: this.content.trim(),
      type: backendQuestionType,
      difficulty: this.difficulty,
      cognitiveLevel: this.cognitiveLevel,
      subject: selectedSub?.name || 'General',
      topic: selectedTop?.name || 'General',
      subtopic: selectedSubtop?.name || '',
      subjectId: this.selectedSubjectId || undefined,
      topicId: this.selectedTopicId || undefined,
      subtopicId: this.selectedSubtopicId || undefined,
      marks: this.marks,
      negativeMarks: this.negativeMarks,
      options: formattedOptions,
      answerKey: correctLetters.join(','),
      explanation: this.explanation.trim() || undefined,
      state: 'APPROVED',
    };

    this.saving.set(true);
    this.feedback.set(null);

    this.questionBank.createQuestion(payload).subscribe({
      next: (created) => {
        this.saving.set(false);
        this.feedback.set({
          type: 'success',
          message: `Question ${created.code || created.id} successfully created and committed to Question Bank!`,
        });
      },
      error: (err) => {
        this.saving.set(false);
        this.feedback.set({
          type: 'error',
          message: err?.error?.message || 'Failed to persist question. Please verify all fields and retry.',
        });
      },
    });
  }

  private savePassage(): void {
    if (!this.passageContent.trim()) {
      this.feedback.set({
        type: 'error',
        message: 'Passage reading content cannot be empty.',
      });
      return;
    }

    if (!this.selectedSubjectId) {
      this.feedback.set({
        type: 'error',
        message: 'Please select a Subject for the comprehension passage.',
      });
      return;
    }

    if (this.subQuestions.length < 2 || this.subQuestions.length > 6) {
      this.feedback.set({
        type: 'error',
        message: 'Comprehension passages require between 2 and 6 sub-questions.',
      });
      return;
    }

    // Validate subquestions
    for (let i = 0; i < this.subQuestions.length; i++) {
      const sq = this.subQuestions[i];
      if (!sq.content.trim()) {
        this.feedback.set({
          type: 'error',
          message: `Sub-question ${i + 1} content cannot be empty.`,
        });
        return;
      }
      const hasCorrect = sq.options.some((o) => o.isCorrect);
      if ((sq.questionType === 'MULTIPLE_CHOICE' || sq.questionType === 'SINGLE_MCQ' || sq.questionType === 'MULTIPLE_SELECT') && !hasCorrect) {
        this.feedback.set({
          type: 'error',
          message: `Sub-question ${i + 1} must have at least one correct option selected.`,
        });
        return;
      }
    }

    const selectedSub = this.subjects().find((s) => s.id === this.selectedSubjectId);
    const selectedTop = this.topics().find((t) => t.id === this.selectedTopicId);
    const selectedSubtop = this.subtopics().find((st) => st.id === this.selectedSubtopicId);

    const formattedSubQuestions: SubQuestionRequest[] = this.subQuestions.map((sq, idx) => {
      const correct = sq.options.filter((o) => o.isCorrect).map((o) => o.id).join(',');
      return {
        passageOrderIndex: idx + 1,
        content: sq.content.trim(),
        questionType: sq.questionType === 'MULTIPLE_CHOICE' ? 'SINGLE_MCQ' : (sq.questionType === 'MULTIPLE_SELECT' ? 'MULTI_MCQ' : sq.questionType),
        difficulty: sq.difficulty,
        cognitiveLevel: sq.cognitiveLevel,
        marks: sq.marks,
        negativeMarks: sq.negativeMarks,
        explanation: sq.explanation.trim() || undefined,
        answerKey: correct,
        options: sq.options.map((opt, oIdx) => ({
          id: this.getOptionLetter(oIdx),
          text: opt.text.trim(),
          isCorrect: opt.isCorrect,
        })),
      };
    });

    const request: PassageRequest = {
      title: this.passageTitle.trim() || undefined,
      content: this.passageContent.trim(),
      contentFormat: 'MIXED',
      subjectId: this.selectedSubjectId,
      topicId: this.selectedTopicId || undefined,
      subject: selectedSub?.name || 'General',
      topic: selectedTop?.name || 'General',
      subtopic: selectedSubtop?.name || undefined,
      subQuestions: formattedSubQuestions,
    };

    this.saving.set(true);
    this.feedback.set(null);

    this.passageService.createPassage(request).subscribe({
      next: (created) => {
        this.saving.set(false);
        this.feedback.set({
          type: 'success',
          message: `Passage set "${created.title || created.id}" with ${created.subQuestions?.length || this.subQuestions.length} sub-questions successfully created!`,
        });
      },
      error: (err) => {
        this.saving.set(false);
        this.feedback.set({
          type: 'error',
          message: err?.error?.message || 'Failed to save comprehension passage set. Please check fields and retry.',
        });
      },
    });
  }

  viewInBank(): void {
    this.router.navigate(['/questions']);
  }
}
