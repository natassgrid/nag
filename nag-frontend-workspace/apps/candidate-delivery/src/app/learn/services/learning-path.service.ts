import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DynamicLearningPath,
  GatekeeperStatusResponse,
  StartAdaptiveQuizResponse,
  StartGatekeeperResponse,
} from '../models/learning-path.model';

@Injectable({ providedIn: 'root' })
export class LearningPathService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/learning-path';

  getLearningPath(): Observable<DynamicLearningPath> {
    return this.http.get<DynamicLearningPath>(this.apiUrl);
  }

  generateLearningPath(targetExam?: string): Observable<DynamicLearningPath> {
    return this.http.post<DynamicLearningPath>(`${this.apiUrl}/generate`, {
      targetExam: targetExam || 'GENERAL_COMPETITIVE',
    });
  }

  completeMilestone(milestoneId: string): Observable<DynamicLearningPath> {
    return this.http.post<DynamicLearningPath>(
      `${this.apiUrl}/milestones/${encodeURIComponent(milestoneId)}/complete`,
      {}
    );
  }

  startAdaptiveQuiz(): Observable<StartAdaptiveQuizResponse> {
    return this.http.post<StartAdaptiveQuizResponse>(
      `${this.apiUrl}/adaptive-quiz/start`,
      {}
    );
  }

  submitAdaptiveQuiz(
    quizSessionId: string,
    scorePercentage: number
  ): Observable<DynamicLearningPath> {
    return this.http.post<DynamicLearningPath>(
      `${this.apiUrl}/adaptive-quiz/submit`,
      { quizSessionId, scorePercentage }
    );
  }

  startGatekeeper(): Observable<StartGatekeeperResponse> {
    return this.http.post<StartGatekeeperResponse>(
      `${this.apiUrl}/gatekeeper/start`,
      {}
    );
  }

  submitGatekeeper(
    gatekeeperSessionId: string,
    scorePercentage: number
  ): Observable<DynamicLearningPath> {
    return this.http.post<DynamicLearningPath>(
      `${this.apiUrl}/gatekeeper/submit`,
      { gatekeeperSessionId, scorePercentage }
    );
  }

  getGatekeeperStatus(): Observable<GatekeeperStatusResponse> {
    return this.http.get<GatekeeperStatusResponse>(
      `${this.apiUrl}/gatekeeper/status`
    );
  }

  checkGatekeeperStatus(): Observable<GatekeeperStatusResponse> {
    return this.getGatekeeperStatus();
  }
}
