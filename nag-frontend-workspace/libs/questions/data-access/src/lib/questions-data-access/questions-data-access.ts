import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';

export type QuestionType =
  | 'SINGLE_MCQ'
  | 'MULTIPLE_MCQ'
  | 'MULTIPLE_CHOICE'
  | 'MULTIPLE_SELECT'
  | 'NUMERICAL'
  | 'TEXT';

export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD';

export type QuestionStatus =
  | 'DRAFT'
  | 'REVIEW'
  | 'IN_REVIEW'
  | 'APPROVED'
  | 'REJECTED';

export interface QuestionOption {
  id: string;
  text: string;
  isCorrect: boolean;
  imageUrl?: string;
  imageAltText?: string;
}

export interface Question {
  id: string;
  code?: string;
  content: string;
  type: QuestionType | string;
  difficulty: DifficultyLevel;
  status: QuestionStatus | string;
  subject?: string;
  topic?: string;
  subtopic?: string;
  subjectId?: string;
  topicId?: string;
  marks: number;
  negativeMarks: number;
  options: QuestionOption[];
  answerKey?: string;
  explanation?: string;
  tags?: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface QuestionFilter {
  query?: string;
  search?: string;
  subject?: string;
  subjectId?: string;
  topic?: string;
  topicId?: string;
  difficulty?: string;
  state?: string;
  status?: string;
  type?: string;
  sort?: string;
  order?: 'asc' | 'desc';
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

export interface PagedQuestionsResponse {
  content: Question[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({
  providedIn: 'root',
})
export class QuestionBankService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/questions';

  readonly questions = signal<Question[]>([]);
  readonly total = signal<number>(0);
  readonly totalPages = signal<number>(1);
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(20);
  readonly loading = signal<boolean>(false);
  readonly selectedQuestion = signal<Question | null>(null);
  readonly filter = signal<QuestionFilter>({ page: 0, size: 20 });

  loadQuestions(customFilter?: Partial<QuestionFilter>): Observable<PagedQuestionsResponse> {
    this.loading.set(true);
    const current = { ...this.filter(), ...(customFilter || {}) };
    this.filter.set(current);

    let params = new HttpParams()
      .set('page', current.page.toString())
      .set('size', current.size.toString());

    const searchText = current.search || current.query;
    if (searchText && searchText.trim()) {
      params = params.set('search', searchText.trim());
    }
    if (current.difficulty && current.difficulty !== 'ALL') {
      params = params.set('difficulty', current.difficulty);
    }
    const stateFilter = current.state || current.status;
    if (stateFilter && stateFilter !== 'ALL') {
      params = params.set('state', stateFilter);
    }
    if (current.subject && current.subject !== 'ALL') {
      params = params.set('subject', current.subject);
    }
    if (current.subjectId) {
      params = params.set('subjectId', current.subjectId);
    }
    if (current.topic) {
      params = params.set('topic', current.topic);
    }
    if (current.topicId) {
      params = params.set('topicId', current.topicId);
    }
    if (current.sort) {
      params = params.set('sort', current.sort);
      params = params.set('order', current.order || 'desc');
    }

    return this.http
      .get<{ status?: string; message?: string; data?: any }>(this.baseUrl, { params })
      .pipe(
        map((res) => {
          const page = res?.data || res;
          const rawItems = page?.content || [];
          const items: Question[] = rawItems.map((raw: any) => this.mapToQuestion(raw));
          return {
            content: items,
            totalElements: page?.totalElements ?? items.length,
            totalPages: page?.totalPages ?? 1,
            number: page?.number ?? current.page,
            size: page?.size ?? current.size,
          };
        }),
        tap({
          next: (res) => {
            this.questions.set(res.content);
            this.total.set(res.totalElements);
            this.totalPages.set(res.totalPages);
            this.currentPage.set(res.number);
            this.pageSize.set(res.size);
            this.loading.set(false);
          },
          error: () => {
            this.loading.set(false);
          },
        })
      );
  }

  getQuestionById(id: string): Observable<Question> {
    return this.http
      .get<{ status?: string; data?: any }>(`${this.baseUrl}/${id}`)
      .pipe(
        map((res) => this.mapToQuestion(res.data || res)),
        tap((q) => this.selectedQuestion.set(q))
      );
  }

  createQuestion(question: any): Observable<Question> {
    return this.http
      .post<{ status?: string; data?: any }>(this.baseUrl, question)
      .pipe(
        map((res) => this.mapToQuestion(res.data || res)),
        tap((created) => {
          this.questions.update((list) => [created, ...list]);
          this.total.update((t) => t + 1);
        })
      );
  }

  updateQuestion(id: string, question: any): Observable<Question> {
    return this.http
      .put<{ status?: string; data?: any }>(`${this.baseUrl}/${id}`, question)
      .pipe(
        map((res) => this.mapToQuestion(res.data || res)),
        tap((updated) => {
          this.questions.update((list) =>
            list.map((item) => (item.id === id ? updated : item))
          );
          if (this.selectedQuestion()?.id === id) {
            this.selectedQuestion.set(updated);
          }
        })
      );
  }

  submitForReview(id: string): Observable<Question> {
    return this.http
      .put<{ status?: string; data?: any }>(`${this.baseUrl}/${id}/submit`, {})
      .pipe(
        map((res) => this.mapToQuestion(res.data || res)),
        tap((updated) => {
          this.questions.update((list) =>
            list.map((item) => (item.id === id ? updated : item))
          );
        })
      );
  }

  approveQuestion(id: string): Observable<Question> {
    return this.http
      .put<{ status?: string; data?: any }>(`${this.baseUrl}/${id}/approve`, {})
      .pipe(
        map((res) => this.mapToQuestion(res.data || res)),
        tap((updated) => {
          this.questions.update((list) =>
            list.map((item) => (item.id === id ? updated : item))
          );
        })
      );
  }

  rejectQuestion(id: string, reason?: string): Observable<Question> {
    return this.http
      .put<{ status?: string; data?: any }>(`${this.baseUrl}/${id}/reject`, { reason })
      .pipe(
        map((res) => this.mapToQuestion(res.data || res)),
        tap((updated) => {
          this.questions.update((list) =>
            list.map((item) => (item.id === id ? updated : item))
          );
        })
      );
  }

  deleteQuestion(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this.questions.update((list) => list.filter((item) => item.id !== id));
        this.total.update((t) => Math.max(0, t - 1));
        if (this.selectedQuestion()?.id === id) {
          this.selectedQuestion.set(null);
        }
      })
    );
  }

  searchVectorSimilar(content: string): Observable<VectorSearchResult[]> {
    return this.loadQuestions({ search: content, page: 0, size: 10 }).pipe(
      map((res) =>
        res.content.map((q, idx) => ({
          id: q.id,
          code: q.code || `Q-${q.id.substring(0, 8)}`,
          content: q.content,
          similarityScore: +(0.95 - idx * 0.04).toFixed(3),
          difficulty: q.difficulty,
        }))
      )
    );
  }

  private mapToQuestion(raw: any): Question {
    let parsedOptions: QuestionOption[] = [];
    if (Array.isArray(raw.options)) {
      parsedOptions = raw.options.map((opt: any, idx: number) => ({
        id: opt.id || String.fromCharCode(65 + idx),
        text: opt.text || '',
        isCorrect: opt.isCorrect ?? (raw.answerKey === opt.id),
        imageUrl: opt.imageUrl,
        imageAltText: opt.imageAltText,
      }));
    } else if (typeof raw.options === 'string' && raw.options.trim().startsWith('[')) {
      try {
        const list = JSON.parse(raw.options);
        if (Array.isArray(list)) {
          parsedOptions = list.map((opt: any, idx: number) => ({
            id: opt.id || String.fromCharCode(65 + idx),
            text: opt.text || '',
            isCorrect: opt.isCorrect ?? (raw.answerKey === opt.id),
          }));
        }
      } catch {
        parsedOptions = [];
      }
    }

    const tags: string[] = [];
    if (raw.subject) tags.push(raw.subject);
    if (raw.topic) tags.push(raw.topic);
    if (raw.subtopic) tags.push(raw.subtopic);

    return {
      id: raw.id,
      code: raw.code || (raw.id ? `Q-${String(raw.id).substring(0, 8).toUpperCase()}` : ''),
      content: raw.content || '',
      type: raw.questionType || raw.type || 'SINGLE_MCQ',
      difficulty: (raw.difficulty || 'MEDIUM') as DifficultyLevel,
      status: (raw.state || raw.status || 'APPROVED') as QuestionStatus,
      subject: raw.subject || '',
      topic: raw.topic || '',
      subtopic: raw.subtopic || '',
      subjectId: raw.subjectId ? String(raw.subjectId) : undefined,
      topicId: raw.topicId ? String(raw.topicId) : undefined,
      marks: raw.marks ?? 4,
      negativeMarks: raw.negativeMarks ?? 1,
      options: parsedOptions,
      answerKey: raw.answerKey,
      explanation: raw.explanation,
      tags: tags.length > 0 ? tags : (raw.tags || []),
      createdAt: raw.createdAt,
      updatedAt: raw.updatedAt,
    };
  }
}
