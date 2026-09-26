import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { ApiResponse } from '../models/common.model';
import {
  ScheduleResponse,
  CreateScheduleRequest,
  ScheduleTransitionRequest,
  AmendScheduleRequest,
  ShiftResponse,
  CreateShiftRequest,
} from '../models/scheduling.model';

@Injectable({
  providedIn: 'root',
})
export class SchedulingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/examinations';

  readonly schedules = signal<ScheduleResponse[]>([]);
  readonly currentSchedule = signal<ScheduleResponse | null>(null);
  readonly shifts = signal<ShiftResponse[]>([]);
  readonly loading = signal<boolean>(false);

  // --- Schedules ---

  createSchedule(examId: string, req: CreateScheduleRequest): Observable<ScheduleResponse> {
    return this.http
      .post<ApiResponse<ScheduleResponse> | ScheduleResponse>(
        `${this.baseUrl}/${examId}/schedules`,
        req
      )
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ScheduleResponse),
        tap((created) => this.schedules.update((list) => [created, ...(list || [])]))
      );
  }

  listSchedules(examId: string, page = 0, size = 20): Observable<ScheduleResponse[]> {
    this.loading.set(true);
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http
      .get<ApiResponse<any> | any>(`${this.baseUrl}/${examId}/schedules`, { params })
      .pipe(
        map((res) => {
          const payload = res?.data ?? res;
          return (Array.isArray(payload?.content)
            ? payload.content
            : Array.isArray(payload)
            ? payload
            : []) as ScheduleResponse[];
        }),
        tap({
          next: (list) => {
            this.schedules.set(list || []);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        })
      );
  }

  getSchedule(examId: string, scheduleId: string): Observable<ScheduleResponse> {
    return this.http
      .get<ApiResponse<ScheduleResponse> | ScheduleResponse>(
        `${this.baseUrl}/${examId}/schedules/${scheduleId}`
      )
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ScheduleResponse),
        tap((s) => this.currentSchedule.set(s))
      );
  }

  transitionSchedule(
    examId: string,
    scheduleId: string,
    req: ScheduleTransitionRequest
  ): Observable<ScheduleResponse> {
    return this.http
      .put<ApiResponse<ScheduleResponse> | ScheduleResponse>(
        `${this.baseUrl}/${examId}/schedules/${scheduleId}/transition`,
        req
      )
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ScheduleResponse),
        tap((updated) => {
          this.schedules.update((list) =>
            (list || []).map((s) => (s.id === scheduleId ? updated : s))
          );
          if (this.currentSchedule()?.id === scheduleId) {
            this.currentSchedule.set(updated);
          }
        })
      );
  }

  amendSchedule(
    examId: string,
    scheduleId: string,
    req: AmendScheduleRequest
  ): Observable<ScheduleResponse> {
    return this.http
      .put<ApiResponse<ScheduleResponse> | ScheduleResponse>(
        `${this.baseUrl}/${examId}/schedules/${scheduleId}/amend`,
        req
      )
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ScheduleResponse),
        tap((newVersion) => this.schedules.update((list) => [newVersion, ...(list || [])]))
      );
  }

  // --- Shifts ---

  listShifts(examId: string, scheduleId: string): Observable<ShiftResponse[]> {
    return this.http
      .get<ApiResponse<ShiftResponse[]> | ShiftResponse[]>(
        `${this.baseUrl}/${examId}/schedules/${scheduleId}/shifts`
      )
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ShiftResponse[]),
        tap((list) => this.shifts.set(list || []))
      );
  }

  addShift(
    examId: string,
    scheduleId: string,
    req: CreateShiftRequest
  ): Observable<ShiftResponse> {
    return this.http
      .post<ApiResponse<ShiftResponse> | ShiftResponse>(
        `${this.baseUrl}/${examId}/schedules/${scheduleId}/shifts`,
        req
      )
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ShiftResponse),
        tap((created) => this.shifts.update((list) => [...(list || []), created]))
      );
  }

  updateShift(
    examId: string,
    scheduleId: string,
    shiftId: string,
    req: CreateShiftRequest
  ): Observable<ShiftResponse> {
    return this.http
      .put<ApiResponse<ShiftResponse> | ShiftResponse>(
        `${this.baseUrl}/${examId}/schedules/${scheduleId}/shifts/${shiftId}`,
        req
      )
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ShiftResponse),
        tap((updated) =>
          this.shifts.update((list) =>
            (list || []).map((s) => (s.id === shiftId ? updated : s))
          )
        )
      );
  }
}
