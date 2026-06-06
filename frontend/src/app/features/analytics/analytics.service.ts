import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface ScoreDistributionEntry {
  range: string;
  count: number;
}

export interface SectionAverage {
  sectionName: string;
  average: number;
}

export interface ExamAnalytics {
  examId: string;
  totalRegistered: number;
  totalAppeared: number;
  scoreDistribution: ScoreDistributionEntry[];
  sectionAverages: SectionAverage[];
  top10PercentileThreshold: number;
  bottom10PercentileThreshold: number;
}

interface ApiResponse<T> {
  data: T;
  message?: string;
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly baseUrl = '/api/v1/analytics';

  constructor(private http: HttpClient) {}

  getExamAnalytics(examId: string): Observable<ExamAnalytics> {
    return this.http
      .get<ApiResponse<ExamAnalytics>>(`${this.baseUrl}/exams/${examId}`)
      .pipe(map(res => res.data));
  }
}
