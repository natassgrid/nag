import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

// --- Interfaces ---

export interface ExamSession {
  sessionId: string;
  examName: string;
  totalQuestions: number;
  durationMinutes: number;
  startedAt: string;
}

export interface Question {
  id: string;
  sequenceNumber: number;
  content: string;
  questionType: string; // MCQ, MSQ, NUMERICAL, DESCRIPTIVE
  options?: { id: string; text: string }[];
}

export interface ResponsePayload {
  sessionId: string;
  questionId: string;
  candidateId: string;
  selectedOptionIds?: string[];
  enteredValue?: string;
  timestamp: string;
  cumulativeTimeSpentMs: number;
  revisionSequence: number;
  saveSource: string;
}

export interface SessionStatus {
  remainingSeconds: number;
  answeredCount: number;
}

interface ApiResponse<T> {
  data: T;
  message?: string;
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class ExamService {
  private readonly baseUrl = '/api/v1';

  constructor(private http: HttpClient) {}

  startSession(data: { shiftId: string; examId: string; candidateId: string }): Observable<ExamSession> {
    return this.http
      .post<ApiResponse<ExamSession>>(`${this.baseUrl}/sessions/start`, data)
      .pipe(map(res => res.data));
  }

  getQuestion(sessionId: string, sequenceNumber: number): Observable<Question> {
    return this.http
      .get<ApiResponse<Question>>(`${this.baseUrl}/sessions/${sessionId}/questions/${sequenceNumber}`)
      .pipe(map(res => res.data));
  }

  saveResponse(data: ResponsePayload): Observable<any> {
    return this.http
      .post<ApiResponse<any>>(`${this.baseUrl}/responses`, data)
      .pipe(map(res => res.data));
  }

  getSessionStatus(sessionId: string): Observable<SessionStatus> {
    return this.http
      .get<ApiResponse<SessionStatus>>(`${this.baseUrl}/sessions/${sessionId}/status`)
      .pipe(map(res => res.data));
  }
}
