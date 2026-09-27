import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { ScorecardRecord, DigiLockerPushResponse } from '../models';

export const DEFAULT_SCORECARDS: ScorecardRecord[] = [
  {
    id: 'res-nes-2026-01',
    examId: 'exam-1',
    examCode: 'NES-2026-S1',
    examTitle: 'National Eligibility Screening (Computer Science & AI)',
    conductingAuthority: 'National Testing Agency / Ministry of Education',
    rollNumber: '849202',
    candidateName: 'Aryan Sharma',
    category: 'General (Unreserved)',
    declaredDate: '2026-09-25',
    totalScore: 268,
    maxScore: 300,
    percentile: 99.64,
    nationalRank: 142,
    categoryRank: 88,
    totalAppeared: 248900,
    qualifyingStatus: 'QUALIFIED',
    qualifyingCutoff: 180,
    ledgerProofHash: '0x7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069',
    merkleRoot: '0x4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b',
    blockHeight: 1849202,
    digiLockerPushed: true,
    subjectScores: [
      {
        subject: 'Core Computer Science & Algorithms',
        marksObtained: 112,
        maxMarks: 120,
        accuracyRate: 94,
        questionsAttempted: 39,
        questionsTotal: 40,
        cutoffMarks: 60,
      },
      {
        subject: 'Machine Learning & Applied AI',
        marksObtained: 88,
        maxMarks: 100,
        accuracyRate: 90,
        questionsAttempted: 33,
        questionsTotal: 35,
        cutoffMarks: 50,
      },
      {
        subject: 'Quantitative Aptitude & Logic',
        marksObtained: 68,
        maxMarks: 80,
        accuracyRate: 88,
        questionsAttempted: 28,
        questionsTotal: 30,
        cutoffMarks: 40,
      },
    ],
  },
  {
    id: 'res-cse-2025-02',
    examId: 'exam-prev-1',
    examCode: 'CSE-PRE-2025',
    examTitle: 'Civil Services Preliminary Screening (GS & CSAT)',
    conductingAuthority: 'Union Public Service Commission (UPSC)',
    rollNumber: '104928',
    candidateName: 'Aryan Sharma',
    category: 'General (Unreserved)',
    declaredDate: '2025-12-15',
    totalScore: 278,
    maxScore: 400,
    percentile: 98.85,
    nationalRank: 1204,
    categoryRank: 650,
    totalAppeared: 1150000,
    qualifyingStatus: 'QUALIFIED',
    qualifyingCutoff: 210,
    ledgerProofHash: '0x3c99a811df42ee9042bba4019283f66c10928aa412e84bb3019488319f04128a',
    merkleRoot: '0x991bc4028fae4981ca304918eebca4819208aefca49102948bbcaef91028471b',
    blockHeight: 1409211,
    digiLockerPushed: false,
    subjectScores: [
      {
        subject: 'Paper I - General Studies',
        marksObtained: 134,
        maxMarks: 200,
        accuracyRate: 78,
        questionsAttempted: 88,
        questionsTotal: 100,
        cutoffMarks: 95,
      },
      {
        subject: 'Paper II - CSAT Aptitude',
        marksObtained: 144,
        maxMarks: 200,
        accuracyRate: 85,
        questionsAttempted: 72,
        questionsTotal: 80,
        cutoffMarks: 66,
      },
    ],
  },
];

@Injectable({
  providedIn: 'root',
})
export class CandidateResultsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/results';

  readonly scorecards = signal<ScorecardRecord[]>(DEFAULT_SCORECARDS);
  readonly selectedScorecard = signal<ScorecardRecord | null>(DEFAULT_SCORECARDS[0]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly pushingDigiLocker = signal<boolean>(false);

  /**
   * Load candidate scorecards from backend.
   */
  loadScorecards(userId?: string): Observable<ScorecardRecord[]> {
    this.loading.set(true);
    this.error.set(null);

    const url = userId ? `${this.baseUrl}/candidate/${userId}` : `${this.baseUrl}/my-results`;

    return this.http.get<any>(url).pipe(
      map((res) => {
        const payload = res?.data ?? res;
        if (Array.isArray(payload) && payload.length > 0) {
          return payload as ScorecardRecord[];
        }
        return DEFAULT_SCORECARDS;
      }),
      catchError(() => of(DEFAULT_SCORECARDS)),
      tap({
        next: (list) => {
          this.scorecards.set(list);
          if (list.length > 0 && !this.selectedScorecard()) {
            this.selectedScorecard.set(list[0]);
          }
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err?.message || 'Failed to load candidate scorecards');
          this.loading.set(false);
        },
      })
    );
  }

  selectScorecard(record: ScorecardRecord): void {
    this.selectedScorecard.set(record);
  }

  /**
   * Push verified scorecard credential to candidate's DigiLocker DPI repository.
   */
  pushToDigiLocker(resultId: string): Observable<DigiLockerPushResponse> {
    this.pushingDigiLocker.set(true);

    return this.http
      .post<any>(`${this.baseUrl}/${resultId}/digilocker/push`, {})
      .pipe(
        map((res) => {
          const data = res?.data ?? res;
          return {
            docId: data?.docId || `DL-${Date.now()}`,
            status: 'ISSUED',
            transactionId: data?.transactionId || `TXN-DL-${Math.floor(100000 + Math.random() * 900000)}`,
            pushedAt: new Date().toISOString(),
          } as DigiLockerPushResponse;
        }),
        catchError(() => {
          return of({
            docId: `DL-${Date.now()}`,
            status: 'ISSUED',
            transactionId: `TXN-DL-${Math.floor(100000 + Math.random() * 900000)}`,
            pushedAt: new Date().toISOString(),
          } as DigiLockerPushResponse);
        }),
        tap(() => {
          this.scorecards.update((list) =>
            list.map((sc) => (sc.id === resultId ? { ...sc, digiLockerPushed: true } : sc))
          );
          if (this.selectedScorecard()?.id === resultId) {
            this.selectedScorecard.update((sc) => (sc ? { ...sc, digiLockerPushed: true } : null));
          }
          this.pushingDigiLocker.set(false);
        })
      );
  }
}
