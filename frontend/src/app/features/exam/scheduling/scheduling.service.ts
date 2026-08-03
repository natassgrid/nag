import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

// ── Interfaces ───────────────────────────────────────────────────────────────

interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
}

export interface ScheduleResponse {
  id: string;
  examinationId: string;
  scheduleName: string;
  scheduleVersion: number;
  notificationNumber?: string;
  examDate: string;
  reserveDate?: string;
  timeZone: string;
  status: string;
  changeReason?: string;
  effectiveFrom?: string;
  previousVersionId?: string;
  createdBy?: string;
  modifiedBy?: string;
  approvedBy?: string;
  approvedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ShiftResponse {
  id: string;
  scheduleId: string;
  shiftNumber: number;
  shiftName?: string;
  reportingTime: string;
  gateClosingTime: string;
  loginStartTime: string;
  examStartTime: string;
  examEndTime: string;
  exitTime?: string;
  durationMinutes: number;
  bufferMinutes: number;
  createdAt: string;
  updatedAt: string;
}

export interface CentreResponse {
  id: string;
  countryId?: number;
  stateId?: number;
  cityId?: number;
  countryName?: string;
  stateName?: string;
  cityName?: string;
  region?: string;
  state: string;
  district?: string;
  city: string;
  centreName: string;
  building?: string;
  floor?: string;
  laboratoryIdentifier?: string;
  totalCapacity: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SeatAllocationResponse {
  id: string;
  shiftId: string;
  centreId: string;
  totalSeats: number;
  availableSeats: number;
  reservedSeats: number;
  pwdSeats: number;
  emergencyBufferSeats: number;
  femaleReservedSeats: number;
  specialCategorySeats: number;
  createdAt: string;
  updatedAt: string;
}

// ── Request DTOs ─────────────────────────────────────────────────────────────

export interface CreateScheduleRequest {
  scheduleName: string;
  notificationNumber?: string;
  examDate: string;
  reserveDate?: string;
  timeZone: string;
}

export interface ScheduleTransitionRequest {
  targetStatus: string;
  comment?: string;
}

export interface AmendScheduleRequest {
  changeReason: string;
  scheduleName: string;
  notificationNumber?: string;
  examDate: string;
  reserveDate?: string;
  effectiveFrom?: string;
  timeZone: string;
}

export interface CreateShiftRequest {
  shiftNumber: number;
  shiftName?: string;
  reportingTime: string;
  gateClosingTime: string;
  loginStartTime: string;
  examStartTime: string;
  examEndTime: string;
  exitTime?: string;
  durationMinutes: number;
  bufferMinutes: number;
}

export interface CreateCentreRequest {
  countryId?: number;
  stateId?: number;
  cityId?: number;
  region?: string;
  state: string;
  district?: string;
  city: string;
  centreName: string;
  building?: string;
  floor?: string;
  laboratoryIdentifier?: string;
  totalCapacity: number;
  active: boolean;
}

export interface SeatAllocationRequest {
  centreId: string;
  totalSeats: number;
  availableSeats: number;
  reservedSeats: number;
  pwdSeats: number;
  emergencyBufferSeats: number;
  femaleReservedSeats: number;
  specialCategorySeats: number;
}

// ── Service ──────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class SchedulingService {
  private readonly base = '/api/v1/examinations';

  constructor(private http: HttpClient) {}

  // ── Schedules ──────────────────────────────────────────────────────────────

  createSchedule(examId: string, req: CreateScheduleRequest): Observable<ScheduleResponse> {
    return this.http.post<ApiResponse<ScheduleResponse>>(`${this.base}/${examId}/schedules`, req)
      .pipe(map(r => r.data));
  }

  listSchedules(examId: string, page = 0, size = 20): Observable<ScheduleResponse[]> {
    return this.http.get<ApiResponse<any>>(`${this.base}/${examId}/schedules?page=${page}&size=${size}`)
      .pipe(map(r => r?.data?.content ?? r?.data ?? []));
  }

  getSchedule(examId: string, scheduleId: string): Observable<ScheduleResponse> {
    return this.http.get<ApiResponse<ScheduleResponse>>(`${this.base}/${examId}/schedules/${scheduleId}`)
      .pipe(map(r => r.data));
  }

  transitionSchedule(examId: string, scheduleId: string, req: ScheduleTransitionRequest): Observable<ScheduleResponse> {
    return this.http.put<ApiResponse<ScheduleResponse>>(
      `${this.base}/${examId}/schedules/${scheduleId}/transition`, req
    ).pipe(map(r => r.data));
  }

  amendSchedule(examId: string, scheduleId: string, req: AmendScheduleRequest): Observable<ScheduleResponse> {
    return this.http.put<ApiResponse<ScheduleResponse>>(
      `${this.base}/${examId}/schedules/${scheduleId}/amend`, req
    ).pipe(map(r => r.data));
  }

  // ── Shifts ─────────────────────────────────────────────────────────────────

  listShifts(examId: string, scheduleId: string): Observable<ShiftResponse[]> {
    return this.http.get<ApiResponse<ShiftResponse[]>>(
      `${this.base}/${examId}/schedules/${scheduleId}/shifts`
    ).pipe(map(r => r?.data ?? []));
  }

  addShift(examId: string, scheduleId: string, req: CreateShiftRequest): Observable<ShiftResponse> {
    return this.http.post<ApiResponse<ShiftResponse>>(
      `${this.base}/${examId}/schedules/${scheduleId}/shifts`, req
    ).pipe(map(r => r.data));
  }

  updateShift(examId: string, scheduleId: string, shiftId: string, req: CreateShiftRequest): Observable<ShiftResponse> {
    return this.http.put<ApiResponse<ShiftResponse>>(
      `${this.base}/${examId}/schedules/${scheduleId}/shifts/${shiftId}`, req
    ).pipe(map(r => r.data));
  }

  // ── Centres ────────────────────────────────────────────────────────────────

  createCentre(req: CreateCentreRequest): Observable<CentreResponse> {
    return this.http.post<ApiResponse<CentreResponse>>(`${this.base}/centres`, req)
      .pipe(map(r => r.data));
  }

  listCentres(state?: string, city?: string, page = 0, size = 20, search?: string): Observable<CentreResponse[]> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (state) params = params.set('state', state);
    if (city) params = params.set('city', city);
    if (search) params = params.set('search', search);
    return this.http.get<ApiResponse<any>>(`${this.base}/centres`, { params })
      .pipe(map(r => r?.data?.content ?? r?.data ?? []));
  }

  getCentre(centreId: string): Observable<CentreResponse> {
    return this.http.get<ApiResponse<CentreResponse>>(`${this.base}/centres/${centreId}`)
      .pipe(map(r => r.data));
  }

  deactivateCentre(centreId: string): Observable<CentreResponse> {
    return this.http.put<ApiResponse<CentreResponse>>(`${this.base}/centres/${centreId}/deactivate`, {})
      .pipe(map(r => r.data));
  }

  // ── Allocations ────────────────────────────────────────────────────────────

  upsertAllocation(examId: string, scheduleId: string, shiftId: string, req: SeatAllocationRequest): Observable<SeatAllocationResponse> {
    return this.http.post<ApiResponse<SeatAllocationResponse>>(
      `${this.base}/${examId}/schedules/${scheduleId}/shifts/${shiftId}/allocations`, req
    ).pipe(map(r => r.data));
  }

  listAllocations(examId: string, scheduleId: string, shiftId: string): Observable<SeatAllocationResponse[]> {
    return this.http.get<ApiResponse<SeatAllocationResponse[]>>(
      `${this.base}/${examId}/schedules/${scheduleId}/shifts/${shiftId}/allocations`
    ).pipe(map(r => r?.data ?? []));
  }
}
