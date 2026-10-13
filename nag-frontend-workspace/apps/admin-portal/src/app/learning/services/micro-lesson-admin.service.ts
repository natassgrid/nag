import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MicroLessonReviewItem,
  ReviewMicroLessonRequest,
  BatchGenerateMicroLessonsRequest,
  BatchGenerateResponse,
} from '../models/micro-lesson.model';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class MicroLessonAdminService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/admin/micro-lessons';

  listForReview(
    status?: string,
    subjectId?: number,
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<MicroLessonReviewItem>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    if (subjectId != null) {
      params = params.set('subjectId', subjectId.toString());
    }

    return this.http.get<PageResponse<MicroLessonReviewItem>>(this.apiUrl, {
      params,
    });
  }

  reviewMicroLesson(
    id: string,
    req: ReviewMicroLessonRequest
  ): Observable<MicroLessonReviewItem> {
    return this.http.put<MicroLessonReviewItem>(
      `${this.apiUrl}/${id}/review`,
      req
    );
  }

  triggerBatchGenerate(
    req?: BatchGenerateMicroLessonsRequest
  ): Observable<BatchGenerateResponse> {
    return this.http.post<BatchGenerateResponse>(
      `${this.apiUrl}/batch-generate`,
      req || {}
    );
  }
}
