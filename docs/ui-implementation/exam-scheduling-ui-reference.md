# Examination Scheduling UI — Implementation Reference

This document is a complete specification for a second agent to implement the Angular UI for the **Examination Scheduling** feature. Read every section before writing any code.

---

## 1. Workspace Context

| Item | Value |
|---|---|
| Framework | Angular 21 (standalone components, no NgModules) |
| UI Library | Angular Material 21 |
| State | RxJS Observables only — no signals, no NgRx |
| Style | SCSS |
| HTTP | `HttpClient` functional interceptor already attaches `Authorization`, `X-Tenant-Id`, `X-Request-Id` |
| Guards | `authGuard` + `roleGuard` (functional `CanActivateFn`) |
| Root | `f:\code\IdeaProjects\nag\frontend\src\app\` |

---

## 2. Existing Patterns — Follow These Exactly

### 2.1 Standalone component skeleton
```typescript
@Component({
  selector: 'app-xyz',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, /* … */],
  template: `…`,
  styles: [`…`]
})
export class XyzComponent { … }
```

### 2.2 HTTP service pattern
```typescript
@Injectable({ providedIn: 'root' })
export class XyzService {
  private readonly baseUrl = '/api/v1/examinations';
  constructor(private http: HttpClient) {}

  // Always unwrap ApiResponse<T> via map(res => res.data)
  getItems(): Observable<Item[]> {
    return this.http
      .get<ApiResponse<Item[]>>(this.baseUrl)
      .pipe(map(res => res.data));
  }
}

interface ApiResponse<T> { status: string; message: string; data: T; timestamp: string; }
```

### 2.3 PaginatedTableComponent
Already exists at `shared/components/paginated-table`. Import via:
```typescript
import { PaginatedTableComponent, PaginatedDataFetcher } from '../../../shared/components/paginated-table';
import { ColumnDef } from '../../../shared/components/paginated-table/pagination.model';
```
Provide a `fetcher: PaginatedDataFetcher<T>` that returns `Observable<PaginatedResponse<T>>`.  
Use `[actionsTemplate]="actionsTmpl"` with `<ng-template #actionsTmpl let-row>` for row actions.

### 2.4 Form dialog pattern
```typescript
@Component({ … })
export class XyzDialogComponent implements OnInit {
  form!: FormGroup;
  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<XyzDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: XyzDialogData
  ) {}
  ngOnInit() { this.form = this.fb.group({ … }); }
  save() { if (this.form.valid) this.dialogRef.close(this.form.value); }
}
```

### 2.5 Route registration pattern
Add to `frontend/src/app/features/exam/exam.routes.ts`:
```typescript
{ path: 'scheduling', loadChildren: () => import('./scheduling/scheduling.routes').then(m => m.SCHEDULING_ROUTES) }
```
Add to `app.component.ts` navItems array:
```typescript
{ label: 'Scheduling', icon: 'event', route: '/exam/scheduling', roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] }
```

### 2.6 Snackbar + reload pattern
```typescript
this.service.doAction().subscribe({
  next: () => { this.snackBar.open('Done', 'OK', { duration: 3000 }); this.table?.reload(); },
  error: (err) => this.snackBar.open(err?.error?.message ?? 'Error', 'Dismiss', { duration: 4000 })
});
```

---

## 3. Backend API Reference

Base URL: `/api/v1/examinations`  
All requests need header `X-Tenant-Id: <tenantId>` (interceptor handles `Authorization`).  
All responses are wrapped: `{ status, message, data: T }`.

### 3.1 Examination Schedule

| Method | Path | Roles | Description |
|---|---|---|---|
| `POST` | `/{examId}/schedules` | EXAM_CONTROLLER | Create DRAFT schedule |
| `GET` | `/{examId}/schedules` | EXAM_CONTROLLER, SUPER_ADMIN, SECURITY_ADMIN | List all versions |
| `GET` | `/{examId}/schedules/{scheduleId}` | same | Get one |
| `PUT` | `/{examId}/schedules/{scheduleId}/transition` | EXAM_CONTROLLER, SUPER_ADMIN, SECURITY_ADMIN | Workflow transition |
| `PUT` | `/{examId}/schedules/{scheduleId}/amend` | EXAM_CONTROLLER, SUPER_ADMIN | Amend published schedule |

**Schedule approval workflow states (in order):**
```
DRAFT → SCHEDULER_REVIEW → CONTROLLER_APPROVED → SECURITY_REVIEW → CHAIRMAN_APPROVED → PUBLISHED
Any → CANCELLED
```

**CreateScheduleRequest**
```json
{ "scheduleName": "string", "notificationNumber": "string|null",
  "examDate": "2027-01-10", "reserveDate": "2027-01-17|null",
  "timeZone": "Asia/Kolkata" }
```

**ScheduleTransitionRequest**
```json
{ "targetStatus": "SCHEDULER_REVIEW", "comment": "optional" }
```

**AmendScheduleRequest**
```json
{ "changeReason": "mandatory string", "scheduleName": "string",
  "notificationNumber": "string|null", "examDate": "2027-02-05",
  "reserveDate": null, "effectiveFrom": "2027-02-05", "timeZone": "Asia/Kolkata" }
```

**ScheduleResponse**
```typescript
interface ScheduleResponse {
  id: string; examinationId: string; scheduleName: string; scheduleVersion: number;
  notificationNumber: string | null; examDate: string; reserveDate: string | null;
  timeZone: string; status: string; changeReason: string | null;
  effectiveFrom: string | null; previousVersionId: string | null;
  createdBy: string; modifiedBy: string; approvedBy: string | null;
  approvedAt: string | null; createdAt: string; updatedAt: string;
}
```

### 3.2 Shifts

| Method | Path | Roles | Description |
|---|---|---|---|
| `POST` | `/{examId}/schedules/{scheduleId}/shifts` | EXAM_CONTROLLER | Add shift |
| `PUT` | `/{examId}/schedules/{scheduleId}/shifts/{shiftId}` | EXAM_CONTROLLER | Update shift |
| `GET` | `/{examId}/schedules/{scheduleId}/shifts` | EXAM_CONTROLLER, SUPER_ADMIN, SECURITY_ADMIN | List shifts |

**CreateShiftRequest**
```json
{ "shiftNumber": 1, "shiftName": "Morning",
  "reportingTime": "07:30", "gateClosingTime": "08:30",
  "loginStartTime": "08:45", "examStartTime": "09:00",
  "examEndTime": "12:00", "exitTime": "12:15",
  "durationMinutes": 180, "bufferMinutes": 30 }
```

**Timing invariants enforced by backend (display as validation in form):**
- reportingTime < gateClosingTime
- gateClosingTime < loginStartTime
- loginStartTime < examStartTime
- examStartTime < examEndTime
- durationMinutes == examEndTime − examStartTime (in minutes)

**ShiftResponse**
```typescript
interface ShiftResponse {
  id: string; scheduleId: string; shiftNumber: number; shiftName: string;
  reportingTime: string; gateClosingTime: string; loginStartTime: string;
  examStartTime: string; examEndTime: string; exitTime: string | null;
  durationMinutes: number; bufferMinutes: number; createdAt: string; updatedAt: string;
}
```

### 3.3 Examination Centres

| Method | Path | Roles | Description |
|---|---|---|---|
| `POST` | `/centres` | EXAM_CONTROLLER, SUPER_ADMIN | Create centre |
| `GET` | `/centres` | EXAM_CONTROLLER, SUPER_ADMIN, SECURITY_ADMIN | List (filter: `?state=` or `?city=`) |
| `GET` | `/centres/{centreId}` | same | Get one |
| `PUT` | `/centres/{centreId}/deactivate` | EXAM_CONTROLLER, SUPER_ADMIN | Deactivate |

**CreateCentreRequest**
```json
{ "region": "North", "state": "Delhi", "district": "Central Delhi",
  "city": "New Delhi", "centreName": "NDMC Centre 1",
  "building": "Block A", "floor": "Ground", "laboratoryIdentifier": "LAB-01",
  "totalCapacity": 120, "active": true }
```

**CentreResponse**
```typescript
interface CentreResponse {
  id: string; region: string; state: string; district: string; city: string;
  centreName: string; building: string; floor: string; laboratoryIdentifier: string;
  totalCapacity: number; active: boolean; createdAt: string; updatedAt: string;
}
```

### 3.4 Seat Allocation

| Method | Path | Roles | Description |
|---|---|---|---|
| `POST` | `/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations` | EXAM_CONTROLLER | Upsert allocation |
| `GET` | `/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations` | EXAM_CONTROLLER, SUPER_ADMIN, SECURITY_ADMIN | List |

**SeatAllocationRequest**
```json
{ "centreId": "uuid", "totalSeats": 100, "availableSeats": 80,
  "reservedSeats": 10, "pwdSeats": 5, "emergencyBufferSeats": 5,
  "femaleReservedSeats": 0, "specialCategorySeats": 10 }
```

---

## 4. Files to Create

All files go under `frontend/src/app/features/exam/scheduling/`.

```
scheduling/
  scheduling.routes.ts
  scheduling.service.ts
  schedule-list.component.ts        ← main page: list exams → click → view schedules
  schedule-detail.component.ts      ← schedules for one exam + shifts tab + seats tab
  schedule-form-dialog.component.ts ← create schedule
  amend-schedule-dialog.component.ts← amend published schedule
  shift-form-dialog.component.ts    ← create/edit shift with timing validation
  centre-list.component.ts          ← manage centres (separate page)
  centre-form-dialog.component.ts   ← create centre
  seat-allocation-dialog.component.ts ← allocate seats for a shift at a centre
```

---

## 5. Routes to Add

### 5.1 Add to `frontend/src/app/features/exam/exam.routes.ts`
```typescript
{
  path: 'scheduling',
  loadChildren: () => import('./scheduling/scheduling.routes').then(m => m.SCHEDULING_ROUTES)
}
```

### 5.2 `scheduling/scheduling.routes.ts`
```typescript
export const SCHEDULING_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./schedule-list.component').then(m => m.ScheduleListComponent) },
  { path: 'centres', loadComponent: () => import('./centre-list.component').then(m => m.CentreListComponent) },
  { path: ':examId', loadComponent: () => import('./schedule-detail.component').then(m => m.ScheduleDetailComponent) }
];
```

### 5.3 Add nav items to `app.component.ts` navItems array (after `Paper Generation`)
```typescript
{ label: 'Scheduling', icon: 'event', route: '/exam/scheduling', roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] },
{ label: 'Exam Centres', icon: 'location_on', route: '/exam/scheduling/centres', roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] },
```

---

## 6. Service (`scheduling.service.ts`)

```typescript
@Injectable({ providedIn: 'root' })
export class SchedulingService {
  private readonly base = '/api/v1/examinations';

  // ── Schedule ─────────────────────────────────────────
  createSchedule(examId: string, req: CreateScheduleRequest): Observable<ScheduleResponse>
  listSchedules(examId: string): Observable<ScheduleResponse[]>
  getSchedule(examId: string, scheduleId: string): Observable<ScheduleResponse>
  transitionSchedule(examId: string, scheduleId: string, req: ScheduleTransitionRequest): Observable<ScheduleResponse>
  amendSchedule(examId: string, scheduleId: string, req: AmendScheduleRequest): Observable<ScheduleResponse>

  // ── Shifts ────────────────────────────────────────────
  listShifts(examId: string, scheduleId: string): Observable<ShiftResponse[]>
  addShift(examId: string, scheduleId: string, req: CreateShiftRequest): Observable<ShiftResponse>
  updateShift(examId: string, scheduleId: string, shiftId: string, req: CreateShiftRequest): Observable<ShiftResponse>

  // ── Centres ───────────────────────────────────────────
  createCentre(req: CreateCentreRequest): Observable<CentreResponse>
  listCentres(state?: string, city?: string): Observable<CentreResponse[]>
  deactivateCentre(centreId: string): Observable<CentreResponse>

  // ── Allocations ───────────────────────────────────────
  upsertAllocation(examId: string, scheduleId: string, shiftId: string, req: SeatAllocationRequest): Observable<SeatAllocationResponse>
  listAllocations(examId: string, scheduleId: string, shiftId: string): Observable<SeatAllocationResponse[]>
}
```

All methods unwrap `ApiResponse<T>` via `.pipe(map(res => res.data))`.

---

## 7. Component Specifications

### 7.1 `ScheduleListComponent` (page)

- Route: `/exam/scheduling`
- Fetches all examinations from `ExamManagementService.getExams()` (already exists at `features/exam/exam-manage/exam-management.service.ts`)
- Displays them in `PaginatedTableComponent` with columns: Name, Code, Mode, Status, Actions
- Actions column: **View Schedules** button → `router.navigate(['/exam/scheduling', exam.id])`
- Page header with title "Examination Scheduling" and sub-title
- FAB or top-right button is NOT needed (schedules are created from the detail page)

### 7.2 `ScheduleDetailComponent` (page)

- Route: `/exam/scheduling/:examId`
- Loads exam name via `ExamManagementService` (for breadcrumb display)
- Three `MatTab`s: **Schedules**, **Shifts**, **Centres & Seats**

**Schedules tab:**
- Table with columns: Version, Name, Exam Date, Reserve Date, Notification #, Status (chip), Actions
- Status chip colors: DRAFT=orange, SCHEDULER_REVIEW=blue, CONTROLLER_APPROVED=cyan, SECURITY_REVIEW=purple, CHAIRMAN_APPROVED=teal, PUBLISHED=green, CANCELLED=red
- Actions: **Transition** button (opens `ScheduleTransitionRequest` inline select + confirm), **Amend** button (only for PUBLISHED, opens `AmendScheduleDialogComponent`), **View Shifts** (switches to Shifts tab and sets selected schedule)
- FAB "+ New Schedule" opens `ScheduleFormDialogComponent`

**Shifts tab:**
- Requires a schedule to be selected (show prompt if none selected)
- Schedule selector dropdown at the top (lists all schedules for the exam)
- Table with columns: Shift #, Name, Reporting, Gate Close, Login, Start, End, Duration (min), Actions
- Actions: Edit (opens `ShiftFormDialogComponent` pre-filled), View Seats (switches to Seats tab)
- "+ Add Shift" button opens empty `ShiftFormDialogComponent`

**Centres & Seats tab:**
- Requires a schedule AND shift to be selected
- Schedule + shift selectors at top
- Table with columns: Centre Name, City, State, Total Seats, Available, PwD, Buffer, Actions
- Actions: Edit allocation (opens `SeatAllocationDialogComponent`)
- "+ Add Allocation" button

### 7.3 `ScheduleFormDialogComponent` (dialog)

Fields:
- Schedule Name (required, text)
- Notification Number (optional, text, hint: "Government gazette reference")
- Exam Date (required, `<input matInput [matDatepicker]>`)
- Reserve Date (optional, date picker)
- Time Zone (select, default "Asia/Kolkata", options: Asia/Kolkata, Asia/Colombo, UTC)

Validation: all `Validators.required` fields. Close returns `CreateScheduleRequest`.

### 7.4 `AmendScheduleDialogComponent` (dialog)

Pre-fills from current schedule. Fields:
- Change Reason (required, textarea, hint: "Mandatory — explain why this schedule is being amended")
- Schedule Name (required)
- Notification Number (optional)
- Exam Date (required, date picker)
- Reserve Date (optional)
- Effective From (optional, date picker)
- Time Zone (select)

Close returns `AmendScheduleRequest`.

### 7.5 `ShiftFormDialogComponent` (dialog)

Fields (all time inputs use `<input matInput type="time">`):
- Shift Number (required, number, min 1)
- Shift Name (text, placeholder "Morning / Afternoon / Evening")
- Reporting Time (required)
- Gate Closing Time (required)
- Login Start Time (required)
- Exam Start Time (required)
- Exam End Time (required)
- Exit Time (optional)
- Duration (minutes) (required, number, min 1) — auto-computed from start/end, but editable
- Buffer Minutes (number, default 0)

**Client-side timing validation** (mirror backend rules — show inline `mat-error`):
```
reportingTime < gateClosingTime
gateClosingTime < loginStartTime
loginStartTime < examStartTime
examStartTime < examEndTime
durationMinutes == toMinutes(examEndTime - examStartTime)
```
Auto-compute `durationMinutes` when `examStartTime` or `examEndTime` changes.

Close returns `CreateShiftRequest`.

### 7.6 `CentreListComponent` (page)

- Route: `/exam/scheduling/centres`
- Filter bar: State (text input), City (text input), Apply/Clear buttons
- `PaginatedTableComponent` columns: Name, City, State, District, Capacity, Status (Active/Inactive chip), Actions
- Actions: Deactivate (only when active)
- FAB "+ New Centre" opens `CentreFormDialogComponent`

### 7.7 `CentreFormDialogComponent` (dialog)

Fields:
- Region, State (required), District, City (required), Centre Name (required)
- Building, Floor, Laboratory Identifier
- Total Capacity (number, min 0)
- Active (slide toggle, default true)

### 7.8 `SeatAllocationDialogComponent` (dialog)

Pre-fills from existing allocation if present. Fields:
- Centre (read-only display if editing; required select from `listCentres()` if creating)
- Total Seats, Available Seats, Reserved Seats, PwD Seats
- Emergency Buffer Seats, Female Reserved Seats, Special Category Seats
- Hint below Available Seats: "Must not be negative"

---

## 8. Status Chip CSS Classes

Add to the relevant component's `styles` array:
```scss
::ng-deep .status-draft              { background: #fff3e0 !important; color: #e65100 !important; }
::ng-deep .status-scheduler_review   { background: #e3f2fd !important; color: #1565c0 !important; }
::ng-deep .status-controller_approved{ background: #e0f7fa !important; color: #00695c !important; }
::ng-deep .status-security_review    { background: #f3e5f5 !important; color: #6a1b9a !important; }
::ng-deep .status-chairman_approved  { background: #e8f5e9 !important; color: #2e7d32 !important; }
::ng-deep .status-published          { background: #e8f5e9 !important; color: #1b5e20 !important; }
::ng-deep .status-cancelled          { background: #ffebee !important; color: #b71c1c !important; }
```

---

## 9. Workflow Transition UI

In the Schedules table, the **Transition** action should:
1. Show a small inline `<mat-select>` in the row (or open a simple confirmation dialog) pre-populated with the valid next status
2. Valid next transitions per current status:

| Current | Valid next targets (show in select) |
|---|---|
| DRAFT | SCHEDULER_REVIEW, CANCELLED |
| SCHEDULER_REVIEW | CONTROLLER_APPROVED, CANCELLED |
| CONTROLLER_APPROVED | SECURITY_REVIEW, CANCELLED |
| SECURITY_REVIEW | CHAIRMAN_APPROVED, CANCELLED |
| CHAIRMAN_APPROVED | PUBLISHED, CANCELLED |
| PUBLISHED | CANCELLED (only via Amend for other changes) |
| CANCELLED | — (no actions) |

Use a simple `MatDialog` confirmation with a `MatSelect` for target status and an optional comment `MatInput`.

---

## 10. ExamManagementService — Additional Methods Needed

The existing service at `features/exam/exam-manage/exam-management.service.ts` needs one addition (call from `ScheduleDetailComponent` to display exam name):

```typescript
getExam(id: string): Observable<ExaminationResponse> {
  return this.http
    .get<ApiResponse<ExaminationResponse>>(`${this.baseUrl}/${id}`)
    .pipe(map(res => res.data));
}
```

---

## 11. Interfaces to Declare in `scheduling.service.ts`

```typescript
export interface ScheduleResponse { /* see section 3.1 */ }
export interface ShiftResponse    { /* see section 3.2 */ }
export interface CentreResponse   { /* see section 3.3 */ }
export interface SeatAllocationResponse {
  id: string; shiftId: string; centreId: string;
  totalSeats: number; availableSeats: number; reservedSeats: number;
  pwdSeats: number; emergencyBufferSeats: number;
  femaleReservedSeats: number; specialCategorySeats: number;
  createdAt: string; updatedAt: string;
}
export interface CreateScheduleRequest { scheduleName: string; notificationNumber?: string; examDate: string; reserveDate?: string; timeZone: string; }
export interface ScheduleTransitionRequest { targetStatus: string; comment?: string; }
export interface AmendScheduleRequest { changeReason: string; scheduleName: string; notificationNumber?: string; examDate: string; reserveDate?: string; effectiveFrom?: string; timeZone: string; }
export interface CreateShiftRequest { shiftNumber: number; shiftName?: string; reportingTime: string; gateClosingTime: string; loginStartTime: string; examStartTime: string; examEndTime: string; exitTime?: string; durationMinutes: number; bufferMinutes: number; }
export interface CreateCentreRequest { region?: string; state: string; district?: string; city: string; centreName: string; building?: string; floor?: string; laboratoryIdentifier?: string; totalCapacity: number; active: boolean; }
export interface SeatAllocationRequest { centreId: string; totalSeats: number; availableSeats: number; reservedSeats: number; pwdSeats: number; emergencyBufferSeats: number; femaleReservedSeats: number; specialCategorySeats: number; }
```

---

## 12. MatDatepicker Setup

The project uses Angular Material. Date pickers need `MatDatepickerModule` + `MatNativeDateModule` in the component imports. Use ISO string format (`YYYY-MM-DD`) when sending to the API.

```typescript
// In component imports array:
MatDatepickerModule, MatNativeDateModule,

// In template:
<mat-form-field appearance="outline">
  <mat-label>Exam Date</mat-label>
  <input matInput [matDatepicker]="examDatePicker" formControlName="examDate" />
  <mat-datepicker-toggle matIconSuffix [for]="examDatePicker"></mat-datepicker-toggle>
  <mat-datepicker #examDatePicker></mat-datepicker>
</mat-form-field>
```

Format date to string before sending: `date instanceof Date ? date.toISOString().split('T')[0] : date`

---

## 13. Angular Material Modules Required

Import these in each component's `imports` array as needed:

```
CommonModule, FormsModule, ReactiveFormsModule,
MatCardModule, MatButtonModule, MatIconModule, MatTabsModule,
MatTableModule, MatPaginatorModule, MatSortModule,
MatFormFieldModule, MatInputModule, MatSelectModule,
MatDialogModule, MatSnackBarModule, MatChipsModule,
MatTooltipModule, MatProgressSpinnerModule, MatDividerModule,
MatSlideToggleModule, MatDatepickerModule, MatNativeDateModule,
RouterModule, PaginatedTableComponent
```

---

## 14. Accessibility Requirements

- All form controls must have `<mat-label>` or `aria-label`
- All icon buttons must have `matTooltip` and `aria-label`
- Status chips must include screen-reader text (use `mat-chip` with visible text, not icons only)
- Date inputs must have `aria-describedby` pointing to any hint/error element

---

## 15. Do NOT Do

- Do not create NgModules
- Do not use signals or Angular state management libraries
- Do not add new npm dependencies
- Do not modify any existing service outside adding `getExam()` to `ExamManagementService`
- Do not modify `app.routes.ts` — only modify `exam.routes.ts` and `app.component.ts`
- Do not inline large amounts of mock data — components load from the real API
