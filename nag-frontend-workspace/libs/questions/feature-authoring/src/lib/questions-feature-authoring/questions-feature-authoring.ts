import {
  Component,
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
  QuestionOption,
  DifficultyLevel,
  QuestionType,
} from '@nag-frontend-workspace/questions-data-access';

export interface SubjectOption {
  id: number;
  name: string;
  topicId: number;
  topicName: string;
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
export class QuestionsFeatureAuthoring {
  private readonly questionBank = inject(QuestionBankService);
  private readonly router = inject(Router);

  saving = signal<boolean>(false);
  feedback = signal<{ type: 'success' | 'error'; message: string } | null>(null);

  readonly subjectOptions: SubjectOption[] = [
    { id: 8, name: 'Mathematics', topicId: 81, topicName: 'Algebra' },
    { id: 3, name: 'Quantitative Aptitude / Mathematical Abilities', topicId: 37, topicName: 'Geometry' },
    { id: 1, name: 'General Intelligence and Reasoning', topicId: 4, topicName: 'Problem Solving' },
    { id: 4, name: 'English Language and Comprehension', topicId: 41, topicName: 'Vocabulary' },
    { id: 2, name: 'General Awareness', topicId: 32, topicName: 'Everyday Science' },
    { id: 14, name: 'Data Interpretation and Logical Analysis', topicId: 107, topicName: 'Caselet and Arithmetic DI' },
  ];

  readonly cognitiveLevels = [
    { value: 'REMEMBER', label: 'Remember / Recall' },
    { value: 'UNDERSTAND', label: 'Understand / Conceptual' },
    { value: 'APPLY', label: 'Apply / Application' },
    { value: 'ANALYZE', label: 'Analyze / Critical Thinking' },
    { value: 'EVALUATE', label: 'Evaluate / Judgment' },
    { value: 'CREATE', label: 'Create / Synthesis' },
  ];

  selectedSubjectId = 8;
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

  difficultyVariant(): StatusVariant {
    switch (this.difficulty) {
      case 'EASY':
        return 'success';
      case 'MEDIUM':
        return 'warn';
      case 'HARD':
        return 'error';
      default:
        return 'neutral';
    }
  }

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
      // Re-index option IDs
      this.options.forEach((opt, idx) => {
        opt.id = this.getOptionLetter(idx);
      });
    }
  }

  toggleCorrect(index: number): void {
    if (this.type === 'MULTIPLE_CHOICE') {
      this.options.forEach((opt, idx) => {
        opt.isCorrect = idx === index;
      });
    } else {
      this.options[index].isCorrect = !this.options[index].isCorrect;
    }
  }

  getOptionLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }

  resetForm(): void {
    this.content = '';
    this.explanation = '';
    this.feedback.set(null);
    this.options = [
      { id: 'A', text: '', isCorrect: true },
      { id: 'B', text: '', isCorrect: false },
      { id: 'C', text: '', isCorrect: false },
      { id: 'D', text: '', isCorrect: false },
    ];
  }

  saveQuestion(): void {
    if (!this.content.trim()) {
      this.feedback.set({
        type: 'error',
        message: 'Question problem statement cannot be empty.',
      });
      return;
    }

    const selectedSubject =
      this.subjectOptions.find((s) => s.id === +this.selectedSubjectId) ||
      this.subjectOptions[0];

    const formattedOptions = this.options.map((opt, idx) => ({
      id: this.getOptionLetter(idx),
      text: opt.text.trim(),
      isCorrect: opt.isCorrect,
    }));

    const correctLetters = formattedOptions
      .filter((opt) => opt.isCorrect)
      .map((opt) => opt.id);

    if (
      (this.type === 'MULTIPLE_CHOICE' || this.type === 'MULTIPLE_SELECT') &&
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
      subjectId: selectedSubject.id,
      topicId: selectedSubject.topicId,
      subject: selectedSubject.name,
      topic: selectedSubject.topicName,
      difficulty: this.difficulty,
      cognitiveLevel: this.cognitiveLevel,
      questionType: backendQuestionType,
      content: this.content.trim(),
      explanation: this.explanation.trim(),
      answerKey: correctLetters.join(','),
      options: formattedOptions,
    };

    this.saving.set(true);
    this.feedback.set(null);

    this.questionBank.createQuestion(payload).subscribe({
      next: (created) => {
        this.saving.set(false);
        this.feedback.set({
          type: 'success',
          message: `Question authored and saved to encrypted bank successfully (ID: ${created.id.substring(0, 8)})!`,
        });
      },
      error: (err) => {
        this.saving.set(false);
        const detail =
          err?.error?.message ||
          err?.error?.detail ||
          'Failed to save question. Please verify all required fields.';
        this.feedback.set({
          type: 'error',
          message: detail,
        });
      },
    });
  }

  viewInBank(): void {
    this.router.navigate(['/questions']);
  }
}
