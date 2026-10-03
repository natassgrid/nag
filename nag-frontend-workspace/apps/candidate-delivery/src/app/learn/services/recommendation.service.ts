import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Recommendation, LearnerProfile } from '../models';

@Injectable({ providedIn: 'root' })
export class RecommendationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/recommendations';

  private tryParseJson<T>(value: any, fallback: T): T {
    if (typeof value === 'string') {
      try {
        return JSON.parse(value);
      } catch {
        return fallback;
      }
    }
    return value || fallback;
  }

  getLatest(): Observable<Recommendation | null> {
    return this.http.get<any>(`${this.apiUrl}/latest`).pipe(
      map(res => {
        if (!res) return null;
        return {
          ...res,
          weakTopicRecommendations: this.tryParseJson(res.weakTopicRecommendations, []),
          studyPlanItems: this.tryParseJson(res.studyPlanItems, []),
          suggestedPracticeSetIds: this.tryParseJson(res.suggestedPracticeSetIds, [])
        } as Recommendation;
      })
    );
  }

  getHistory(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<any>(`${this.apiUrl}/history`, { params }).pipe(
      map(res => {
        if (!res || !res.content) return res;
        return {
          ...res,
          content: res.content.map((item: any) => ({
            ...item,
            weakTopicRecommendations: this.tryParseJson(item.weakTopicRecommendations, []),
            studyPlanItems: this.tryParseJson(item.studyPlanItems, []),
            suggestedPracticeSetIds: this.tryParseJson(item.suggestedPracticeSetIds, [])
          }))
        };
      })
    );
  }

  dismiss(id: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/dismiss`, {});
  }

  getLearnerProfile(): Observable<LearnerProfile> {
    return this.http.get<any>(`${this.apiUrl}/learner-profile`).pipe(
      map(res => {
        if (!res) return res;
        return {
          ...res,
          weakTopics: this.tryParseJson(res.weakTopics, []),
          strongTopics: this.tryParseJson(res.strongTopics, []),
          topicAccuracyMap: this.tryParseJson(res.topicAccuracyMap, {})
        } as LearnerProfile;
      })
    );
  }
}
