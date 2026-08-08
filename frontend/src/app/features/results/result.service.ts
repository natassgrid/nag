import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of, catchError } from 'rxjs';

export interface SectionScore {
  sectionName: string;
  score: number;
  maxMarks: number;
}

export interface ResultResponse {
  id: string;
  candidateId: string;
  examId: string;
  totalScore: number;
  sectionScores: SectionScore[];
  overallRank: number;
  overallPercentile: number;
  scorecardPdfRef: string;
  createdAt: string;
}

interface ApiResponse<T> {
  data: T;
  message?: string;
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class ResultService {
  private readonly baseUrl = '/api/v1/results';

  constructor(private http: HttpClient) {}

  getResults(candidateId: string): Observable<ResultResponse[]> {
    return this.http
      .get<ApiResponse<ResultResponse[]>>(`${this.baseUrl}?candidateId=${candidateId}`)
      .pipe(
        map(res => res?.data ?? []),
        catchError(() => of([] as ResultResponse[]))
      );
  }

  getResult(candidateId: string, examId: string): Observable<ResultResponse> {
    return this.http
      .get<ApiResponse<ResultResponse>>(`${this.baseUrl}/${candidateId}/${examId}`)
      .pipe(
        map(res => res?.data ?? ({} as ResultResponse)),
        catchError(() => of({} as ResultResponse))
      );
  }
}
