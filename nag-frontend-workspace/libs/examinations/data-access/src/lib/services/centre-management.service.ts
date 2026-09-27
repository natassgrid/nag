import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { ApiResponse, PagedResponse } from '../models/common.model';
import {
  CentreResponse,
  CreateCentreRequest,
  SeatAllocationResponse,
  SeatAllocationRequest,
} from '../models/centre.model';

@Injectable({
  providedIn: 'root',
})
export class CentreManagementService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/examinations';

  readonly centres = signal<CentreResponse[]>([]);
  readonly loading = signal<boolean>(false);

  createCentre(req: CreateCentreRequest): Observable<CentreResponse> {
    return this.http
      .post<ApiResponse<CentreResponse> | CentreResponse>(`${this.baseUrl}/centres`, req)
      .pipe(
        map((res) => ((res as any)?.data ?? res) as CentreResponse),
        tap((created) => this.centres.update((list) => [created, ...(list || [])]))
      );
  }

  listCentres(
    state?: string,
    city?: string,
    page = 0,
    size = 20,
    search?: string,
    sort?: string,
    order?: string
  ): Observable<CentreResponse[]> {
    this.loading.set(true);
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (state) params = params.set('state', state);
    if (city) params = params.set('city', city);
    if (search) params = params.set('search', search.trim());
    if (sort) params = params.set('sort', sort);
    if (order) params = params.set('order', order);

    return this.http
      .get<ApiResponse<any> | any>(`${this.baseUrl}/centres`, { params })
      .pipe(
        map((res) => {
          const payload = res?.data ?? res;
          return (Array.isArray(payload?.content)
            ? payload.content
            : Array.isArray(payload)
            ? payload
            : []) as CentreResponse[];
        }),
        tap({
          next: (list) => {
            this.centres.set(list || []);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        })
      );
  }

  listCentresPaged(
    state?: string,
    city?: string,
    page = 0,
    size = 20,
    search?: string,
    sort?: string,
    order?: string
  ): Observable<PagedResponse<CentreResponse>> {
    this.loading.set(true);
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (state) params = params.set('state', state);
    if (city) params = params.set('city', city);
    if (search) params = params.set('search', search.trim());
    if (sort) params = params.set('sort', sort);
    if (order) params = params.set('order', order);

    return this.http
      .get<ApiResponse<any> | any>(`${this.baseUrl}/centres`, { params })
      .pipe(
        map((res) => {
          const payload = res?.data ?? res;
          if (Array.isArray(payload)) {
            return {
              content: payload,
              totalElements: payload.length,
              totalPages: 1,
              size: payload.length,
              number: 0,
            };
          }
          if (payload && Array.isArray(payload.content)) {
            return {
              content: payload.content,
              totalElements: payload.totalElements ?? payload.content.length,
              totalPages: payload.totalPages ?? 1,
              size: payload.size ?? payload.content.length,
              number: payload.number ?? 0,
            };
          }
          return { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };
        }),
        tap({
          next: (paged) => {
            this.centres.set(paged.content || []);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        })
      );
  }

  getCentre(centreId: string): Observable<CentreResponse> {
    return this.http
      .get<ApiResponse<CentreResponse> | CentreResponse>(
        `${this.baseUrl}/centres/${centreId}`
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as CentreResponse));
  }

  deactivateCentre(centreId: string): Observable<CentreResponse> {
    return this.http
      .put<ApiResponse<CentreResponse> | CentreResponse>(
        `${this.baseUrl}/centres/${centreId}/deactivate`,
        {}
      )
      .pipe(
        map((res) => ((res as any)?.data ?? res) as CentreResponse),
        tap((updated) =>
          this.centres.update((list) =>
            (list || []).map((c) => (c.id === centreId ? updated : c))
          )
        )
      );
  }

  // --- Seat Allocations ---

  upsertAllocation(
    examId: string,
    scheduleId: string,
    shiftId: string,
    req: SeatAllocationRequest
  ): Observable<SeatAllocationResponse> {
    return this.http
      .post<ApiResponse<SeatAllocationResponse> | SeatAllocationResponse>(
        `${this.baseUrl}/${examId}/schedules/${scheduleId}/shifts/${shiftId}/allocations`,
        req
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as SeatAllocationResponse));
  }

  listAllocations(
    examId: string,
    scheduleId: string,
    shiftId: string
  ): Observable<SeatAllocationResponse[]> {
    return this.http
      .get<ApiResponse<SeatAllocationResponse[]> | SeatAllocationResponse[]>(
        `${this.baseUrl}/${examId}/schedules/${scheduleId}/shifts/${shiftId}/allocations`
      )
      .pipe(
        map((res) => {
          const payload = (res as any)?.data ?? res;
          return (Array.isArray(payload) ? payload : []) as SeatAllocationResponse[];
        })
      );
  }
}
