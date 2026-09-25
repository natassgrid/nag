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
// 6. QUESTION PAPER & BLUEPRINT MODELS
// ============================================================================

export interface BlueprintRule {
  subject: string;
  topic: string;
  difficulty?: string;
  cognitiveLevel?: string;
  questionType?: string;
  targetCount?: number;
  questionCount?: number;
}

export interface PaperSummary {
  id: string;
  paperId?: string;
  name: string;
  examId?: string;
  examName?: string;
  shiftId?: string;
  shiftName?: string;
  status: string;
  isPractice?: boolean;
  totalMarks?: number;
  totalQuestions?: number;
  difficultyScore?: number;
  generatedBy?: string;
  approvedBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface QuestionDetail {
  questionId: string;
  content: string;
  answerKey: string;
  subject: string;
  topic: string;
  difficulty: string;
  cognitiveLevel: string;
  orderIndex: number;
  marks: number;
  negativeMarks: number;
  explanation?: string;
  options?: any[];
  usageCount?: number;
  lastUsedAt?: string;
}

export interface PaperDetail {
  id?: string;
  paperId?: string;
  name?: string;
  examId?: string;
  examName?: string;
  shiftId?: string;
  shiftName?: string;
  status?: string;
  isPractice?: boolean;
  totalMarks?: number;
  difficultyScore?: number;
  encryptedPackageRef?: string;
  encryptionKeyId?: string;
  generatedBy?: string;
  createdAt?: string;
  updatedAt?: string;
  totalQuestions?: number;
  topicDistribution?: Record<string, number>;
  questions?: QuestionDetail[];
  paperDefinitionJson?: string;
}

export interface PaperGenerationRequest {
  examId: string;
  shiftId: string;
  name?: string;
  paperName?: string;
  isPractice?: boolean;
  blueprintRules: BlueprintRule[];
}

export interface PaperGenerationResponse {
  paperId: string;
  name?: string;
  status: string;
  isPractice?: boolean;
  message: string;
}

export interface PaperApprovalResponse {
  paperId: string;
  name?: string;
  status: string;
  isPractice?: boolean;
  encryptionKeyId?: string;
  message: string;
}

export interface PaperTranslateRequest {
  targetLanguage?: string;
  sourceLanguage?: string;
  targetStatus?: string;
  overwriteExisting?: boolean;
  maxConcurrency?: number;
  throttleDelayMs?: number;
}

export interface PaperTranslateResponse {
  jobId: string;
  paperId: string;
  status: string;
  sourceLanguage: string;
  targetLanguage: string;
  targetStatus: string;
  overwriteExisting: boolean;
  totalQuestions: number;
  processedQuestions: number;
  successfulQuestions: number;
  failedQuestions: number;
  progressPercentage: number;
  errorMessage?: string;
  message?: string;
  createdAt?: string;
  completedAt?: string;
}

export interface BlueprintTemplateRequest {
  name: string;
  description?: string;
  examId?: string;
  rules: BlueprintRule[];
}

export interface BlueprintTemplateResponse {
  id: string;
  name: string;
  description?: string;
  examId?: string;
  examName?: string;
  rules: BlueprintRule[];
  totalQuestions?: number;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface GapDetail {
  subject?: string;
  topic?: string;
  difficulty?: string;
  cognitiveLevel?: string;
  needed?: number;
  available?: number;
  deficit?: number;
  message?: string;
}

export interface RuleFeasibility {
  subject: string;
  topic: string;
  difficulty?: string;
  cognitiveLevel?: string;
  requested?: number;
  available?: number;
  needed?: number;
  surplus?: number;
  sufficient?: boolean;
  status?: string;
  deficit?: number;
  targetCount?: number;
  questionCount?: number;
}

export interface BlueprintFeasibilityRequest {
  examId?: string;
  shiftId?: string;
  isPractice?: boolean;
  blueprintRules?: BlueprintRule[];
  rules?: BlueprintRule[];
  notifyAdminOnDeficit?: boolean;
}

export interface BlueprintFeasibilityResponse {
  feasible: boolean;
  totalRequested?: number;
  totalAvailable?: number;
  totalQuestionsNeeded?: number;
  totalQuestionsAvailable?: number;
  deficitRuleCount?: number;
  summary?: string;
  checkedAt?: string;
  rules?: RuleFeasibility[];
  ruleDetails?: RuleFeasibility[];
  insufficientRules?: RuleFeasibility[];
  gaps?: GapDetail[];
  overallSufficiency?: number;
  notificationDispatched?: boolean;
}

export interface PaperListParams {
  page?: number;
  size?: number;
  sort?: string;
  order?: string;
  search?: string;
  status?: string;
  examId?: string;
  shiftId?: string;
  isPractice?: boolean;
}

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
            this.exams.set(list || []);
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
            this.exams.set(paged.content || []);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        })
      );
  }

  getExam(examId: string): Observable<ExaminationResponse> {
    return this.http
      .get<ApiResponse<ExaminationResponse> | ExaminationResponse>(`${this.baseUrl}/${examId}`)
      .pipe(map((res) => ((res as any)?.data ?? res) as ExaminationResponse));
  }

  createExam(req: CreateExamRequest): Observable<ExaminationResponse> {
    return this.http
      .post<ApiResponse<ExaminationResponse> | ExaminationResponse>(this.baseUrl, req)
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ExaminationResponse),
        tap((created) => this.exams.update((list) => [created, ...(list || [])]))
      );
  }

  updateExam(examId: string, req: CreateExamRequest): Observable<ExaminationResponse> {
    return this.http
      .put<ApiResponse<ExaminationResponse> | ExaminationResponse>(`${this.baseUrl}/${examId}`, req)
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ExaminationResponse),
        tap((updated) =>
          this.exams.update((list) =>
            (list || []).map((e) => (e.id === examId ? updated : e))
          )
        )
      );
  }

  publishExam(examId: string): Observable<ExaminationResponse> {
    return this.http
      .post<ApiResponse<ExaminationResponse> | ExaminationResponse>(
        `${this.baseUrl}/${examId}/publish`,
        {}
      )
      .pipe(
        map((res) => ((res as any)?.data ?? res) as ExaminationResponse),
        tap((updated) =>
          this.exams.update((list) =>
            (list || []).map((e) => (e.id === examId ? updated : e))
          )
        )
      );
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

@Injectable({
  providedIn: 'root',
})
export class PaperService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/papers';
  private readonly templateBaseUrl = '/api/v1/papers/blueprint-templates';

  readonly papers = signal<PaperSummary[]>([]);
  readonly loading = signal<boolean>(false);

  getPapers(params?: PaperListParams): Observable<PagedResponse<PaperSummary>> {
    this.loading.set(true);
    let httpParams = new HttpParams();

    if (params) {
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
      if (params.sort) httpParams = httpParams.set('sort', params.sort);
      if (params.order) httpParams = httpParams.set('order', params.order);
      if (params.search) httpParams = httpParams.set('search', params.search.trim());
      if (params.status) httpParams = httpParams.set('status', params.status);
      if (params.examId) httpParams = httpParams.set('examId', params.examId);
      if (params.shiftId) httpParams = httpParams.set('shiftId', params.shiftId);
      if (params.isPractice !== undefined)
        httpParams = httpParams.set('isPractice', params.isPractice.toString());
    }

    return this.http.get<any>(this.baseUrl, { params: httpParams }).pipe(
      map((res) => {
        const payload = res?.data ?? res;
        const items = payload?.content || (Array.isArray(payload) ? payload : []);
        const normalized: PaperSummary[] = items.map((p: any) => ({
          ...p,
          paperId: p.paperId || p.id,
        }));
        return {
          content: normalized,
          totalElements: payload?.totalElements ?? normalized.length,
          totalPages: payload?.totalPages ?? 1,
          size: payload?.size ?? (params?.size || 10),
          number: payload?.number ?? (params?.page || 0),
        };
      }),
      tap({
        next: (paged) => {
          this.papers.set(paged.content || []);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      })
    );
  }

  getPaper(paperId: string): Observable<PaperDetail> {
    return this.http
      .get<ApiResponse<PaperDetail> | PaperDetail>(`${this.baseUrl}/${paperId}`)
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperDetail));
  }

  generatePaper(request: PaperGenerationRequest): Observable<PaperGenerationResponse> {
    return this.http
      .post<ApiResponse<PaperGenerationResponse> | PaperGenerationResponse>(
        `${this.baseUrl}/generate`,
        request
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperGenerationResponse));
  }

  approvePaper(paperId: string): Observable<PaperApprovalResponse> {
    return this.http
      .post<ApiResponse<PaperApprovalResponse> | PaperApprovalResponse>(
        `${this.baseUrl}/${paperId}/approve`,
        {}
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperApprovalResponse));
  }

  publishPaper(paperId: string): Observable<PaperApprovalResponse> {
    return this.http
      .post<ApiResponse<PaperApprovalResponse> | PaperApprovalResponse>(
        `${this.baseUrl}/${paperId}/publish`,
        {}
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperApprovalResponse));
  }

  startTranslation(
    paperId: string,
    req: PaperTranslateRequest
  ): Observable<PaperTranslateResponse> {
    return this.http
      .post<ApiResponse<PaperTranslateResponse> | PaperTranslateResponse>(
        `${this.baseUrl}/${paperId}/translate`,
        req
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperTranslateResponse));
  }

  getTranslationStatus(paperId: string): Observable<PaperTranslateResponse> {
    return this.http
      .get<ApiResponse<PaperTranslateResponse> | PaperTranslateResponse>(
        `${this.baseUrl}/${paperId}/translation-status`
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperTranslateResponse));
  }

  getTranslationJob(jobId: string): Observable<PaperTranslateResponse> {
    return this.http
      .get<ApiResponse<PaperTranslateResponse> | PaperTranslateResponse>(
        `${this.baseUrl}/translations/${jobId}`
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as PaperTranslateResponse));
  }

  // --- Blueprint Templates ---

  createTemplate(req: BlueprintTemplateRequest): Observable<BlueprintTemplateResponse> {
    return this.http
      .post<ApiResponse<BlueprintTemplateResponse> | BlueprintTemplateResponse>(
        this.templateBaseUrl,
        req
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as BlueprintTemplateResponse));
  }

  listTemplates(examId?: string): Observable<BlueprintTemplateResponse[]> {
    let params = new HttpParams();
    if (examId) {
      params = params.set('examId', examId);
    }
    return this.http
      .get<ApiResponse<BlueprintTemplateResponse[]> | BlueprintTemplateResponse[]>(
        this.templateBaseUrl,
        { params }
      )
      .pipe(
        map((res) => {
          const payload = (res as any)?.data ?? res;
          return (Array.isArray(payload) ? payload : []) as BlueprintTemplateResponse[];
        })
      );
  }

  getTemplate(templateId: string): Observable<BlueprintTemplateResponse> {
    return this.http
      .get<ApiResponse<BlueprintTemplateResponse> | BlueprintTemplateResponse>(
        `${this.templateBaseUrl}/${templateId}`
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as BlueprintTemplateResponse));
  }

  updateTemplate(
    templateId: string,
    req: BlueprintTemplateRequest
  ): Observable<BlueprintTemplateResponse> {
    return this.http
      .put<ApiResponse<BlueprintTemplateResponse> | BlueprintTemplateResponse>(
        `${this.templateBaseUrl}/${templateId}`,
        req
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as BlueprintTemplateResponse));
  }

  deleteTemplate(templateId: string): Observable<void> {
    return this.http.delete<void>(`${this.templateBaseUrl}/${templateId}`);
  }

  checkFeasibility(req: BlueprintFeasibilityRequest): Observable<BlueprintFeasibilityResponse> {
    const payload = {
      examId: req.examId,
      shiftId: req.shiftId,
      isPractice: req.isPractice,
      blueprintRules: req.blueprintRules || req.rules || [],
      notifyAdminOnDeficit: req.notifyAdminOnDeficit ?? true,
    };
    return this.http
      .post<ApiResponse<BlueprintFeasibilityResponse> | BlueprintFeasibilityResponse>(
        `${this.baseUrl}/blueprints/check-sufficiency`,
        payload
      )
      .pipe(map((res) => ((res as any)?.data ?? res) as BlueprintFeasibilityResponse));
  }

  checkTemplateSufficiency(templateId: string, notifyAdmin = false): Observable<any> {
    return this.http.post<any>(
      `${this.templateBaseUrl}/${templateId}/check-sufficiency`,
      {},
      { params: new HttpParams().set('notifyAdmin', notifyAdmin.toString()) }
    );
  }

  checkBlueprintSufficiency(request: any): Observable<any> {
    const payload = {
      examId: request.examId,
      shiftId: request.shiftId,
      isPractice: request.isPractice,
      blueprintRules: request.blueprintRules || request.rules || [],
      notifyAdminOnDeficit: request.notifyAdminOnDeficit ?? true,
    };
    return this.http.post<any>(`${this.baseUrl}/blueprints/check-sufficiency`, payload);
  }
}
