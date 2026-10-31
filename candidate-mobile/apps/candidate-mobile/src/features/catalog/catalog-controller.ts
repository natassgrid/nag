/**
 * NAG Exam Catalog Discovery Controller
 * Real-time search, categorization, eligibility determination (age, qualification, category),
 * fee waiver computation, and multi-criteria sorting.
 */

import { DOCKER_API_PATHS } from '@nag/candidate-core';
import { defaultDockerApiService } from '../../services/docker-api.service';

export type ExamCategory = 'ALL' | 'UPSC' | 'SSC' | 'NTA' | 'STATE_PSC' | 'BANKING';
export type ExamFilterStatus = 'ALL' | 'OPEN' | 'CLOSING_SOON' | 'UPCOMING';
export type CatalogSortOption = 'DATE_ASC' | 'FEE_ASC' | 'FEE_DESC' | 'TITLE_ASC';

export interface CandidateEligibilityProfile {
  age: number;
  qualification: '10TH' | '12TH' | 'GRADUATE' | 'POST_GRADUATE' | 'B_TECH';
  category: 'GEN' | 'OBC' | 'SC' | 'ST' | 'EWS';
  pwd: boolean;
}

export interface ExamEligibilityCriteria {
  minAge?: number;
  maxAge?: number;
  minQualification?: '10TH' | '12TH' | 'GRADUATE' | 'POST_GRADUATE' | 'B_TECH';
  allowedCategories?: string[];
}

export interface CatalogExam {
  id: string;
  code: string;
  title: string;
  conductingBody: string;
  category: 'UPSC' | 'SSC' | 'NTA' | 'STATE_PSC' | 'BANKING';
  description: string;
  registrationStartDate: string;
  registrationEndDate: string;
  examDate: string;
  feeAmount: number;
  durationMinutes: number;
  totalMarks: number;
  eligibilityCriteria?: ExamEligibilityCriteria;
  status: 'REGISTRATION_OPEN' | 'UPCOMING' | 'REGISTRATION_CLOSED';
}

export interface EligibilityResult {
  eligible: boolean;
  reasons: string[];
  feeWaiverApplicable: boolean;
  effectiveFee: number;
}

export interface CatalogState {
  exams: CatalogExam[];
  searchQuery: string;
  selectedCategory: ExamCategory;
  selectedStatus: ExamFilterStatus;
  selectedSort: CatalogSortOption;
  eligibilityProfile: CandidateEligibilityProfile | null;
  onlyEligible: boolean;
}

export const SAMPLE_CATALOG_EXAMS: CatalogExam[] = [
  {
    id: 'EXAM-UPSC-2026',
    code: 'UPSC-CSP-2026',
    title: 'Civil Services (Preliminary) Examination 2026',
    conductingBody: 'Union Public Service Commission (UPSC)',
    category: 'UPSC',
    description: 'National recruitment examination for IAS, IPS, IFS, and Central Civil Services Group A.',
    registrationStartDate: '2026-10-01',
    registrationEndDate: '2026-11-20',
    examDate: '2026-12-15',
    feeAmount: 100,
    durationMinutes: 120,
    totalMarks: 200,
    eligibilityCriteria: {
      minAge: 21,
      maxAge: 32,
      minQualification: 'GRADUATE',
    },
    status: 'REGISTRATION_OPEN',
  },
  {
    id: 'EXAM-NTA-JEE-2026',
    code: 'NTA-JEE-MAIN-2026',
    title: 'Joint Entrance Examination (Main) 2026 Session 1',
    conductingBody: 'National Testing Agency (NTA)',
    category: 'NTA',
    description: 'National level engineering entrance examination for admission into NITs, IIITs, and CFTIs.',
    registrationStartDate: '2026-10-15',
    registrationEndDate: '2026-11-05', // Closing soon!
    examDate: '2026-11-28',
    feeAmount: 1000,
    durationMinutes: 180,
    totalMarks: 300,
    eligibilityCriteria: {
      minAge: 16,
      maxAge: 25,
      minQualification: '12TH',
    },
    status: 'REGISTRATION_OPEN',
  },
  {
    id: 'EXAM-SSC-CGL-2026',
    code: 'SSC-CGL-2026',
    title: 'Combined Graduate Level Examination 2026 Tier-I',
    conductingBody: 'Staff Selection Commission (SSC)',
    category: 'SSC',
    description: 'Recruitment for Group B and C Gazetted/Non-Gazetted positions across Union Ministries.',
    registrationStartDate: '2026-10-20',
    registrationEndDate: '2026-12-10',
    examDate: '2027-01-10',
    feeAmount: 100,
    durationMinutes: 60,
    totalMarks: 200,
    eligibilityCriteria: {
      minAge: 18,
      maxAge: 30,
      minQualification: 'GRADUATE',
    },
    status: 'REGISTRATION_OPEN',
  },
  {
    id: 'EXAM-IBPS-PO-2026',
    code: 'IBPS-PO-XIV',
    title: 'Probationary Officers / Management Trainees (CRP PO/MT-XIV)',
    conductingBody: 'Institute of Banking Personnel Selection (IBPS)',
    category: 'BANKING',
    description: 'Common recruitment process for Probationary Officers in 11 participating public sector banks.',
    registrationStartDate: '2026-11-01',
    registrationEndDate: '2026-11-30',
    examDate: '2027-02-14',
    feeAmount: 850,
    durationMinutes: 60,
    totalMarks: 100,
    eligibilityCriteria: {
      minAge: 20,
      maxAge: 30,
      minQualification: 'GRADUATE',
    },
    status: 'UPCOMING',
  },
  {
    id: 'EXAM-UPPSC-PCS-2026',
    code: 'UPPSC-PCS-2026',
    title: 'Combined State / Upper Subordinate Services Examination 2026',
    conductingBody: 'Uttar Pradesh Public Service Commission (UPPSC)',
    category: 'STATE_PSC',
    description: 'Premier state civil services recruitment for Deputy Collector, DSP, and Block Development Officers.',
    registrationStartDate: '2026-09-01',
    registrationEndDate: '2026-10-15',
    examDate: '2026-12-22',
    feeAmount: 125,
    durationMinutes: 120,
    totalMarks: 200,
    eligibilityCriteria: {
      minAge: 21,
      maxAge: 40,
      minQualification: 'GRADUATE',
    },
    status: 'REGISTRATION_CLOSED',
  },
];

const QUALIFICATION_RANKS: Record<string, number> = {
  '10TH': 1,
  '12TH': 2,
  'GRADUATE': 3,
  'POST_GRADUATE': 4,
  'B_TECH': 3,
};

export class CatalogController {
  private state: CatalogState;
  private listeners: Set<(state: CatalogState) => void> = new Set();

  constructor(initialExams: CatalogExam[] = SAMPLE_CATALOG_EXAMS) {
    this.state = {
      exams: initialExams,
      searchQuery: '',
      selectedCategory: 'ALL',
      selectedStatus: 'ALL',
      selectedSort: 'DATE_ASC',
      eligibilityProfile: {
        age: 23,
        qualification: 'GRADUATE',
        category: 'GEN',
        pwd: false,
      },
      onlyEligible: false,
    };

    if (typeof window !== 'undefined' && typeof (window as any).NagReactMount === 'function') {
      void this.syncFromDocker();
    }
  }

  public async syncFromDocker(): Promise<void> {
    try {
      const res = await defaultDockerApiService.requestDocker<any>(
        DOCKER_API_PATHS.EXAMINATIONS.PUBLIC,
        { method: 'GET', skipAuth: true }
      );
      const items = Array.isArray(res)
        ? res
        : Array.isArray(res?.content)
        ? res.content
        : Array.isArray(res?.data)
        ? res.data
        : [];

      if (items.length > 0) {
        const mapped: CatalogExam[] = items.map((item: any, idx: number) => ({
          id: item.id || `EXAM-DOC-${idx}`,
          code: item.code || item.examCode || `CODE-${idx}`,
          title: item.title || item.name || 'Examination',
          conductingBody: item.conductingBody || item.conductingAuthority || 'National Assessment Grid',
          category: (['UPSC', 'SSC', 'NTA', 'STATE_PSC', 'BANKING'].includes(item.category)
            ? item.category
            : 'NTA') as any,
          description: item.description || '',
          registrationStartDate: item.registrationStartDate || new Date().toISOString().split('T')[0],
          registrationEndDate:
            item.registrationEndDate || new Date(Date.now() + 30 * 86400000).toISOString().split('T')[0],
          examDate:
            item.examDate || item.scheduledDate || new Date(Date.now() + 60 * 86400000).toISOString().split('T')[0],
          feeAmount: typeof item.feeAmount === 'number' ? item.feeAmount : 100,
          durationMinutes: typeof item.durationMinutes === 'number' ? item.durationMinutes : 120,
          totalMarks: typeof item.totalMarks === 'number' ? item.totalMarks : 200,
          eligibilityCriteria: item.eligibilityCriteria,
          status: item.status || 'REGISTRATION_OPEN',
        }));
        this.state.exams = mapped;
        this.notify();
      }
    } catch {
      // Non-blocking fallback to local seed exams
    }
  }

  public getState(): CatalogState {
    return { ...this.state };
  }

  public subscribe(listener: (state: CatalogState) => void): () => void {
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

  public setSearchQuery(query: string): void {
    this.state.searchQuery = query || '';
    this.notify();
  }

  public setCategory(category: ExamCategory): void {
    this.state.selectedCategory = category;
    this.notify();
  }

  public setStatus(status: ExamFilterStatus): void {
    this.state.selectedStatus = status;
    this.notify();
  }

  public setSort(sort: CatalogSortOption): void {
    this.state.selectedSort = sort;
    this.notify();
  }

  public setEligibilityProfile(profile: CandidateEligibilityProfile | null): void {
    this.state.eligibilityProfile = profile;
    this.notify();
  }

  public setOnlyEligible(onlyEligible: boolean): void {
    this.state.onlyEligible = onlyEligible;
    this.notify();
  }

  /**
   * Evaluates eligibility against age, degree qualifications,
   * category reservation relaxations, and fee waiver rights.
   */
  public checkEligibility(
    exam: CatalogExam,
    profile: CandidateEligibilityProfile | null = this.state.eligibilityProfile
  ): EligibilityResult {
    if (!profile) {
      return {
        eligible: true,
        reasons: ['No candidate profile specified; general entry presumed.'],
        feeWaiverApplicable: false,
        effectiveFee: exam.feeAmount,
      };
    }

    const reasons: string[] = [];
    let eligible = true;

    // Fee waiver check: SC, ST, PwD candidates have complete fee waiver under Central Govt norms
    const isFeeWaiver = profile.category === 'SC' || profile.category === 'ST' || profile.pwd;

    // Category age relaxation (SC/ST: +5 years, OBC: +3 years, PwD: +10 years)
    let ageRelaxation = 0;
    if (profile.pwd) {
      ageRelaxation += 10;
    } else if (profile.category === 'SC' || profile.category === 'ST') {
      ageRelaxation += 5;
    } else if (profile.category === 'OBC') {
      ageRelaxation += 3;
    }

    const criteria = exam.eligibilityCriteria;
    if (criteria) {
      if (criteria.minAge !== undefined && profile.age < criteria.minAge) {
        eligible = false;
        reasons.push(`Minimum age is ${criteria.minAge} (candidate age is ${profile.age}).`);
      }

      if (criteria.maxAge !== undefined) {
        const relaxedMaxAge = criteria.maxAge + ageRelaxation;
        if (profile.age > relaxedMaxAge) {
          eligible = false;
          reasons.push(
            `Maximum age limit of ${relaxedMaxAge} exceeded (candidate age is ${profile.age}, includes +${ageRelaxation} yrs category relaxation).`
          );
        }
      }

      if (criteria.minQualification) {
        const candidateRank = QUALIFICATION_RANKS[profile.qualification] || 0;
        const requiredRank = QUALIFICATION_RANKS[criteria.minQualification] || 0;
        if (candidateRank < requiredRank) {
          eligible = false;
          reasons.push(
            `Minimum qualification is ${criteria.minQualification} (candidate holds ${profile.qualification}).`
          );
        }
      }
    }

    if (eligible) {
      reasons.push('Candidate satisfies age, educational qualification, and reservation criteria.');
    }

    return {
      eligible,
      reasons,
      feeWaiverApplicable: isFeeWaiver,
      effectiveFee: isFeeWaiver ? 0 : exam.feeAmount,
    };
  }

  /**
   * Filters and sorts the exam list in real-time.
   */
  public getFilteredExams(): CatalogExam[] {
    const { exams, searchQuery, selectedCategory, selectedStatus, selectedSort, onlyEligible } =
      this.state;

    const query = (searchQuery || '').toLowerCase().trim();
    const now = Date.now();

    const filtered = exams.filter((exam) => {
      // 1. Text Search Filter (Title, Code, Conducting Body)
      if (query) {
        const titleMatch = (exam.title || '').toLowerCase().includes(query);
        const codeMatch = (exam.code || '').toLowerCase().includes(query);
        const bodyMatch = (exam.conductingBody || '').toLowerCase().includes(query);
        if (!titleMatch && !codeMatch && !bodyMatch) return false;
      }

      // 2. Category Filter
      if (selectedCategory !== 'ALL' && exam.category !== selectedCategory) {
        return false;
      }

      // 3. Status Filter
      if (selectedStatus === 'OPEN' && exam.status !== 'REGISTRATION_OPEN') {
        return false;
      }
      if (selectedStatus === 'UPCOMING' && exam.status !== 'UPCOMING') {
        return false;
      }
      if (selectedStatus === 'CLOSING_SOON') {
        if (exam.status !== 'REGISTRATION_OPEN') return false;
        const endMs = new Date(exam.registrationEndDate).getTime();
        const daysLeft = (endMs - now) / (1000 * 60 * 60 * 24);
        if (daysLeft < 0 || daysLeft > 7) return false;
      }

      // 4. Eligibility Filter
      if (onlyEligible) {
        const check = this.checkEligibility(exam);
        if (!check.eligible) return false;
      }

      return true;
    });

    // Sort Results
    return filtered.sort((a, b) => {
      switch (selectedSort) {
        case 'DATE_ASC':
          return new Date(a.examDate).getTime() - new Date(b.examDate).getTime();
        case 'FEE_ASC':
          return a.feeAmount - b.feeAmount;
        case 'FEE_DESC':
          return b.feeAmount - a.feeAmount;
        case 'TITLE_ASC':
          return a.title.localeCompare(b.title);
        default:
          return 0;
      }
    });
  }
}
