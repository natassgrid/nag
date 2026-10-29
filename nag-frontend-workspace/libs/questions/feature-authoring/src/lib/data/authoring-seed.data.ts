import { QuestionOption } from '@nag-frontend-workspace/questions-data-access';
import { AuthoringSubQuestion } from '../models/authoring.model';

export const DEFAULT_AUTHORING_CONTENT =
  'Consider a symmetric matrix $A \\in \\mathbb{R}^{n \\times n}$. Which of the following statements is ALWAYS true regarding its eigenvalues and eigenvectors?\n\n$$\\det(A - \\lambda I) = 0$$';

export const DEFAULT_AUTHORING_EXPLANATION =
  'By the Spectral Theorem for real symmetric matrices, all eigenvalues are real and eigenvectors corresponding to distinct eigenvalues are orthogonal.';

export const DEFAULT_AUTHORING_OPTIONS: QuestionOption[] = [
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

export const DEFAULT_PASSAGE_TITLE =
  'Quantum Mechanics and Wave-Particle Duality in Semiconductors';

export const DEFAULT_PASSAGE_CONTENT =
  'In semiconductor physics, electrons exhibit both particle and wave-like properties governed by the de Broglie relation $\\lambda = \\frac{h}{p}$. When electrons are confined to potential wells of dimensions comparable to their de Broglie wavelength, quantum size effects emerge.\n\nThe time-independent Schrödinger equation describes the stationary states:\n\n$$-\\frac{\\hbar^2}{2m^*} \\nabla^2 \\psi(r) + V(r)\\psi(r) = E\\psi(r)$$\n\nwhere $m^*$ is the effective mass and $V(r)$ is the confinement potential.';

export const DEFAULT_SUB_QUESTIONS: AuthoringSubQuestion[] = [
  {
    passageOrderIndex: 1,
    content:
      'According to the passage, under what condition do quantum size effects emerge in semiconductors?',
    questionType: 'MULTIPLE_CHOICE',
    difficulty: 'MEDIUM',
    cognitiveLevel: 'UNDERSTAND',
    marks: 4,
    negativeMarks: 1,
    explanation:
      'Quantum size effects emerge when confinement dimensions are comparable to the electron de Broglie wavelength.',
    options: [
      {
        id: 'A',
        text: 'When the temperature approaches absolute zero ($T \\to 0$ K).',
        isCorrect: false,
      },
      {
        id: 'B',
        text: 'When electrons are confined to dimensions comparable to their de Broglie wavelength.',
        isCorrect: true,
      },
      {
        id: 'C',
        text: 'When the effective mass $m^*$ becomes infinite.',
        isCorrect: false,
      },
      { id: 'D', text: 'When potential $V(r) = 0$ everywhere.', isCorrect: false },
    ],
  },
  {
    passageOrderIndex: 2,
    content:
      'In the Schrödinger formulation presented, what does $m^*$ represent?',
    questionType: 'MULTIPLE_CHOICE',
    difficulty: 'EASY',
    cognitiveLevel: 'REMEMBER',
    marks: 4,
    negativeMarks: 1,
    explanation:
      'The parameter $m^*$ denotes the effective mass of the electron in the lattice crystal potential.',
    options: [
      {
        id: 'A',
        text: 'Effective mass of the electron in the crystal potential.',
        isCorrect: true,
      },
      {
        id: 'B',
        text: 'Permittivity of the dielectric medium.',
        isCorrect: false,
      },
      {
        id: 'C',
        text: 'Reduced Planck constant squared.',
        isCorrect: false,
      },
      { id: 'D', text: 'Confinement potential depth.', isCorrect: false },
    ],
  },
];
