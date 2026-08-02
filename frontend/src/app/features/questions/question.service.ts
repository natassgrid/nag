import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface QuestionResponse {
  id: string;
  subject: string;
  topic: string;
  subtopic: string;
  chapter: string;
  difficulty: string;
  cognitiveLevel: string;
  questionType: string;
  content: string;
  answerKey: string;
  state: string;
  authorId: string;
  createdAt: string;
  options?: { id: string; text: string; isCorrect: boolean }[];
}

export interface CreateQuestionRequest {
  subject: string;
  topic: string;
  subtopic?: string;
  difficulty: string;
  cognitiveLevel: string;
  questionType: string;
  content: string;
  answerKey?: string;
  options?: { id: string; text: string; isCorrect: boolean }[];
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // current page (0-based)
}

interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class QuestionService {
  private readonly baseUrl = '/api/v1/questions';

  constructor(private http: HttpClient) {}

  getQuestions(filters?: {
    subject?: string;
    topic?: string;
    difficulty?: string;
    state?: string;
    page?: number;
    size?: number;
  }): Observable<PagedResponse<QuestionResponse>> {
    let params = new HttpParams();
    if (filters) {
      if (filters.subject)    params = params.set('subject', filters.subject);
      if (filters.topic)      params = params.set('topic', filters.topic);
      if (filters.difficulty) params = params.set('difficulty', filters.difficulty);
      if (filters.state)      params = params.set('state', filters.state);
      params = params.set('page', String(filters.page ?? 0));
      params = params.set('size', String(filters.size ?? 20));
    }
    return this.http
      .get<ApiResponse<any>>(this.baseUrl, { params })
      .pipe(map(res => {
        const payload = res?.data ?? res;
        if (Array.isArray(payload)) {
          return { content: payload, totalElements: payload.length, totalPages: 1, size: payload.length, number: 0 };
        }
        if (payload && Array.isArray(payload.content)) {
          return {
            content: payload.content,
            totalElements: payload.totalElements ?? payload.content.length,
            totalPages: payload.totalPages ?? 1,
            size: payload.size ?? payload.content.length,
            number: payload.number ?? 0
          };
        }
        return { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };
      }));
  }

  createQuestion(data: CreateQuestionRequest): Observable<QuestionResponse> {
    return this.http
      .post<ApiResponse<QuestionResponse>>(this.baseUrl, data)
      .pipe(map(res => res.data));
  }

  updateQuestion(id: string, data: CreateQuestionRequest): Observable<QuestionResponse> {
    return this.http
      .put<ApiResponse<QuestionResponse>>(`${this.baseUrl}/${id}`, data)
      .pipe(map(res => res.data));
  }

  submitForReview(id: string): Observable<QuestionResponse> {
    return this.http
      .put<ApiResponse<QuestionResponse>>(`${this.baseUrl}/${id}/submit`, {})
      .pipe(map(res => res.data));
  }

  getQuestionsForReview(page = 0, size = 20): Observable<PagedResponse<QuestionResponse>> {
    return this.getQuestions({ state: 'REVIEW', page, size });
  }

  approveQuestion(id: string): Observable<QuestionResponse> {
    return this.http
      .put<ApiResponse<QuestionResponse>>(`${this.baseUrl}/${id}/approve`, {})
      .pipe(map(res => res.data));
  }

  rejectQuestion(id: string, comments: string): Observable<QuestionResponse> {
    return this.http
      .put<ApiResponse<QuestionResponse>>(`${this.baseUrl}/${id}/reject`, { comments })
      .pipe(map(res => res.data));
  }
}
