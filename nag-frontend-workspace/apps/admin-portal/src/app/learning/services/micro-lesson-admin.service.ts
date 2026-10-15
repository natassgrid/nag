import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  MicroLessonReviewItem,
  ReviewMicroLessonRequest,
  BatchGenerateMicroLessonsRequest,
  BatchGenerateResponse,
  GenerateMicroLessonRequest,
  adaptToReviewItem,
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

    return this.http
      .get<PageResponse<any>>(this.apiUrl, { params })
      .pipe(
        map((res) => ({
          ...res,
          content: (res.content || []).map(adaptToReviewItem),
        }))
      );
  }

  reviewMicroLesson(
    id: string,
    req: ReviewMicroLessonRequest
  ): Observable<MicroLessonReviewItem> {
    const payload: any = {
      status: req.action === 'EDIT' ? (req.status || 'IN_REVIEW') : (req.action || req.status),
      comments: req.reviewerNotes || req.comments,
      action: req.action,
      reviewerNotes: req.reviewerNotes,
    };
    if (req.updatedContent) {
      payload.coreConcept = req.updatedContent.coreConcept;
      payload.trap = req.updatedContent.trapAlert;
      payload.trapAlert = req.updatedContent.trapAlert;
      if (req.updatedContent.formulaCheatSheet) {
        payload.cheatSheet = req.updatedContent.formulaCheatSheet
          .split('\n\n')
          .map((s) => s.trim())
          .filter((s) => s.length > 0);
      }
      if (req.updatedContent.stepByStepBreakdown) {
        payload.stepByStepBreakdown = {
          questionStem: req.updatedContent.stepByStepBreakdown.problemStatement,
          steps: req.updatedContent.stepByStepBreakdown.steps.map(
            (s) => s.explanation || s.title
          ),
          finalAnswer: req.updatedContent.stepByStepBreakdown.finalAnswer,
        };
      }
    }
    return this.http
      .put<any>(`${this.apiUrl}/${id}/review`, payload)
      .pipe(map(adaptToReviewItem));
  }

  generateSingle(
    req: GenerateMicroLessonRequest
  ): Observable<MicroLessonReviewItem> {
    return this.http
      .post<any>(`${this.apiUrl}/generate`, req)
      .pipe(map(adaptToReviewItem));
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
