import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface Subject {
  id: string;
  name: string;
  code: string;
  description: string;
  active: boolean;
}

export interface Topic {
  id: string;
  subjectId: string;
  name: string;
  description: string;
  active: boolean;
}

export interface Subtopic {
  id: string;
  topicId: string;
  name: string;
  description: string;
  active: boolean;
}

export interface SubjectHierarchy {
  id: string;
  name: string;
  code: string;
  description: string;
  active: boolean;
  topics: TopicNode[];
}

export interface TopicNode {
  id: string;
  name: string;
  description: string;
  active: boolean;
  subtopics: SubtopicNode[];
}

export interface SubtopicNode {
  id: string;
  name: string;
  description: string;
  active: boolean;
}

interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class SubjectTopicService {
  private readonly baseUrl = '/api/v1/subjects';

  constructor(private http: HttpClient) {}

  getSubjects(): Observable<Subject[]> {
    return this.http
      .get<ApiResponse<Subject[]>>(this.baseUrl)
      .pipe(map(res => res.data));
  }

  createSubject(data: { name: string; code?: string; description?: string }): Observable<Subject> {
    return this.http
      .post<ApiResponse<Subject>>(this.baseUrl, data)
      .pipe(map(res => res.data));
  }

  getTopics(subjectId: string): Observable<Topic[]> {
    return this.http
      .get<ApiResponse<Topic[]>>(`${this.baseUrl}/${subjectId}/topics`)
      .pipe(map(res => res.data));
  }

  createTopic(subjectId: string, data: { name: string; description?: string }): Observable<Topic> {
    return this.http
      .post<ApiResponse<Topic>>(`${this.baseUrl}/${subjectId}/topics`, data)
      .pipe(map(res => res.data));
  }

  getSubtopics(topicId: string, subjectId: string): Observable<Subtopic[]> {
    return this.http
      .get<ApiResponse<Subtopic[]>>(`${this.baseUrl}/${subjectId}/topics/${topicId}/subtopics`)
      .pipe(map(res => res.data));
  }

  createSubtopic(subjectId: string, topicId: string, data: { name: string; description?: string }): Observable<Subtopic> {
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
