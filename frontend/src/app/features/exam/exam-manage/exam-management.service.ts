import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface ExaminationResponse {
  id: string;
  name: string;
  durationMinutes: number;
  totalMarks: number;
  negativeMarkingEnabled: boolean;
  negativeMarkingValue: number;
  navigationPolicy: string;
  calculatorPolicy: string;
  reviewFlagEnabled: boolean;
  sections: Section[];
  status: string;
  createdAt: string;
}

export interface Section {
  name: string;
  questionCount: number;
  marksPerQuestion: number;
}

export interface CreateExamRequest {
  name: string;
  durationMinutes: number;
  totalMarks: number;
  negativeMarkingEnabled: boolean;
  negativeMarkingValue: number;
  navigationPolicy: string;
  calculatorPolicy: string;
  reviewFlagEnabled: boolean;
  sections: Section[];
}

interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class ExamManagementService {
  private readonly baseUrl = '/api/v1/examinations';

  constructor(private http: HttpClient) {}

  getExams(): Observable<ExaminationResponse[]> {
    return this.http
      .get<ApiResponse<ExaminationResponse[]>>(this.baseUrl)
      .pipe(map(res => res.data));
  }

  createExam(data: CreateExamRequest): Observable<ExaminationResponse> {
    return this.http
      .post<ApiResponse<ExaminationResponse>>(this.baseUrl, data)
      .pipe(map(res => res.data));
  }

  updateExam(id: string, data: CreateExamRequest): Observable<ExaminationResponse> {
    return this.http
      .put<ApiResponse<ExaminationResponse>>(`${this.baseUrl}/${id}`, data)
      .pipe(map(res => res.data));
  }

  publishExam(id: string): Observable<ExaminationResponse> {
    return this.http
      .put<ApiResponse<ExaminationResponse>>(`${this.baseUrl}/${id}/publish`, {})
      .pipe(map(res => res.data));
  }

  getExam(id: string): Observable<ExaminationResponse> {
    return this.http
      .get<ApiResponse<ExaminationResponse>>(`${this.baseUrl}/${id}`)
      .pipe(map(res => res.data));
  }
}
