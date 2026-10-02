import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PracticeSet, UpdatePracticeSetRequest } from '../models';

@Injectable({ providedIn: 'root' })
export class PracticeSetService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/admin/practice/sets';

  getAll(): Observable<PracticeSet[]> {
    return this.http.get<PracticeSet[]>(this.base);
  }

  create(req: UpdatePracticeSetRequest): Observable<PracticeSet> {
    return this.http.post<PracticeSet>(this.base, req);
  }

  update(id: string, req: UpdatePracticeSetRequest): Observable<PracticeSet> {
    return this.http.put<PracticeSet>(`${this.base}/${id}`, req);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  publish(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/publish`, {});
  }

  unpublish(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/unpublish`, {});
  }
}
