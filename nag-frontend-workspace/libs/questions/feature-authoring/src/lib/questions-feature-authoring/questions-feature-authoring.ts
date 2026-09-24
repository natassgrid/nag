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
  MathRendererComponent,
  StatusBadgeComponent,
  StatusVariant,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  QuestionBankService,
  Question,
  QuestionOption,
  DifficultyLevel,
  QuestionType,
} from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-questions-feature-authoring',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
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

  saving = signal<boolean>(false);

  type: QuestionType = 'MULTIPLE_CHOICE';
  difficulty: DifficultyLevel = 'MEDIUM';
  marks = 4;
  negativeMarks = 1;
  content =
    'Consider a symmetric matrix $A \\in \\mathbb{R}^{n \\times n}$. Which of the following statements is ALWAYS true regarding its eigenvalues and eigenvectors?\\n\\n$$\\det(A - \\lambda I) = 0$$';
  explanation =
    'By the Spectral Theorem for real symmetric matrices, all eigenvalues are real and there exists an orthonormal basis of eigenvectors.';

  options: QuestionOption[] = [
    {
      id: '1',
      text: 'All eigenvalues are strictly positive and distinct.',
      isCorrect: false,
    },
    {
      id: '2',
      text: 'All eigenvalues are real and eigenvectors corresponding to distinct eigenvalues are orthogonal.',
      isCorrect: true,
    },
    {
      id: '3',
      text: 'The matrix must have non-zero determinant.',
      isCorrect: false,
    },
    {
      id: '4',
      text: 'Eigenvectors are strictly complex conjugate pairs.',
      isCorrect: false,
    },
  ];

  difficultyVariant = (): StatusVariant => {
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
  };

  addOption(): void {
    this.options.push({
      id: String(this.options.length + 1),
      text: '',
      isCorrect: false,
    });
  }

  removeOption(index: number): void {
    if (this.options.length > 2) {
      this.options.splice(index, 1);
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
    this.options = [
      { id: '1', text: '', isCorrect: true },
      { id: '2', text: '', isCorrect: false },
    ];
  }

  saveQuestion(): void {
    if (!this.content.trim()) return;

    this.saving.set(true);
    const newQuestion: Question = {
      id: 'q-' + Date.now(),
      code: 'Q-' + Math.floor(1000 + Math.random() * 9000),
      type: this.type,
      difficulty: this.difficulty,
      status: 'IN_REVIEW',
      content: this.content,
      options: this.options,
      marks: this.marks,
      negativeMarks: this.negativeMarks,
      tags: ['Mathematics', 'Linear-Algebra'],
    };

    this.questionBank.createQuestion(newQuestion).subscribe({
      next: () => {
        this.saving.set(false);
        alert('Question authored and saved to encrypted bank successfully!');
      },
      error: () => {
        this.saving.set(false);
        alert('Failed to save question.');
      },
    });
  }
}
