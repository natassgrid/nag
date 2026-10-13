import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SubtopicMicroLesson } from '../models/micro-lesson.model';

@Injectable({ providedIn: 'root' })
export class MicroLessonService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/micro-lessons';

  getPublishedMicroLesson(
    subtopicId: number,
    targetExam?: string
  ): Observable<SubtopicMicroLesson> {
    let params = new HttpParams();
    if (targetExam) {
      params = params.set('targetExam', targetExam);
    }
    return this.http.get<SubtopicMicroLesson>(
      `${this.apiUrl}/subtopic/${subtopicId}`,
      { params }
    );
  }
}
