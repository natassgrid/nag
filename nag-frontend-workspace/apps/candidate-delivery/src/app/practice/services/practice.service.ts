import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PracticeSet, PracticeSession, PracticeResult,
  PracticeHistoryItem, StartSessionRequest, PagedResponse
} from '../models';

@Injectable({ providedIn: 'root' })
export class PracticeService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/practice';

  getSets(): Observable<PracticeSet[]> {
    return this.http.get<PracticeSet[]>(`${this.base}/sets`);
  }

  getSet(setId: string): Observable<PracticeSet> {
    return this.http.get<PracticeSet>(`${this.base}/sets/${setId}`);
  }

  startSession(req: StartSessionRequest): Observable<PracticeSession> {
    return this.http.post<PracticeSession>(`${this.base}/sessions`, req);
  }

  getSession(sessionId: string): Observable<PracticeSession> {
    return this.http.get<PracticeSession>(`${this.base}/sessions/${sessionId}`);
  }

  submitSession(sessionId: string): Observable<PracticeResult> {
    return this.http.post<PracticeResult>(`${this.base}/sessions/${sessionId}/submit`, {});
  }

  getResult(sessionId: string): Observable<PracticeResult> {
    return this.http.get<PracticeResult>(`${this.base}/sessions/${sessionId}/result`);
  }

  getHistory(page = 0, size = 10): Observable<PagedResponse<PracticeHistoryItem>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedResponse<PracticeHistoryItem>>(`${this.base}/history`, { params });
  }
}
