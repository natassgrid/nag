import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface ScheduleResponse {
  id: string;
  examinationId: string;
  scheduleName: string;
  scheduleVersion: number;
  notificationNumber: string | null;
  examDate: string;
  reserveDate: string | null;
  timeZone: string;
  status: string;
  changeReason: string | null;
  effectiveFrom: string | null;
  previousVersionId: string | null;
  createdBy: string;
  modifiedBy: string;
  approvedBy: string | null;
  approvedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ShiftResponse {
  id: string;
  scheduleId: string;
  shiftNumber: number;
  shiftName: string;
  reportingTime: string;
  gateClosingTime: string;
  loginStartTime: string;
  examStartTime: string;
  examEndTime: string;
  exitTime: string | null;
  durationMinutes: number;
  bufferMinutes: number;
  createdAt: string;
  updatedAt: string;
}

export interface CentreResponse {
  id: string;
  region: string;
  state: string;
  district: string;
  city: string;
  centreName: string;
  building: string;
  floor: string;
  laboratoryIdentifier: string;
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

export interface CreateScheduleRequest {
  scheduleName: string;
  notificationNumber?: string | null;
  examDate: string;
  reserveDate?: string | null;
  timeZone: string;
}

export interface ScheduleTransitionRequest {
  targetStatus: string;
  comment?: string;
}

export interface AmendScheduleRequest {
  changeReason: string;
  scheduleName: string;
  notificationNumber?: string | null;
  examDate: string;
  reserveDate?: string | null;
  effectiveFrom?: string | null;
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

interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class SchedulingService {
  private readonly base = '/api/v1/examinations';

  constructor(private http: HttpClient) {}

  // ── Schedules ─────────────────────────────────────────
  createSchedule(examId: string, req: CreateScheduleRequest): Observable<ScheduleResponse> {
    return this.http
      .post<ApiResponse<ScheduleResponse>>(`${this.base}/${examId}/schedules`, req)
      .pipe(map(res => res.data));
  }

  listSchedules(examId: string): Observable<ScheduleResponse[]> {
    return this.http
      .get<ApiResponse<ScheduleResponse[]>>(`${this.base}/${examId}/schedules`)
      .pipe(map(res => res.data));
  }

  getSchedule(examId: string, scheduleId: string): Observable<ScheduleResponse> {
    return this.http
      .get<ApiResponse<ScheduleResponse>>(`${this.base}/${examId}/schedules/${scheduleId}`)
      .pipe(map(res => res.data));
  }

  transitionSchedule(examId: string, scheduleId: string, req: ScheduleTransitionRequest): Observable<ScheduleResponse> {
    return this.http
      .put<ApiResponse<ScheduleResponse>>(`${this.base}/${examId}/schedules/${scheduleId}/transition`, req)
      .pipe(map(res => res.data));
  }

  amendSchedule(examId: string, scheduleId: string, req: AmendScheduleRequest): Observable<ScheduleResponse> {
    return this.http
      .put<ApiResponse<ScheduleResponse>>(`${this.base}/${examId}/schedules/${scheduleId}/amend`, req)
      .pipe(map(res => res.data));
  }

  // ── Shifts ────────────────────────────────────────────
  listShifts(examId: string, scheduleId: string): Observable<ShiftResponse[]> {
    return this.http
      .get<ApiResponse<ShiftResponse[]>>(`${this.base}/${examId}/schedules/${scheduleId}/shifts`)
      .pipe(map(res => res.data));
  }

  addShift(examId: string, scheduleId: string, req: CreateShiftRequest): Observable<ShiftResponse> {
    return this.http
      .post<ApiResponse<ShiftResponse>>(`${this.base}/${examId}/schedules/${scheduleId}/shifts`, req)
      .pipe(map(res => res.data));
  }

  updateShift(examId: string, scheduleId: string, shiftId: string, req: CreateShiftRequest): Observable<ShiftResponse> {
    return this.http
      .put<ApiResponse<ShiftResponse>>(`${this.base}/${examId}/schedules/${scheduleId}/shifts/${shiftId}`, req)
      .pipe(map(res => res.data));
  }

  // ── Centres ───────────────────────────────────────────
  createCentre(req: CreateCentreRequest): Observable<CentreResponse> {
    return this.http
      .post<ApiResponse<CentreResponse>>(`${this.base}/centres`, req)
      .pipe(map(res => res.data));
  }

  listCentres(state?: string, city?: string): Observable<CentreResponse[]> {
    let params = new HttpParams();
    if (state) params = params.set('state', state);
    if (city) params = params.set('city', city);

    return this.http
      .get<ApiResponse<CentreResponse[]>>(`${this.base}/centres`, { params })
      .pipe(map(res => res.data));
  }

  deactivateCentre(centreId: string): Observable<CentreResponse> {
    return this.http
      .put<ApiResponse<CentreResponse>>(`${this.base}/centres/${centreId}/deactivate`, {})
      .pipe(map(res => res.data));
  }

  // ── Allocations ───────────────────────────────────────
  upsertAllocation(examId: string, scheduleId: string, shiftId: string, req: SeatAllocationRequest): Observable<SeatAllocationResponse> {
    return this.http
      .post<ApiResponse<SeatAllocationResponse>>(`${this.base}/${examId}/schedules/${scheduleId}/shifts/${shiftId}/allocations`, req)
      .pipe(map(res => res.data));
  }

  listAllocations(examId: string, scheduleId: string, shiftId: string): Observable<SeatAllocationResponse[]> {
    return this.http
      .get<ApiResponse<SeatAllocationResponse[]>>(`${this.base}/${examId}/schedules/${scheduleId}/shifts/${shiftId}/allocations`)
      .pipe(map(res => res.data));
  }
}
