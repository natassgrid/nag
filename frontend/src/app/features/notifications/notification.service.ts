import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of, catchError } from 'rxjs';

export interface Notification {
  id: string;
  subject: string;
  body: string;
  type: string;
  status: string;
  isRead: boolean;
  sentAt: string;
  createdAt: string;
}

interface ApiResponse<T> {
  data: T;
  message?: string;
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly baseUrl = '/api/v1/notifications';

  constructor(private http: HttpClient) {}

  getNotifications(): Observable<Notification[]> {
    return this.http
      .get<ApiResponse<Notification[]>>(this.baseUrl)
      .pipe(
        map(res => res?.data ?? []),
        catchError(() => of([] as Notification[]))
      );
  }

  markAsRead(id: string): Observable<void> {
    return this.http
      .put<ApiResponse<void>>(`${this.baseUrl}/${id}/read`, {})
      .pipe(map(res => res.data));
  }
}
