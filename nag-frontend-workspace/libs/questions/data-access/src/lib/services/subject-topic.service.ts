import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';
import {
  Subject,
  Topic,
  Subtopic,
  SubjectHierarchy,
  TargetExaminationOption,
  TaxonomySubjectNode,
  TaxonomyTopicNode,
  TaxonomySubtopicNode,
  TaxonomyOverrideRequest,
  MergeTaxonomyRequest,
  ReorderTaxonomyRequest,
} from '../models/taxonomy.model';
import { TaxonomyStore } from './taxonomy.store';


@Injectable({
  providedIn: 'root',
})
export class SubjectTopicService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/subjects';
  private readonly examBaseUrl = '/api/v1/examinations';

  getSubjects(): Observable<Subject[]> {
    return this.http
      .get<{ data?: Subject[] } | Subject[]>(this.baseUrl)
      .pipe(
        map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])),
        catchError(() =>
          this.getTaxonomyTree().pipe(
            map((tree) =>
              (tree || []).map((s) => ({
                id: s.id,
                name: s.name,
                code: s.code,
                description: s.description,
              }))
            )
          )
        )
      );
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
      .pipe(
        map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])),
        catchError(() =>
          this.getTaxonomyTree().pipe(
            map((tree) => {
              const sub = (tree || []).find((s) => s.id === subjectId);
              return (sub?.topics || []).map((t) => ({
                id: t.id,
                subjectId,
                name: t.name,
                description: t.description,
              }));
            })
          )
        )
      );
  }

  createTopic(subjectId: number, data: { name: string; description?: string }): Observable<Topic> {
    return this.http
      .post<{ data?: Topic } | Topic>(`${this.baseUrl}/${subjectId}/topics`, data)
      .pipe(map((res) => ((res as any)?.data || res) as Topic));
  }

  getSubtopics(subjectId: number, topicId: number): Observable<Subtopic[]> {
    return this.http
      .get<{ data?: Subtopic[] } | Subtopic[]>(`${this.baseUrl}/${subjectId}/topics/${topicId}/subtopics`)
      .pipe(
        map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])),
        catchError(() =>
          this.getTaxonomyTree().pipe(
            map((tree) => {
              const sub = (tree || []).find((s) => s.id === subjectId);
              const top = (sub?.topics || []).find((t) => t.id === topicId);
              return (top?.subtopics || []).map((st) => ({
                id: st.id,
                topicId,
                name: st.name,
                description: st.description,
              }));
            })
          )
        )
      );
  }

  createSubtopic(subjectId: number, topicId: number, data: { name: string; description?: string }): Observable<Subtopic> {
    return this.http
      .post<{ data?: Subtopic } | Subtopic>(`${this.baseUrl}/${subjectId}/topics/${topicId}/subtopics`, data)
      .pipe(map((res) => ((res as any)?.data || res) as Subtopic));
  }

  getHierarchy(): Observable<SubjectHierarchy[]> {
    return this.getTaxonomyTree().pipe(
      map((tree) => {
        if (!tree || tree.length === 0) return [];
        return tree.map((s) => ({
          id: s.id,
          name: s.name,
          code: s.code,
          description: s.description,
          questionCount: s.questionCount,
          topics: (s.topics || []).map((t) => ({
            id: t.id,
            name: t.name,
            description: t.description,
            questionCount: t.questionCount,
            subtopics: (t.subtopics || []).map((st) => ({
              id: st.id,
              name: st.name,
              description: st.description,
              questionCount: st.questionCount,
            })),
          })),
        }));
      }),
      catchError(() =>
        this.http
          .get<{ data?: SubjectHierarchy[] } | SubjectHierarchy[]>(`${this.baseUrl}/hierarchy`)
          .pipe(map((res) => (Array.isArray(res) ? res : (res as any)?.data || [])))
      )
    );
  }

  // ── Unified Exam Spine Taxonomy Operations ─────────────────────────────

  private readonly taxonomyStore = inject(TaxonomyStore);

  /** Signal of effective taxonomy tree from store */
  readonly taxonomyTree = this.taxonomyStore.tree;
  readonly taxonomyLoading = this.taxonomyStore.loading;

  getTaxonomyTree(examId?: string): Observable<TaxonomySubjectNode[]> {
    return this.taxonomyStore.loadTree(examId);
  }

  applyOverrides(requests: TaxonomyOverrideRequest[]): Observable<void> {
    return this.taxonomyStore.applyOverrides(requests);
  }

  mergeNodes(request: MergeTaxonomyRequest): Observable<void> {
    return this.taxonomyStore.mergeNodes(request);
  }

  reorderNodes(request: ReorderTaxonomyRequest): Observable<void> {
    return this.taxonomyStore.reorderNodes(request);
  }

  nodeName(type: 'SUBJECT' | 'TOPIC' | 'SUBTOPIC' | string, id: number | null | undefined): string {
    return this.taxonomyStore.nodeName(type, id);
  }

  subjectName(id: number | null | undefined): string {
    return this.taxonomyStore.subjectName(id);
  }

  topicName(id: number | null | undefined): string {
    return this.taxonomyStore.topicName(id);
  }

  subtopicName(id: number | null | undefined): string {
    return this.taxonomyStore.subtopicName(id);
  }

  getTopicsForSubject(subjectId: number | null | undefined): TaxonomyTopicNode[] {
    return this.taxonomyStore.getTopicsForSubject(subjectId);
  }

  getSubtopicsForTopic(topicId: number | null | undefined): TaxonomySubtopicNode[] {
    return this.taxonomyStore.getSubtopicsForTopic(topicId);
  }

  /**
   * Fetches target examinations dynamically from the examination database/service (/api/v1/examinations).
   * Falls back gracefully to /api/v1/examinations/public if necessary.
   */
  getExaminations(page: number = 0, size: number = 100): Observable<TargetExaminationOption[]> {
    return this.http
      .get<any>(this.examBaseUrl, {
        params: { page: page.toString(), size: size.toString() },
      })
      .pipe(
        map((res) => {
          const payload = res?.data ?? res;
          const items: any[] = Array.isArray(payload?.content)
            ? payload.content
            : Array.isArray(payload)
            ? payload
            : [];
          return items.map((e) => ({
            id: String(e.id),
            name: e.name,
            code: e.code || e.name,
            category: e.category,
            examinationType: e.examinationType,
          }));
        }),
        catchError(() =>
          this.http
            .get<any>(`${this.examBaseUrl}/public`, {
              params: { page: page.toString(), size: size.toString() },
            })
            .pipe(
              map((res) => {
                const payload = res?.data ?? res;
                const items: any[] = Array.isArray(payload?.content)
                  ? payload.content
                  : Array.isArray(payload)
                  ? payload
                  : [];
                return items.map((e) => ({
                  id: String(e.id),
                  name: e.name,
                  code: e.code || e.name,
                  category: e.category,
                  examinationType: e.examinationType,
                }));
              }),
              catchError(() => of([]))
            )
        )
      );
  }

  /**
   * Alias for getExaminations for semantic clarity.
   */
  getTargetExaminations(page: number = 0, size: number = 100): Observable<TargetExaminationOption[]> {
    return this.getExaminations(page, size);
  }
}

