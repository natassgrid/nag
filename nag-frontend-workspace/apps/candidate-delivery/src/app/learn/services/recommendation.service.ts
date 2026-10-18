import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Recommendation, RecommendationLearnerProfile, StudyPlanItem } from '../models';
import { AdaptiveLearningService } from './adaptive-learning.service';

@Injectable({ providedIn: 'root' })
export class RecommendationService {
  private readonly http = inject(HttpClient);
  private readonly learningService = inject(AdaptiveLearningService);
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

  private normalizeStudyPlanItems(items: any): StudyPlanItem[] {
    const parsed = this.tryParseJson<any[]>(items, []);
    if (!Array.isArray(parsed)) return [];
    return parsed.map((item: any) => ({
      ...item,
      day: item.day ?? 1,
      topicName: item.topicName || item.focusTopic || 'Focus Topic',
      estimatedMinutes: item.estimatedMinutes || item.durationMinutes || 45,
      tasks: item.tasks || [],
      focusTopic: item.focusTopic || item.topicName,
      durationMinutes: item.durationMinutes || item.estimatedMinutes,
      completed: !!item.completed,
    }));
  }

  generate(examTarget?: string): Observable<Recommendation> {
    const resolvedTarget =
      examTarget ||
      this.learningService.activeEnrollment()?.examinationCode ||
      'GENERAL_COMPETITIVE';
    return this.http
      .post<any>(
        `${this.apiUrl}/generate?examTarget=${encodeURIComponent(resolvedTarget)}`,
        {}
      )
      .pipe(
        map((res) => {
          if (!res) return res;
          return {
            ...res,
            weakTopicRecommendations: this.tryParseJson(
              res.weakTopicRecommendations,
              []
            ),
            studyPlanItems: this.normalizeStudyPlanItems(res.studyPlanItems),
            suggestedPracticeSetIds: this.tryParseJson(
              res.suggestedPracticeSetIds,
              []
            ),
          } as Recommendation;
        })
      );
  }

  generateRecommendations(examTarget?: string): Observable<Recommendation> {
    return this.generate(examTarget);
  }

  getLatest(): Observable<Recommendation | null> {
    return this.http.get<any>(`${this.apiUrl}/latest`).pipe(
      map(res => {
        if (!res) return null;
        return {
          ...res,
          weakTopicRecommendations: this.tryParseJson(res.weakTopicRecommendations, []),
          studyPlanItems: this.normalizeStudyPlanItems(res.studyPlanItems),
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
            studyPlanItems: this.normalizeStudyPlanItems(item.studyPlanItems),
            suggestedPracticeSetIds: this.tryParseJson(item.suggestedPracticeSetIds, [])
          }))
        };
      })
    );
  }

  dismiss(id: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/dismiss`, {});
  }

  getLearnerProfile(): Observable<RecommendationLearnerProfile> {
    return this.http.get<any>(`${this.apiUrl}/learner-profile`).pipe(
      map(res => {
        if (!res) return res;
        return {
          ...res,
          weakTopics: this.tryParseJson(res.weakTopics, []),
          strongTopics: this.tryParseJson(res.strongTopics, []),
          topicAccuracyMap: this.tryParseJson(res.topicAccuracyMap, {})
        } as RecommendationLearnerProfile;
      })
    );
  }

  toggleStudyPlanDay(dayNumber: number, completed: boolean): Observable<Recommendation> {
    const params = new HttpParams().set('completed', completed.toString());
    return this.http.patch<any>(`${this.apiUrl}/study-plan/days/${dayNumber}`, {}, { params }).pipe(
      map(res => {
        if (!res) return res;
        return {
          ...res,
          weakTopicRecommendations: this.tryParseJson(res.weakTopicRecommendations, []),
          studyPlanItems: this.normalizeStudyPlanItems(res.studyPlanItems),
          suggestedPracticeSetIds: this.tryParseJson(res.suggestedPracticeSetIds, [])
        } as Recommendation;
      })
    );
  }
}
