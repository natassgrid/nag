import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export type QuestionType =
  | 'MULTIPLE_CHOICE'
  | 'MULTIPLE_SELECT'
  | 'NUMERICAL'
  | 'TEXT';

export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD';

export type QuestionStatus =
  | 'DRAFT'
  | 'IN_REVIEW'
  | 'APPROVED'
  | 'REJECTED';

export interface QuestionOption {
  id: string;
  text: string;
  isCorrect: boolean;
}

export interface Question {
  id: string;
  code: string;
  content: string;
  type: QuestionType;
  difficulty: DifficultyLevel;
  status: QuestionStatus;
  subjectId?: string;
  topicId?: string;
  marks: number;
  negativeMarks: number;
  options: QuestionOption[];
  explanation?: string;
  tags?: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface QuestionFilter {
  query?: string;
  subjectId?: string;
  topicId?: string;
  difficulty?: string;
  type?: string;
  page: number;
  size: number;
}

export interface VectorSearchResult {
  id: string;
  code: string;
  content: string;
  similarityScore: number;
  difficulty: DifficultyLevel;
}

@Injectable({
  providedIn: 'root',
})
export class QuestionBankService {
  private readonly http = inject(HttpClient);

  readonly questions = signal<Question[]>([]);
  readonly total = signal<number>(0);
  readonly loading = signal<boolean>(false);
  readonly selectedQuestion = signal<Question | null>(null);
  readonly filter = signal<QuestionFilter>({ page: 0, size: 20 });

  loadQuestions(customFilter?: Partial<QuestionFilter>): Observable<{ content: Question[]; totalElements: number }> {
    this.loading.set(true);
    const current = { ...this.filter(), ...(customFilter || {}) };
    this.filter.set(current);

    let params = new HttpParams()
      .set('page', current.page.toString())
      .set('size', current.size.toString());

    if (current.query) params = params.set('search', current.query);
    if (current.difficulty) params = params.set('difficulty', current.difficulty);
    if (current.type) params = params.set('type', current.type);
    if (current.subjectId) params = params.set('subjectId', current.subjectId);

    return this.http
      .get<{ content: Question[]; totalElements: number }>('/api/v1/question-bank/questions', { params })
      .pipe(
        tap({
          next: (res) => {
            this.questions.set(res.content || []);
            this.total.set(res.totalElements || 0);
            this.loading.set(false);
          },
          error: () => {
            this.loading.set(false);
          },
        })
      );
  }

  getQuestionById(id: string): Observable<Question> {
    return this.http.get<Question>(`/api/v1/question-bank/questions/${id}`).pipe(
      tap((q) => this.selectedQuestion.set(q))
    );
  }

  createQuestion(question: Partial<Question>): Observable<Question> {
    return this.http.post<Question>('/api/v1/question-bank/questions', question).pipe(
      tap((created) => {
        this.questions.update((list) => [created, ...list]);
        this.total.update((t) => t + 1);
      })
    );
  }

  updateQuestion(id: string, question: Partial<Question>): Observable<Question> {
    return this.http.put<Question>(`/api/v1/question-bank/questions/${id}`, question).pipe(
      tap((updated) => {
        this.questions.update((list) => list.map((item) => (item.id === id ? updated : item)));
        if (this.selectedQuestion()?.id === id) {
          this.selectedQuestion.set(updated);
        }
      })
    );
  }

  deleteQuestion(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/question-bank/questions/${id}`).pipe(
      tap(() => {
        this.questions.update((list) => list.filter((item) => item.id !== id));
        this.total.update((t) => Math.max(0, t - 1));
        if (this.selectedQuestion()?.id === id) {
          this.selectedQuestion.set(null);
        }
      })
    );
  }

  searchVectorSimilar(content: string, threshold = 0.8): Observable<VectorSearchResult[]> {
    return this.http.post<VectorSearchResult[]>('/api/v1/question-bank/search/vector', {
      content,
      similarityThreshold: threshold,
    });
  }
}
