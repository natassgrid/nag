import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap, catchError, throwError, map } from 'rxjs';
import {
  TaxonomySubjectNode,
  TaxonomyTopicNode,
  TaxonomySubtopicNode,
  TaxonomyOverrideRequest,
  MergeTaxonomyRequest,
  ReorderTaxonomyRequest,
} from '../models/taxonomy.model';

@Injectable({
  providedIn: 'root',
})
export class TaxonomyStore {
  private readonly http = inject(HttpClient);
  private readonly treeEndpoint = '/api/v1/taxonomy/tree';
  private readonly overridesEndpoint = '/api/v1/taxonomy/overrides';
  private readonly mergeEndpoint = '/api/v1/taxonomy/merge';
  private readonly reorderEndpoint = '/api/v1/taxonomy/reorder';

  readonly tree = signal<TaxonomySubjectNode[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly subjects = computed(() => this.tree());

  readonly subjectMap = computed(() => {
    const map = new Map<number, TaxonomySubjectNode>();
    for (const sub of this.tree()) {
      if (sub && sub.id != null) {
        map.set(Number(sub.id), sub);
      }
    }
    return map;
  });

  readonly topicMap = computed(() => {
    const map = new Map<number, TaxonomyTopicNode>();
    for (const sub of this.tree()) {
      if (sub?.topics) {
        for (const top of sub.topics) {
          if (top && top.id != null) {
            map.set(Number(top.id), top);
          }
        }
      }
    }
    return map;
  });

  readonly subtopicMap = computed(() => {
    const map = new Map<number, TaxonomySubtopicNode>();
    for (const sub of this.tree()) {
      if (sub?.topics) {
        for (const top of sub.topics) {
          if (top?.subtopics) {
            for (const st of top.subtopics) {
              if (st && st.id != null) {
                map.set(Number(st.id), st);
              }
            }
          }
        }
      }
    }
    return map;
  });

  loadTree(examId?: string): Observable<TaxonomySubjectNode[]> {
    this.loading.set(true);
    this.error.set(null);

    let params = new HttpParams();
    if (examId) {
      params = params.set('examId', examId);
    }

    return this.http
      .get<{ data?: TaxonomySubjectNode[] } | TaxonomySubjectNode[]>(this.treeEndpoint, { params })
      .pipe(
        map((res) => {
          const list = Array.isArray(res) ? res : (res as any)?.data || [];
          return list as TaxonomySubjectNode[];
        }),
        tap({
          next: (nodes) => {
            this.tree.set(nodes);
            this.loading.set(false);
          },
          error: (err) => {
            this.loading.set(false);
            const msg = err?.error?.message || err?.message || 'Failed to load taxonomy tree';
            this.error.set(msg);
          },
        }),
        catchError((err) => {
          this.loading.set(false);
          return throwError(() => err);
        })
      );
  }

  applyOverrides(requests: TaxonomyOverrideRequest[]): Observable<void> {
    this.loading.set(true);
    return this.http.put<any>(this.overridesEndpoint, requests).pipe(
      map(() => void 0),
      tap({
        next: () => {
          this.loadTree().subscribe();
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err?.error?.message || 'Failed to apply taxonomy overrides');
        },
      })
    );
  }

  mergeNodes(request: MergeTaxonomyRequest): Observable<void> {
    this.loading.set(true);
    return this.http.post<any>(this.mergeEndpoint, request).pipe(
      map(() => void 0),
      tap({
        next: () => {
          this.loadTree().subscribe();
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err?.error?.message || 'Failed to merge taxonomy nodes');
        },
      })
    );
  }

  reorderNodes(request: ReorderTaxonomyRequest): Observable<void> {
    this.loading.set(true);
    return this.http.put<any>(this.reorderEndpoint, request).pipe(
      map(() => void 0),
      tap({
        next: () => {
          this.loadTree().subscribe();
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err?.error?.message || 'Failed to reorder taxonomy nodes');
        },
      })
    );
  }

  nodeName(type: 'SUBJECT' | 'TOPIC' | 'SUBTOPIC' | string, id: number | null | undefined): string {
    if (id == null) return '';
    const numericId = Number(id);
    const upperType = (type || '').toUpperCase();

    if (upperType === 'SUBJECT') {
      return this.subjectMap().get(numericId)?.name || '';
    }
    if (upperType === 'TOPIC') {
      return this.topicMap().get(numericId)?.name || '';
    }
    if (upperType === 'SUBTOPIC') {
      return this.subtopicMap().get(numericId)?.name || '';
    }

    // Fallback: search across all maps
    return (
      this.subtopicMap().get(numericId)?.name ||
      this.topicMap().get(numericId)?.name ||
      this.subjectMap().get(numericId)?.name ||
      ''
    );
  }

  subjectName(id: number | null | undefined): string {
    return this.nodeName('SUBJECT', id);
  }

  topicName(id: number | null | undefined): string {
    return this.nodeName('TOPIC', id);
  }

  subtopicName(id: number | null | undefined): string {
    return this.nodeName('SUBTOPIC', id);
  }

  getTopicsForSubject(subjectId: number | null | undefined): TaxonomyTopicNode[] {
    if (subjectId == null) return [];
    return this.subjectMap().get(Number(subjectId))?.topics || [];
  }

  getSubtopicsForTopic(topicId: number | null | undefined): TaxonomySubtopicNode[] {
    if (topicId == null) return [];
    return this.topicMap().get(Number(topicId))?.subtopics || [];
  }
}
