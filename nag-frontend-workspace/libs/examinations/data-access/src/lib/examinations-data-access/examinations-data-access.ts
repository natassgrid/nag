import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';

// ============================================================================
// 1. COMMON / API RESPONSE MODELS
// ============================================================================

export interface ApiResponse<T> {
  status?: string;
  message?: string;
  data: T;
  timestamp?: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ============================================================================
// 2. EXAMINATION MODELS
// ============================================================================

export type ExamStatus = 'DRAFT' | 'PUBLISHED' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface ExamSection {
  name: string;
  questionCount: number;
  marksPerQuestion: number;
}

export interface ExaminationResponse {
  id: string;
  name: string;
  code?: string;
  conductingAuthority?: string;
  category?: string;
  examinationType?: string;
  academicYear?: string;
  examinationMode?: string;
  durationMinutes: number;
  totalMarks: number;
  negativeMarkingEnabled: boolean;
  negativeMarkingValue: number;
  navigationPolicy: string;
  calculatorPolicy: string;
  reviewFlagEnabled: boolean;
  isPractice?: boolean;
  sections: ExamSection[];
  status: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateExamRequest {
  name: string;
  code?: string;
  conductingAuthority?: string;
  category?: string;
  examinationType?: string;
  academicYear?: string;
  examinationMode?: string;
  durationMinutes: number;
  totalMarks: number;
  negativeMarkingEnabled: boolean;
  negativeMarkingValue: number;
  navigationPolicy: string;
  calculatorPolicy: string;
  reviewFlagEnabled: boolean;
  isPractice?: boolean;
  sections: ExamSection[];
}

// ============================================================================
// 3. SCHEDULE & SHIFT MODELS
// ============================================================================

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
  createdAt?: string;
  updatedAt?: string;
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

// ============================================================================
// 4. TEST CENTRE & ALLOCATION MODELS
// ============================================================================

export interface CentreResponse {
  id: string;
  countryId?: number;
  countryName?: string;
  stateId?: number;
  stateName?: string;
  cityId?: number;
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

export interface SeatAllocationResponse {
  id: string;
  shiftId: string;
  centreId: string;
  centreName?: string;
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

// ============================================================================
// 5. GEO LOCATION MODELS
// ============================================================================

export interface GeoCountry {
  id: number;
  name: string;
  iso2: string;
  iso3: string;
  phoneCode: string;
  capital: string;
  currency: string;
  region: string;
  subregion: string;
  active: boolean;
}

export interface GeoState {
  id: number;
  name: string;
  countryId: number;
  stateCode: string;
  type: string;
  active: boolean;
}

export interface GeoCity {
  id: number;
  name: string;
  stateId: number;
  countryId: number;
  latitude: number;
  longitude: number;
  active: boolean;
}

// ============================================================================
// 6. PAPER GENERATION & LEDGER MODELS
// ============================================================================

export interface PaperSectionConfig {
  id: string;
  name: string;
  questionCount: number;
  marksPerQuestion: number;
}

export interface PaperGenerationConfig {
  examId: string;
  totalQuestions: number;
  difficultyDistribution: {
    easy: number;
    medium: number;
    hard: number;
  };
  sections: PaperSectionConfig[];
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  ledgerProofHash?: string;
}

export interface LedgerProofResult {
  proofHash: string;
  blockNumber: number;
  timestamp: string;
  merkleRoot: string;
  verified: boolean;
}

// ============================================================================
// 7. SERVICES
// ============================================================================

@Injectable({
  providedIn: 'root',
})
export class ExaminationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/examinations';

  readonly exams = signal<ExaminationResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly activeProof = signal<LedgerProofResult | null>(null);

  getExams(
    page = 0,
    size = 20,
    search?: string,
    sort?: string,
    order?: string
  ): Observable<ExaminationResponse[]> {
    this.loading.set(true);
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (search) params = params.set('search', search.trim());
    if (sort) params = params.set('sort', sort);
    if (order) params = params.set('order', order);

    return this.http
      .get<ApiResponse<any> | any>(this.baseUrl, { params })
      .pipe(
        map((res) => {
          const payload = res?.data ?? res;
          const items: ExaminationResponse[] = Array.isArray(payload?.content)
            ? payload.content
            : Array.isArray(payload)
            ? payload
            : [];
          return items;
        }),
        tap({
          next: (list) => {
            this.exams.set(list);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        })
      );
  }

  getExamsPaged(
    page = 0,
    size = 20,
    search?: string,
    sort?: string,
    order?: string
  ): Observable<PagedResponse<ExaminationResponse>> {
    this.loading.set(true);
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (search) params = params.set('search', search.trim());
    if (sort) params = params.set('sort', sort);
    if (order) params = params.set('order', order);

    return this.http
      .get<ApiResponse<any> | any>(this.baseUrl, { params })
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
            this.exams.set(paged.content);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        })
      );
  }

  getExam(id: string): Observable<ExaminationResponse> {
    return this.http
      .get<ApiResponse<ExaminationResponse> | ExaminationResponse>(`${this.baseUrl}/${id}`)
      .pipe(map((res) => ((res as any)?.data ?? res) as ExaminationResponse));
  }

  createExam(data: CreateExamRequest): Observable<ExaminationResponse> {
    return this.http
      .post<ApiResponse<ExaminationResponse> | ExaminationResponse>(this.baseUrl, data)
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ExaminationResponse),
        tap((created) => this.exams.update((list) => [created, ...list]))
      );
  }

  updateExam(id: string, data: CreateExamRequest): Observable<ExaminationResponse> {
    return this.http
      .put<ApiResponse<ExaminationResponse> | ExaminationResponse>(`${this.baseUrl}/${id}`, data)
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ExaminationResponse),
        tap((updated) =>
          this.exams.update((list) => list.map((e) => (e.id === id ? updated : e)))
        )
      );
  }

  publishExam(id: string): Observable<ExaminationResponse> {
    return this.http
      .put<ApiResponse<ExaminationResponse> | ExaminationResponse>(
        `${this.baseUrl}/${id}/publish`,
        {}
      )
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ExaminationResponse),
        tap((updated) =>
          this.exams.update((list) => list.map((e) => (e.id === id ? updated : e)))
        )
      );
  }

  generatePaper(
    config: PaperGenerationConfig
  ): Observable<{ paperId: string; proofHash: string }> {
    return this.http.post<{ paperId: string; proofHash: string }>(
      `${this.baseUrl}/papers/generate`,
      config
    );
  }

  verifyLedgerProof(proofHash: string): Observable<LedgerProofResult> {
    return this.http
      .get<LedgerProofResult>(`${this.baseUrl}/ledger/verify/${proofHash}`)
      .pipe(tap((proof) => this.activeProof.set(proof)));
  }
}

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
        tap((created) => this.schedules.update((list) => [created, ...list]))
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
            this.schedules.set(list);
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
            list.map((s) => (s.id === scheduleId ? updated : s))
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
        tap((newVersion) => this.schedules.update((list) => [newVersion, ...list]))
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
        tap((created) => this.shifts.update((list) => [...list, created]))
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
          this.shifts.update((list) => list.map((s) => (s.id === shiftId ? updated : s)))
        )
      );
  }
}

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
        tap((created) => this.centres.update((list) => [created, ...list]))
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
            this.centres.set(list);
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
            this.centres.set(paged.content);
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
          this.centres.update((list) => list.map((c) => (c.id === centreId ? updated : c)))
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

@Injectable({
  providedIn: 'root',
})
export class GeoLocationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/geo';

  getCountries(): Observable<GeoCountry[]> {
    return this.http
      .get<ApiResponse<GeoCountry[]> | GeoCountry[]>(`${this.baseUrl}/countries`)
      .pipe(
        map((res) => {
          const payload = (res as any)?.data ?? res;
          return (Array.isArray(payload) ? payload : []) as GeoCountry[];
        })
      );
  }

  getStates(countryId: number): Observable<GeoState[]> {
    return this.http
      .get<ApiResponse<GeoState[]> | GeoState[]>(
        `${this.baseUrl}/countries/${countryId}/states`
      )
      .pipe(
        map((res) => {
          const payload = (res as any)?.data ?? res;
          return (Array.isArray(payload) ? payload : []) as GeoState[];
        })
      );
  }

  getCities(stateId: number): Observable<GeoCity[]> {
    return this.http
      .get<ApiResponse<GeoCity[]> | GeoCity[]>(`${this.baseUrl}/states/${stateId}/cities`)
      .pipe(
        map((res) => {
          const payload = (res as any)?.data ?? res;
          return (Array.isArray(payload) ? payload : []) as GeoCity[];
        })
      );
  }
}
