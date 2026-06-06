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
  }): Observable<QuestionResponse[]> {
    let params = new HttpParams();
    if (filters) {
      if (filters.subject) params = params.set('subject', filters.subject);
      if (filters.topic) params = params.set('topic', filters.topic);
      if (filters.difficulty) params = params.set('difficulty', filters.difficulty);
      if (filters.state) params = params.set('state', filters.state);
    }
    return this.http
      .get<ApiResponse<QuestionResponse[]>>(this.baseUrl, { params })
      .pipe(map(res => res.data));
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
}
