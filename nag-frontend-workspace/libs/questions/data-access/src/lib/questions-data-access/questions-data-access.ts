import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';

export type QuestionType =
  | 'SINGLE_MCQ'
  | 'MULTIPLE_MCQ'
  | 'MULTI_MCQ'
  | 'MULTIPLE_CHOICE'
  | 'MULTIPLE_SELECT'
  | 'NUMERICAL'
  | 'TEXT'
  | 'DESCRIPTIVE'
  | 'PARAGRAPH_SET';

export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD' | 'EXPERT';

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
  subjectId?: string | number;
  topicId?: string | number;
  subtopicId?: string | number;
  marks: number;
  negativeMarks: number;
  options: QuestionOption[];
  answerKey?: string;
  explanation?: string;
  tags?: string[];
  passageId?: string;
  passageOrderIndex?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface QuestionFilter {
  query?: string;
  search?: string;
  subject?: string;
  subjectId?: string | number;
  topic?: string;
  topicId?: string | number;
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

// ============================================================================
// 1. SUBJECT & TOPIC TAXONOMY MODELS & SERVICE
// ============================================================================

export interface Subject {
  id: number;
  name: string;
  code?: string;
  description?: string;
  topicCount?: number;
  questionCount?: number;
}

export interface Topic {
  id: number;
  subjectId: number;
  name: string;
  description?: string;
  subtopicCount?: number;
  questionCount?: number;
}

export interface Subtopic {
  id: number;
  topicId: number;
  name: string;
  description?: string;
  questionCount?: number;
}

export interface SubtopicNode {
  id: number;
  name: string;
  description?: string;
  questionCount?: number;
}

export interface TopicNode {
  id: number;
  name: string;
  description?: string;
  questionCount?: number;
  subtopics: SubtopicNode[];
}

export interface SubjectHierarchy {
  id: number;
  name: string;
  code?: string;
  description?: string;
  questionCount?: number;
  topics: TopicNode[];
}

@Injectable({
  providedIn: 'root',
})
export class SubjectTopicService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/subjects';

  getSubjects(): Observable<Subject[]> {
    return this.http
      .get<{ data?: Subject[] } | Subject[]>(this.baseUrl)
      .pipe(map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])));
  }

  getSubject(id: number): Observable<Subject> {
    return this.http
      .get<{ data?: Subject } | Subject>(`${this.baseUrl}/${id}`)
      .pipe(map((res) => ((res as any)?.data || res) as Subject));
  }

  createSubject(data: { name: string; code?: string; description?: string }): Observable<Subject> {
    return this.http
      .post<{ data?: Subject } | Subject>(this.baseUrl, data)
      .pipe(map((res) => ((res as any)?.data || res) as Subject));
  }

  updateSubject(id: number, data: { name: string; code?: string; description?: string }): Observable<Subject> {
    return this.http
      .put<{ data?: Subject } | Subject>(`${this.baseUrl}/${id}`, data)
      .pipe(map((res) => ((res as any)?.data || res) as Subject));
  }

  deleteSubject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getTopics(subjectId: number): Observable<Topic[]> {
    return this.http
      .get<{ data?: Topic[] } | Topic[]>(`${this.baseUrl}/${subjectId}/topics`)
      .pipe(map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])));
  }

  createTopic(subjectId: number, data: { name: string; description?: string }): Observable<Topic> {
    return this.http
      .post<{ data?: Topic } | Topic>(`${this.baseUrl}/${subjectId}/topics`, data)
      .pipe(map((res) => ((res as any)?.data || res) as Topic));
  }

  getSubtopics(subjectId: number, topicId: number): Observable<Subtopic[]> {
    return this.http
      .get<{ data?: Subtopic[] } | Subtopic[]>(`${this.baseUrl}/${subjectId}/topics/${topicId}/subtopics`)
      .pipe(map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])));
  }

  createSubtopic(subjectId: number, topicId: number, data: { name: string; description?: string }): Observable<Subtopic> {
    return this.http
      .post<{ data?: Subtopic } | Subtopic>(`${this.baseUrl}/${subjectId}/topics/${topicId}/subtopics`, data)
      .pipe(map((res) => ((res as any)?.data || res) as Subtopic));
  }

  getHierarchy(): Observable<SubjectHierarchy[]> {
    return this.http
      .get<{ data?: SubjectHierarchy[] } | SubjectHierarchy[]>(`${this.baseUrl}/hierarchy`)
      .pipe(map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])));
  }
}

// ============================================================================
// 2. PASSAGE & COMPREHENSION SET MODELS & SERVICE
// ============================================================================

export interface SubQuestionRequest {
  id?: string;
  passageOrderIndex?: number;
  content: string;
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD' | 'EXPERT' | string;
  cognitiveLevel?: 'REMEMBER' | 'UNDERSTAND' | 'APPLY' | 'ANALYZE' | 'EVALUATE' | 'CREATE' | string;
  questionType?: QuestionType | string;
  options: QuestionOption[];
  answerKey?: string;
  explanation?: string;
  marks?: number;
  negativeMarks?: number;
}

export interface PassageRequest {
  title?: string;
  content: string;
  contentFormat?: string;
  subjectId: number;
  topicId?: number;
  subject?: string;
  topic?: string;
  subtopic?: string;
  hasImages?: boolean;
  subQuestions: SubQuestionRequest[];
}

export interface PassageResponse {
  id: string;
  title?: string;
  content: string;
  contentFormat?: string;
  subjectId: number;
  topicId?: number;
  subject?: string;
  topic?: string;
  subtopic?: string;
  hasImages?: boolean;
  state: string;
  authorId?: string;
  reviewerId?: string;
  subQuestions: Question[];
  subQuestionCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PagedPassagesResponse {
  content: PassageResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({
  providedIn: 'root',
})
export class PassageService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/passages';

  readonly passages = signal<PassageResponse[]>([]);
  readonly total = signal<number>(0);
  readonly loading = signal<boolean>(false);

  listPassages(filters?: {
    subjectId?: number;
    state?: string;
    search?: string;
    page?: number;
    size?: number;
  }): Observable<PagedPassagesResponse> {
    this.loading.set(true);
    let params = new HttpParams()
      .set('page', (filters?.page ?? 0).toString())
      .set('size', (filters?.size ?? 20).toString());

    if (filters?.subjectId) params = params.set('subjectId', filters.subjectId.toString());
    if (filters?.state && filters.state !== 'ALL') params = params.set('state', filters.state);
    if (filters?.search && filters.search.trim()) params = params.set('search', filters.search.trim());

    return this.http
      .get<{ status?: string; data?: any }>(this.baseUrl, { params })
      .pipe(
        map((res) => {
          const page = res?.data || res;
          const rawItems = Array.isArray(page?.content) ? page.content : (Array.isArray(page) ? page : []);
          const items: PassageResponse[] = rawItems.map((raw: any) => this.mapToPassage(raw));
          return {
            content: items,
            totalElements: page?.totalElements ?? items.length,
            totalPages: page?.totalPages ?? 1,
            number: page?.number ?? 0,
            size: page?.size ?? 20,
          };
        }),
        tap({
          next: (res) => {
            this.passages.set(res.content);
            this.total.set(res.totalElements);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        })
      );
  }

  getPassage(id: string): Observable<PassageResponse> {
    return this.http
      .get<{ status?: string; data?: any }>(`${this.baseUrl}/${id}`)
      .pipe(map((res) => this.mapToPassage(res?.data || res)));
  }

  createPassage(data: PassageRequest): Observable<PassageResponse> {
    return this.http
      .post<{ status?: string; data?: any }>(this.baseUrl, data)
      .pipe(
        map((res) => this.mapToPassage(res?.data || res)),
        tap((created) => {
          this.passages.update((list) => [created, ...list]);
          this.total.update((t) => t + 1);
        })
      );
  }

  updatePassage(id: string, data: PassageRequest): Observable<PassageResponse> {
    return this.http
      .put<{ status?: string; data?: any }>(`${this.baseUrl}/${id}`, data)
      .pipe(
        map((res) => this.mapToPassage(res?.data || res)),
        tap((updated) => {
          this.passages.update((list) =>
            list.map((item) => (item.id === id ? updated : item))
          );
        })
      );
  }

  deletePassage(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this.passages.update((list) => list.filter((p) => p.id !== id));
        this.total.update((t) => Math.max(0, t - 1));
      })
    );
  }

  submitForReview(id: string): Observable<PassageResponse> {
    return this.http
      .put<{ status?: string; data?: any }>(`${this.baseUrl}/${id}/submit`, {})
      .pipe(
        map((res) => this.mapToPassage(res?.data || res)),
        tap((updated) => {
          this.passages.update((list) =>
            list.map((item) => (item.id === id ? updated : item))
          );
        })
      );
  }

  approvePassage(id: string): Observable<PassageResponse> {
    return this.http
      .put<{ status?: string; data?: any }>(`${this.baseUrl}/${id}/approve`, {})
      .pipe(
        map((res) => this.mapToPassage(res?.data || res)),
        tap((updated) => {
          this.passages.update((list) =>
            list.map((item) => (item.id === id ? updated : item))
          );
        })
      );
  }

  rejectPassage(id: string, comments?: string): Observable<PassageResponse> {
    return this.http
      .put<{ status?: string; data?: any }>(`${this.baseUrl}/${id}/reject`, { comments })
      .pipe(
        map((res) => this.mapToPassage(res?.data || res)),
        tap((updated) => {
          this.passages.update((list) =>
            list.map((item) => (item.id === id ? updated : item))
          );
        })
      );
  }

  private mapToPassage(raw: any): PassageResponse {
    const rawSubs = raw?.subQuestions || [];
    const subQuestions: Question[] = rawSubs.map((sub: any, idx: number) => {
      let options: QuestionOption[] = [];
      if (Array.isArray(sub.options)) {
        options = sub.options.map((opt: any, oIdx: number) => ({
          id: opt.id || String.fromCharCode(65 + oIdx),
          text: opt.text || '',
          isCorrect: opt.isCorrect ?? (sub.answerKey === opt.id),
          imageUrl: opt.imageUrl,
          imageAltText: opt.imageAltText,
        }));
      }

      return {
        id: sub.id || `SQ-${idx + 1}`,
        content: sub.content || '',
        type: sub.questionType || sub.type || 'SINGLE_MCQ',
        difficulty: (sub.difficulty || 'MEDIUM') as DifficultyLevel,
        status: (sub.state || sub.status || raw.state || 'DRAFT') as QuestionStatus,
        subject: raw.subject || '',
        topic: raw.topic || '',
        subtopic: raw.subtopic || '',
        subjectId: raw.subjectId,
        topicId: raw.topicId,
        marks: sub.marks ?? 4,
        negativeMarks: sub.negativeMarks ?? 1,
        options,
        answerKey: sub.answerKey,
        explanation: sub.explanation,
        passageId: raw.id,
        passageOrderIndex: sub.passageOrderIndex ?? (idx + 1),
      };
    });

    return {
      id: raw.id,
      title: raw.title || '',
      content: raw.content || '',
      contentFormat: raw.contentFormat || 'MIXED',
      subjectId: raw.subjectId,
      topicId: raw.topicId,
      subject: raw.subject || '',
      topic: raw.topic || '',
      subtopic: raw.subtopic || '',
      hasImages: !!raw.hasImages,
      state: raw.state || 'DRAFT',
      authorId: raw.authorId,
      reviewerId: raw.reviewerId,
      subQuestions,
      subQuestionCount: raw.subQuestionCount ?? subQuestions.length,
      createdAt: raw.createdAt,
      updatedAt: raw.updatedAt,
    };
  }
}

// ============================================================================
// 3. MAIN QUESTION BANK SERVICE
// ============================================================================

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
      params = params.set('subjectId', current.subjectId.toString());
    }
    if (current.topic && current.topic !== 'ALL') {
      params = params.set('topic', current.topic);
    }
    if (current.topicId) {
      params = params.set('topicId', current.topicId.toString());
    }
    if (current.type && current.type !== 'ALL') {
      params = params.set('type', current.type);
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
          const rawItems = Array.isArray(page?.content) ? page.content : (Array.isArray(page) ? page : []);
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
      subtopicId: raw.subtopicId ? String(raw.subtopicId) : undefined,
      marks: raw.marks ?? 4,
      negativeMarks: raw.negativeMarks ?? 1,
      options: parsedOptions,
      answerKey: raw.answerKey,
      explanation: raw.explanation,
      tags: tags.length > 0 ? tags : (raw.tags || []),
      passageId: raw.passageId,
      passageOrderIndex: raw.passageOrderIndex,
      createdAt: raw.createdAt,
      updatedAt: raw.updatedAt,
    };
  }
}

// ============================================================================
// 4. RULE BLUEPRINT MODELS & SERVICE
// ============================================================================

export interface BlueprintRule {
  id?: string;
  subject: string;
  topic?: string;
  subtopic?: string;
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD' | string;
  cognitiveLevel?: string;
  questionType?: string;
  questionCount?: number;
  targetCount?: number;
  marksPerQuestion?: number;
  negativeMarks?: number;
}

export interface BlueprintTemplateRequest {
  name: string;
  description?: string;
  examId?: string;
  rules: BlueprintRule[];
}

export interface BlueprintTemplateResponse {
  id: string;
  name: string;
  description?: string;
  examId?: string;
  examName?: string;
  rules: BlueprintRule[];
  totalQuestions?: number;
  totalMarks?: number;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface RuleFeasibilityDetail {
  subject: string;
  topic?: string;
  difficulty?: string;
  cognitiveLevel?: string;
  requested?: number;
  targetCount?: number;
  questionCount?: number;
  available?: number;
  needed?: number;
  deficit?: number;
  surplus?: number;
  sufficient?: boolean;
  status?: string;
}

export interface BlueprintFeasibilityResponse {
  feasible: boolean;
  totalRequested?: number;
  totalAvailable?: number;
  totalQuestionsNeeded?: number;
  totalQuestionsAvailable?: number;
  deficitRuleCount?: number;
  summary?: string;
  checkedAt?: string;
  rules?: RuleFeasibilityDetail[];
  ruleDetails?: RuleFeasibilityDetail[];
  insufficientRules?: RuleFeasibilityDetail[];
  overallSufficiency?: number;
}

@Injectable({
  providedIn: 'root',
})
export class BlueprintTemplateService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/papers/blueprint-templates';

  listTemplates(examId?: string): Observable<BlueprintTemplateResponse[]> {
    let params = new HttpParams();
    if (examId) {
      params = params.set('examId', examId);
    }
    return this.http
      .get<{ data?: BlueprintTemplateResponse[] } | BlueprintTemplateResponse[]>(this.baseUrl, { params })
      .pipe(map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])));
  }

  getTemplate(id: string): Observable<BlueprintTemplateResponse> {
    return this.http
      .get<{ data?: BlueprintTemplateResponse } | BlueprintTemplateResponse>(`${this.baseUrl}/${id}`)
      .pipe(map((res) => ((res as any).data || res) as BlueprintTemplateResponse));
  }

  createTemplate(req: BlueprintTemplateRequest): Observable<BlueprintTemplateResponse> {
    return this.http
      .post<{ data?: BlueprintTemplateResponse } | BlueprintTemplateResponse>(this.baseUrl, req)
      .pipe(map((res) => ((res as any).data || res) as BlueprintTemplateResponse));
  }

  updateTemplate(id: string, req: BlueprintTemplateRequest): Observable<BlueprintTemplateResponse> {
    return this.http
      .put<{ data?: BlueprintTemplateResponse } | BlueprintTemplateResponse>(`${this.baseUrl}/${id}`, req)
      .pipe(map((res) => ((res as any).data || res) as BlueprintTemplateResponse));
  }

  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  checkSufficiency(id: string, notifyAdmin = false): Observable<BlueprintFeasibilityResponse> {
    const params = new HttpParams().set('notifyAdmin', notifyAdmin.toString());
    return this.http
      .post<{ data?: BlueprintFeasibilityResponse } | BlueprintFeasibilityResponse>(
        `${this.baseUrl}/${id}/check-sufficiency`,
        {},
        { params }
      )
      .pipe(map((res) => ((res as any).data || res) as BlueprintFeasibilityResponse));
  }
}

// ============================================================================
// 5. QUESTION TRANSLATION & INDIC AI MODELS & SERVICE
// ============================================================================

export type TranslationStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'REJECTED' | 'STALE';

export interface SupportedLanguage {
  code: string;
  name: string;
  nativeName: string;
  script: string;
}

export const SUPPORTED_LANGUAGES: SupportedLanguage[] = [
  { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी', script: 'Devanagari' },
  { code: 'bn', name: 'Bengali', nativeName: 'বাংলা', script: 'Bengali' },
  { code: 'te', name: 'Telugu', nativeName: 'తెలుగు', script: 'Telugu' },
  { code: 'mr', name: 'Marathi', nativeName: 'मराठी', script: 'Devanagari' },
  { code: 'ta', name: 'Tamil', nativeName: 'தமிழ்', script: 'Tamil' },
  { code: 'gu', name: 'Gujarati', nativeName: 'ગુજરાતી', script: 'Gujarati' },
  { code: 'kn', name: 'Kannada', nativeName: 'ಕನ್ನಡ', script: 'Kannada' },
  { code: 'ml', name: 'Malayalam', nativeName: 'മലയാളം', script: 'Malayalam' },
  { code: 'or', name: 'Odia', nativeName: 'ଓଡ଼ିଆ', script: 'Odia' },
  { code: 'pa', name: 'Punjabi', nativeName: 'ਪੰਜਾਬੀ', script: 'Gurmukhi' },
  { code: 'as', name: 'Assamese', nativeName: 'অসমীয়া', script: 'Bengali' },
  { code: 'ur', name: 'Urdu', nativeName: 'اردو', script: 'Perso-Arabic' },
  { code: 'sa', name: 'Sanskrit', nativeName: 'संस्कृतम्', script: 'Devanagari' },
  { code: 'en', name: 'English', nativeName: 'English', script: 'Latin' },
];

export interface TranslatedOptionDto {
  id: string;
  text: string;
  imageUrl?: string;
  imageAltText?: string;
}

export interface TranslationRequest {
  questionId: string;
  languageCode: string;
  translatorId?: string;
  translatedContent: string;
  translatedOptions?: TranslatedOptionDto[];
  translatedExplanation?: string;
}

export interface TranslationResponse {
  translationId?: string;
  id?: string;
  questionId: string;
  languageCode: string;
  translatedContent: string;
  translatedOptions?: TranslatedOptionDto[];
  translatedExplanation?: string;
  sourceVersion?: number;
  status: TranslationStatus;
  translatorId?: string;
  reviewerId?: string;
  reviewComments?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AutoTranslateResponse {
  questionId: string;
  languageCode?: string;
  language?: string;
  targetLangIndicTrans?: string;
  translatedContent: string;
  translatedOptions?: TranslatedOptionDto[];
  translatedExplanation?: string;
  model?: string;
  confidenceScore?: number;
}

export interface BatchTranslationRequest {
  sourceLanguage?: string;
  targetLanguage?: string;
  targetStatus?: string;
  subject?: string;
  overwriteExisting?: boolean;
  batchSize?: number;
  throttleDelayMs?: number;
  maxConcurrency?: number;
}

export type BatchJobStatus = 'PENDING' | 'IN_PROGRESS' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface BatchTranslationJobResponse {
  id: string;
  jobId?: string;
  tenantId?: string;
  status: BatchJobStatus;
  sourceLanguage: string;
  targetLanguage: string;
  targetStatus?: string;
  subjectFilter?: string;
  overwriteExisting?: boolean;
  totalQuestions: number;
  processedQuestions: number;
  successfulQuestions: number;
  translatedCount?: number;
  failedQuestions: number;
  progressPercentage: number;
  failedQuestionIds?: string[];
  batchSize?: number;
  initiatedBy?: string;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({
  providedIn: 'root',
})
export class TranslationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/translations';

  getLanguage(code: string): SupportedLanguage | undefined {
    return SUPPORTED_LANGUAGES.find(
      (l) => l.code.toLowerCase() === (code || '').toLowerCase()
    );
  }

  listTranslationsForQuestion(questionId: string): Observable<TranslationResponse[]> {
    return this.http
      .get<{ data?: TranslationResponse[] } | TranslationResponse[]>(`${this.baseUrl}/question/${questionId}`)
      .pipe(map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])));
  }

  getApprovedTranslation(questionId: string, lang: string): Observable<TranslationResponse> {
    return this.http
      .get<{ data?: TranslationResponse } | TranslationResponse>(
        `${this.baseUrl}/question/${questionId}/language/${lang}`
      )
      .pipe(map((res) => ((res as any).data || res) as TranslationResponse));
  }

  autoTranslateQuestion(questionId: string, languageCode: string): Observable<AutoTranslateResponse> {
    return this.http
      .post<{ data?: AutoTranslateResponse } | AutoTranslateResponse>(
        `${this.baseUrl}/question/${questionId}/auto-translate/${languageCode}`,
        {}
      )
      .pipe(map((res) => ((res as any).data || res) as AutoTranslateResponse));
  }

  startBatchTranslation(request?: BatchTranslationRequest): Observable<BatchTranslationJobResponse> {
    return this.http
      .post<{ data?: BatchTranslationJobResponse } | BatchTranslationJobResponse>(
        `${this.baseUrl}/batch/auto-translate`,
        request || { sourceLanguage: 'en', targetLanguage: 'hi' }
      )
      .pipe(map((res) => ((res as any).data || res) as BatchTranslationJobResponse));
  }

  getBatchJobStatus(jobId: string): Observable<BatchTranslationJobResponse> {
    return this.http
      .get<{ data?: BatchTranslationJobResponse } | BatchTranslationJobResponse>(
        `${this.baseUrl}/batch/${jobId}`
      )
      .pipe(map((res) => ((res as any).data || res) as BatchTranslationJobResponse));
  }

  listBatchJobs(): Observable<BatchTranslationJobResponse[]> {
    return this.http
      .get<{ data?: BatchTranslationJobResponse[] } | BatchTranslationJobResponse[]>(`${this.baseUrl}/batch`)
      .pipe(map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])));
  }

  cancelBatchJob(jobId: string): Observable<BatchTranslationJobResponse> {
    return this.http
      .post<{ data?: BatchTranslationJobResponse } | BatchTranslationJobResponse>(
        `${this.baseUrl}/batch/${jobId}/cancel`,
        {}
      )
      .pipe(map((res) => ((res as any).data || res) as BatchTranslationJobResponse));
  }

  saveTranslation(request: TranslationRequest): Observable<TranslationResponse> {
    return this.http
      .post<{ data?: TranslationResponse } | TranslationResponse>(this.baseUrl, request)
      .pipe(map((res) => ((res as any).data || res) as TranslationResponse));
  }

  approveTranslation(translationId: string): Observable<TranslationResponse> {
    return this.http
      .post<{ data?: TranslationResponse } | TranslationResponse>(
        `${this.baseUrl}/${translationId}/approve`,
        {}
      )
      .pipe(map((res) => ((res as any).data || res) as TranslationResponse));
  }

  rejectTranslation(translationId: string, comments?: string): Observable<TranslationResponse> {
    return this.http
      .post<{ data?: TranslationResponse } | TranslationResponse>(
        `${this.baseUrl}/${translationId}/reject`,
        { comments: comments || 'Changes requested' }
      )
      .pipe(map((res) => ((res as any).data || res) as TranslationResponse));
  }
}

// ============================================================================
// 6. AI QUESTION GENERATION MODELS & SERVICE (LiteLLM / Bedrock / RAG)
// ============================================================================

export interface ParagraphSetConfig {
  passageWordLength?: number;
  subQuestionCount?: number;
  passageTheme?: string;
}

export interface QuestionGenerationRequest {
  subject: string;
  topic: string;
  subtopic?: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD' | string;
  cognitiveLevel:
    | 'REMEMBER'
    | 'UNDERSTAND'
    | 'APPLY'
    | 'ANALYZE'
    | 'EVALUATE'
    | 'CREATE'
    | string;
  questionType:
    | 'SINGLE_MCQ'
    | 'MULTI_MCQ'
    | 'NUMERICAL'
    | 'DESCRIPTIVE'
    | 'PARAGRAPH_SET'
    | string;
  generationType?: 'STANDALONE' | 'PARAGRAPH_SET' | string;
  paragraphSetConfig?: ParagraphSetConfig;
  count: number;
  avoidDuplicate?: boolean;
  autoSave?: boolean;
}

export interface GeneratedQuestionValidation {
  valid: boolean;
  errors: string[];
}

export interface GeneratedQuestionDuplicate {
  similarQuestionId?: string;
  similarity: number;
}

export interface GeneratedQuestion {
  content: string;
  answerKey?: string;
  explanation?: string;
  options?: QuestionOption[];
  difficulty: string;
  cognitiveLevel: string;
  questionType: string;
  validation?: GeneratedQuestionValidation;
  duplicate?: GeneratedQuestionDuplicate;
  savedQuestionId?: string;
}

export interface QuestionGenerationResponse {
  questions: GeneratedQuestion[];
  modelUsed: string;
  totalGenerated: number;
  totalValid: number;
  totalDuplicates: number;
}

export interface BatchItem {
  subject: string;
  topic: string;
  subtopic?: string;
  difficulty: string;
  cognitiveLevel: string;
  questionType: string;
  count: number;
}

export interface BatchGenerationRequest {
  items: BatchItem[];
  avoidDuplicates?: boolean;
}

export interface BatchGenerationJob {
  id: string;
  status:
    | 'PENDING'
    | 'PROCESSING'
    | 'COMPLETED'
    | 'PARTIALLY_COMPLETED'
    | 'FAILED'
    | 'CANCELLED'
    | string;
  items?: string | BatchItem[];
  totalRequested: number;
  totalGenerated: number;
  totalFailed: number;
  totalDuplicates: number;
  modelUsed?: string;
  initiatedBy?: string;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
  progress?: number;
}

@Injectable({
  providedIn: 'root',
})
export class QuestionAiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/questions';

  generateQuestions(request: QuestionGenerationRequest): Observable<QuestionGenerationResponse> {
    return this.http
      .post<{ status?: string; message?: string; data?: QuestionGenerationResponse } | QuestionGenerationResponse>(
        `${this.baseUrl}/generate`,
        request
      )
      .pipe(map((res) => ((res as any)?.data || res) as QuestionGenerationResponse));
  }

  submitBatchJob(request: BatchGenerationRequest): Observable<BatchGenerationJob> {
    return this.http
      .post<{ status?: string; message?: string; data?: BatchGenerationJob } | BatchGenerationJob>(
        `${this.baseUrl}/batch`,
        request
      )
      .pipe(map((res) => ((res as any)?.data || res) as BatchGenerationJob));
  }

  getBatchJobStatus(jobId: string): Observable<BatchGenerationJob> {
    return this.http
      .get<{ status?: string; message?: string; data?: BatchGenerationJob } | BatchGenerationJob>(
        `${this.baseUrl}/batch/${jobId}`
      )
      .pipe(map((res) => ((res as any)?.data || res) as BatchGenerationJob));
  }

  listBatchJobs(
    page = 0,
    size = 20
  ): Observable<{ content: BatchGenerationJob[]; totalElements: number }> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http
      .get<{ status?: string; message?: string; data?: any }>(`${this.baseUrl}/batch`, { params })
      .pipe(
        map((res) => {
          const p = res?.data || res;
          return {
            content: p?.content || [],
            totalElements: p?.totalElements || 0,
          };
        })
      );
  }

  cancelBatchJob(jobId: string): Observable<BatchGenerationJob> {
    return this.http
      .post<{ status?: string; message?: string; data?: BatchGenerationJob } | BatchGenerationJob>(
        `${this.baseUrl}/batch/${jobId}/cancel`,
        {}
      )
      .pipe(map((res) => ((res as any)?.data || res) as BatchGenerationJob));
  }

  backfillEmbeddings(): Observable<any> {
    return this.http.post(`${this.baseUrl}/embeddings/backfill`, {});
  }
}
