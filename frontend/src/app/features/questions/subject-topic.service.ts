import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

export interface Subject {
  id: number;
  name: string;
  code: string;
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

export interface SubjectHierarchy {
  id: number;
  name: string;
  code: string;
  topics: {
    id: number;
    name: string;
    subtopics: {
      id: number;
      name: string;
    }[];
  }[];
}

interface ApiResponse<T> {
  status: string;
  message?: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class SubjectTopicService {
  private readonly baseUrl = `${environment.apiUrl}/api/v1/subjects`;

  constructor(private http: HttpClient) {}

  getSubjects(): Observable<Subject[]> {
    return this.http
      .get<ApiResponse<Subject[]>>(this.baseUrl)
      .pipe(map(res => res.data));
  }

  getSubject(id: number): Observable<Subject> {
    return this.http
      .get<ApiResponse<Subject>>(`${this.baseUrl}/${id}`)
      .pipe(map(res => res.data));
  }

  createSubject(data: { name: string; code: string; description?: string }): Observable<Subject> {
    return this.http
      .post<ApiResponse<Subject>>(this.baseUrl, data)
      .pipe(map(res => res.data));
  }

  updateSubject(id: number, data: { name: string; code: string; description?: string }): Observable<Subject> {
    return this.http
      .put<ApiResponse<Subject>>(`${this.baseUrl}/${id}`, data)
      .pipe(map(res => res.data));
  }

  deleteSubject(id: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.baseUrl}/${id}`)
      .pipe(map(() => void 0));
  }

  getTopics(subjectId: number): Observable<Topic[]> {
    return this.http
      .get<ApiResponse<Topic[]>>(`${this.baseUrl}/${subjectId}/topics`)
      .pipe(map(res => res.data));
  }

  createTopic(subjectId: number, data: { name: string; description?: string }): Observable<Topic> {
    return this.http
      .post<ApiResponse<Topic>>(`${this.baseUrl}/${subjectId}/topics`, data)
      .pipe(map(res => res.data));
  }

  getSubtopics(subjectId: number, topicId: number): Observable<Subtopic[]> {
    return this.http
      .get<ApiResponse<Subtopic[]>>(`${this.baseUrl}/${subjectId}/topics/${topicId}/subtopics`)
      .pipe(map(res => res.data));
  }

  createSubtopic(subjectId: number, topicId: number, data: { name: string; description?: string }): Observable<Subtopic> {
    return this.http
      .post<ApiResponse<Subtopic>>(`${this.baseUrl}/${subjectId}/topics/${topicId}/subtopics`, data)
      .pipe(map(res => res.data));
  }

  getHierarchy(): Observable<SubjectHierarchy[]> {
    return this.http
      .get<ApiResponse<SubjectHierarchy[]>>(`${this.baseUrl}/hierarchy`)
      .pipe(map(res => res.data));
  }
}
