import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ExamSession {
  sessionId: string;
  examId: string;
  examName: string;
  totalQuestions: number;
  durationMinutes: number;
  scheduledEndAt: string;
  navigationPolicy: 'Sequential' | 'Flexible' | 'Restricted';
  sections: ExamSection[];
}

export interface ExamSection {
  sectionId: string;
  name: string;
  questionCount: number;
  marksPerQuestion: number;
}

export interface Question {
  questionId: string;
  sequenceNumber: number;
  sectionId: string;
  questionType: string;
  content: string;
  contentType: 'HTML5' | 'LaTeX' | 'MathML' | 'Image' | 'Audio' | 'Video';
  options?: QuestionOption[];
  marks: number;
  negativeMarks: number;
}

export interface QuestionOption {
  optionId: string;
  label: string;
  content: string;
}

export interface ResponseSave {
  questionId: string;
  selectedOptionIds?: string[];
  enteredValue?: string;
  cumulativeTimeSpentMs: number;
  saveSource: 'USER' | 'AUTO_SAVE' | 'NAVIGATION';
}

@Injectable({ providedIn: 'root' })
export class ExamService {
  private readonly baseUrl = '/api/v1/delivery';

  constructor(private http: HttpClient) {}

  startSession(shiftId: string): Observable<ExamSession> {
    return this.http.post<ExamSession>(`${this.baseUrl}/sessions/start`, { shiftId });
  }

  getQuestion(sessionId: string, sequenceNumber: number): Observable<Question> {
    return this.http.get<Question>(`${this.baseUrl}/sessions/${sessionId}/questions/${sequenceNumber}`);
  }

  saveResponse(sessionId: string, response: ResponseSave): Observable<{ revisionSequence: number }> {
    return this.http.post<{ revisionSequence: number }>(
      `/api/v1/responses/${sessionId}/save`, response
    );
  }

  submitExam(sessionId: string): Observable<{ submittedAt: string }> {
    return this.http.post<{ submittedAt: string }>(
      `/api/v1/responses/${sessionId}/submit`, {}
    );
  }

  getSessionStatus(sessionId: string): Observable<{ remainingSeconds: number; answeredCount: number }> {
    return this.http.get<{ remainingSeconds: number; answeredCount: number }>(
      `${this.baseUrl}/sessions/${sessionId}/status`
    );
  }
}
