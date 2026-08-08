# Examination Management & Scheduling — UI Implementation Reference

This document is a **complete specification** for an agent to:
1. Update the **Exam Create/Edit dialog** with new fields (code, conducting authority, category, type, academic year, mode).
2. Update the **ExamManagementService** and **ExaminationResponse** interface to include the new fields.
3. Implement the full **Examination Scheduling UI** (schedules, shifts, centres with cascading geo dropdowns, seat allocation).

Read every section before writing any code.

---

## 1. Workspace & Architecture Context

| Item | Value |
|---|---|
| Framework | Angular 21 (standalone components, no NgModules) |
| UI Library | Angular Material 21 |
| State | RxJS Observables only — no signals, no NgRx |
| Style | SCSS |
| HTTP | `HttpClient` + functional interceptor (auto-adds `Authorization`, `X-Tenant-Id`, `X-Request-Id`) |
| Guards | `authGuard` + `roleGuard` (functional `CanActivateFn`) |
| Root | `frontend/src/app/` |
| API proxy | Dev proxy forwards `/api` → `localhost:9000` |

---

## 2. Existing Patterns — Follow Exactly

### 2.1 Standalone component
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

### 2.2 HTTP service
```typescript
@Injectable({ providedIn: 'root' })
export class XyzService {
  private readonly baseUrl = '/api/v1/…';
  constructor(private http: HttpClient) {}

  getItems(): Observable<Item[]> {
    return this.http.get<ApiResponse<Item[]>>(this.baseUrl).pipe(map(res => res.data));
  }
}
interface ApiResponse<T> { status: string; message: string; data: T; timestamp: string; }
```

### 2.3 Form dialog
```typescript
@Component({ … })
export class XyzDialogComponent implements OnInit {
  form!: FormGroup;
  constructor(private fb: FormBuilder, private dialogRef: MatDialogRef<…>,
              @Inject(MAT_DIALOG_DATA) public data: …) {}
  ngOnInit() { this.form = this.fb.group({ … }); }
  save() { if (this.form.valid) this.dialogRef.close(this.form.value); }
}
```

### 2.4 Snackbar + reload
```typescript
this.service.doAction().subscribe({
  next: () => { this.snackBar.open('Done', 'OK', { duration: 3000 }); this.table?.reload(); },
  error: (err) => this.snackBar.open(err?.error?.message ?? 'Error', 'Dismiss', { duration: 4000 })
});
```

---

## 3. PART A — Update Exam Create/Edit Form with New Fields

### 3.1 Files to modify

| File | What to change |
|---|---|
| `features/exam/exam-manage/exam-management.service.ts` | Add new fields to `ExaminationResponse` and `CreateExamRequest` interfaces |
| `features/exam/exam-manage/exam-form-dialog.component.ts` | Add new form controls + template fields |

### 3.2 New fields on `ExaminationResponse`

Add these fields to the existing interface:
```typescript
export interface ExaminationResponse {
  // … existing fields …
  code?: string;                   // "JEE-MAIN-2027"
  conductingAuthority?: string;    // "NTA"
  category?: string;               // RECRUITMENT | ENTRANCE | CERTIFICATION | DEPARTMENTAL
  examinationType?: string;        // PRELIMINARY | MAIN | SKILL_TEST | INTERVIEW | PHYSICAL_TEST
  academicYear?: string;           // "2026-27"
  examinationMode?: string;        // CBT | OMR | HYBRID
}
```

### 3.3 New fields on `CreateExamRequest`

```typescript
export interface CreateExamRequest {
  // … existing fields …
  code?: string;
  conductingAuthority?: string;
  category?: string;
  examinationType?: string;
  academicYear?: string;
  examinationMode?: string;
}
```

### 3.4 Updated exam form template (add BEFORE the Duration/Total Marks row)

```html
<!-- ── New identity fields ──────────────────────────────── -->
<div class="form-row">
  <mat-form-field appearance="outline">
    <mat-label>Exam Code</mat-label>
    <input matInput formControlName="code" placeholder="e.g. JEE-MAIN-2027" />
  </mat-form-field>
  <mat-form-field appearance="outline">
    <mat-label>Conducting Authority</mat-label>
    <input matInput formControlName="conductingAuthority" placeholder="e.g. NTA, UPSC" />
  </mat-form-field>
</div>

<div class="form-row">
  <mat-form-field appearance="outline">
    <mat-label>Category</mat-label>
    <mat-select formControlName="category">
      <mat-option value="">None</mat-option>
      <mat-option value="RECRUITMENT">Recruitment</mat-option>
      <mat-option value="ENTRANCE">Entrance</mat-option>
      <mat-option value="CERTIFICATION">Certification</mat-option>
      <mat-option value="DEPARTMENTAL">Departmental</mat-option>
    </mat-select>
  </mat-form-field>
  <mat-form-field appearance="outline">
    <mat-label>Examination Type</mat-label>
    <mat-select formControlName="examinationType">
      <mat-option value="">None</mat-option>
      <mat-option value="PRELIMINARY">Preliminary</mat-option>
      <mat-option value="MAIN">Main</mat-option>
      <mat-option value="SKILL_TEST">Skill Test</mat-option>
      <mat-option value="INTERVIEW">Interview</mat-option>
      <mat-option value="PHYSICAL_TEST">Physical Test</mat-option>
    </mat-select>
  </mat-form-field>
</div>

<div class="form-row">
  <mat-form-field appearance="outline">
    <mat-label>Academic Year</mat-label>
    <input matInput formControlName="academicYear" placeholder="e.g. 2026-27" />
  </mat-form-field>
  <mat-form-field appearance="outline">
    <mat-label>Mode</mat-label>
    <mat-select formControlName="examinationMode">
      <mat-option value="">None</mat-option>
      <mat-option value="CBT">CBT (Computer Based)</mat-option>
      <mat-option value="OMR">OMR (Paper Based)</mat-option>
      <mat-option value="HYBRID">Hybrid</mat-option>
    </mat-select>
  </mat-form-field>
</div>
```

### 3.5 Updated form group initialization

In `ngOnInit()`, add to the `this.fb.group({…})`:
```typescript
code: [exam?.code || ''],
conductingAuthority: [exam?.conductingAuthority || ''],
category: [exam?.category || ''],
examinationType: [exam?.examinationType || ''],
academicYear: [exam?.academicYear || ''],
examinationMode: [exam?.examinationMode || ''],
```

### 3.6 Backend endpoint (already exists)

`POST /api/v1/examinations` and `PUT /api/v1/examinations/{id}` already accept the new fields — no backend change needed. The `Examination` entity has been updated in the current session.

---

## 4. PART B — Geo Location Cascade Dropdown API

### 4.1 Endpoints (no authentication required)

| Method | Path | Response |
|---|---|---|
| `GET` | `/api/v1/geo/countries` | `ApiResponse<GeoCountry[]>` |
| `GET` | `/api/v1/geo/countries/{countryId}/states` | `ApiResponse<GeoState[]>` |
| `GET` | `/api/v1/geo/states/{stateId}/cities` | `ApiResponse<GeoCity[]>` |

### 4.2 Response interfaces

```typescript
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
  type: string;    // "state" | "union territory"
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
```

### 4.3 GeoLocationService (create as new file)

```typescript
// File: frontend/src/app/features/exam/scheduling/geo-location.service.ts

@Injectable({ providedIn: 'root' })
export class GeoLocationService {
  private readonly base = '/api/v1/geo';
  constructor(private http: HttpClient) {}

  getCountries(): Observable<GeoCountry[]> {
    return this.http.get<ApiResponse<GeoCountry[]>>(`${this.base}/countries`).pipe(map(r => r.data));
  }

  getStates(countryId: number): Observable<GeoState[]> {
    return this.http.get<ApiResponse<GeoState[]>>(`${this.base}/countries/${countryId}/states`).pipe(map(r => r.data));
  }

  getCities(stateId: number): Observable<GeoCity[]> {
    return this.http.get<ApiResponse<GeoCity[]>>(`${this.base}/states/${stateId}/cities`).pipe(map(r => r.data));
  }
}
```

### 4.4 Cascading dropdown pattern in a dialog

```typescript
// In component class:
countries: GeoCountry[] = [];
states: GeoState[] = [];
cities: GeoCity[] = [];

ngOnInit() {
  this.geoService.getCountries().subscribe(c => this.countries = c);

  // When country changes → reload states, clear city
  this.form.get('countryId')!.valueChanges.subscribe(countryId => {
    this.states = [];
    this.cities = [];
    this.form.patchValue({ stateId: null, cityId: null, state: '', city: '' });
    if (countryId) {
      this.geoService.getStates(countryId).subscribe(s => this.states = s);
    }
  });

  // When state changes → reload cities, set state name
  this.form.get('stateId')!.valueChanges.subscribe(stateId => {
    this.cities = [];
    this.form.patchValue({ cityId: null, city: '' });
    if (stateId) {
      const selectedState = this.states.find(s => s.id === stateId);
      if (selectedState) this.form.patchValue({ state: selectedState.name });
      this.geoService.getCities(stateId).subscribe(c => this.cities = c);
    }
  });

  // When city changes → set city name
  this.form.get('cityId')!.valueChanges.subscribe(cityId => {
    if (cityId) {
      const selectedCity = this.cities.find(c => c.id === cityId);
      if (selectedCity) this.form.patchValue({ city: selectedCity.name });
    }
  });
}
```

```html
<!-- Template -->
<mat-form-field appearance="outline">
  <mat-label>Country</mat-label>
  <mat-select formControlName="countryId">
    <mat-option *ngFor="let c of countries" [value]="c.id">{{ c.name }}</mat-option>
  </mat-select>
</mat-form-field>

<mat-form-field appearance="outline">
  <mat-label>State / UT</mat-label>
  <mat-select formControlName="stateId" [disabled]="!states.length">
    <mat-option *ngFor="let s of states" [value]="s.id">{{ s.name }}</mat-option>
  </mat-select>
</mat-form-field>

<mat-form-field appearance="outline">
  <mat-label>City</mat-label>
  <mat-select formControlName="cityId" [disabled]="!cities.length">
    <mat-option *ngFor="let c of cities" [value]="c.id">{{ c.name }}</mat-option>
  </mat-select>
</mat-form-field>
```

---

## 5. PART C — Examination Centre Create/Edit (Updated)

### 5.1 CreateCentreRequest (updated)

```typescript
export interface CreateCentreRequest {
  countryId?: number;       // geo_country.id — from Country dropdown
  stateId?: number;         // geo_state.id — from State dropdown (filtered by country)
  cityId?: number;          // geo_city.id — from City dropdown (filtered by state)
  region?: string;          // optional free-text region name
  state: string;            // denormalized state name (auto-set from dropdown selection)
  district?: string;        // optional free-text district
  city: string;             // denormalized city name (auto-set from dropdown selection)
  centreName: string;
  building?: string;
  floor?: string;
  laboratoryIdentifier?: string;
  totalCapacity: number;
  active: boolean;
}
```

### 5.2 CentreResponse (updated)

```typescript
export interface CentreResponse {
  id: string;
  countryId?: number;
  stateId?: number;
  cityId?: number;
  countryName?: string;     // resolved by backend
  stateName?: string;       // resolved by backend
  cityName?: string;        // resolved by backend
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
```

### 5.3 Centre Form Dialog — Complete Template

The `CentreFormDialogComponent` should have these form fields in order:
1. **Country** — `<mat-select>` populated from `GeoLocationService.getCountries()`
2. **State / UT** — `<mat-select>` cascading from country selection
3. **City** — `<mat-select>` cascading from state selection
4. **District** — optional free-text input
5. **Centre Name** — required text input
6. **Building** — optional text
7. **Floor** — optional text
8. **Laboratory Identifier** — optional text (e.g. "LAB-A")
9. **Total Capacity** — number input, min 0
10. **Active** — `<mat-slide-toggle>`, default true

Hidden fields auto-set by cascade logic: `state` (string), `city` (string).

When editing, pre-select the stored `countryId` → load states → pre-select `stateId` → load cities → pre-select `cityId`.

### 5.4 Centre API endpoints

| Method | Path | Roles | Description |
|---|---|---|---|
| `POST` | `/api/v1/examinations/centres` | EXAM_CONTROLLER, SUPER_ADMIN | Create |
| `GET` | `/api/v1/examinations/centres` | EXAM_CONTROLLER, SUPER_ADMIN, SECURITY_ADMIN | List (filter: `?state=&city=`) |
| `GET` | `/api/v1/examinations/centres/{centreId}` | same | Get one |
| `PUT` | `/api/v1/examinations/centres/{centreId}/deactivate` | EXAM_CONTROLLER, SUPER_ADMIN | Deactivate |

---

## 6. PART D — Examination Scheduling UI

Refer to `docs/ui-implementation/exam-scheduling-ui-reference.md` for the full scheduling spec. This section adds clarifications on integration.

### 6.1 Route registration

Add to `frontend/src/app/features/exam/exam.routes.ts`:
```typescript
{
  path: 'scheduling',
  loadChildren: () => import('./scheduling/scheduling.routes').then(m => m.SCHEDULING_ROUTES)
}
```

### 6.2 Nav items to add in `app.component.ts`

After `{ label: 'Paper Generation', … }`:
```typescript
{ label: 'Scheduling', icon: 'event', route: '/exam/scheduling', roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] },
{ label: 'Exam Centres', icon: 'location_on', route: '/exam/scheduling/centres', roles: ['EXAM_CONTROLLER', 'SUPER_ADMIN'] },
```

### 6.3 Scheduling routes

```typescript
// frontend/src/app/features/exam/scheduling/scheduling.routes.ts
export const SCHEDULING_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./schedule-list.component').then(m => m.ScheduleListComponent) },
  { path: 'centres', loadComponent: () => import('./centre-list.component').then(m => m.CentreListComponent) },
  { path: ':examId', loadComponent: () => import('./schedule-detail.component').then(m => m.ScheduleDetailComponent) }
];
```

---

## 7. Files to Create (Full List)

```
frontend/src/app/features/exam/scheduling/
  scheduling.routes.ts
  scheduling.service.ts              ← schedules + shifts + allocations
  geo-location.service.ts            ← country/state/city cascade
  schedule-list.component.ts         ← select exam → navigate to detail
  schedule-detail.component.ts       ← tabs: Schedules | Shifts | Centres & Seats
  schedule-form-dialog.component.ts  ← create schedule
  amend-schedule-dialog.component.ts ← amend published schedule
  shift-form-dialog.component.ts     ← create/edit shift with timing validation
  centre-list.component.ts           ← manage centres (separate page)
  centre-form-dialog.component.ts    ← create/edit centre with cascading geo dropdowns
  seat-allocation-dialog.component.ts← allocate seats for a shift at a centre
```

---

## 8. Scheduling API Reference (Full)

Base: `/api/v1/examinations`

### 8.1 Schedules

| Method | Path | Description |
|---|---|---|
| `POST` | `/{examId}/schedules` | Create DRAFT schedule |
| `GET` | `/{examId}/schedules` | List all versions |
| `GET` | `/{examId}/schedules/{scheduleId}` | Get one |
| `PUT` | `/{examId}/schedules/{scheduleId}/transition` | Workflow transition |
| `PUT` | `/{examId}/schedules/{scheduleId}/amend` | Amend published |

**Workflow FSM:**
```
DRAFT → SCHEDULER_REVIEW → CONTROLLER_APPROVED → SECURITY_REVIEW → CHAIRMAN_APPROVED → PUBLISHED
Any → CANCELLED
```

**CreateScheduleRequest:**
```json
{ "scheduleName": "Phase 1", "notificationNumber": "F.No.12/2027",
  "examDate": "2027-01-10", "reserveDate": "2027-01-17", "timeZone": "Asia/Kolkata" }
```

**ScheduleTransitionRequest:**
```json
{ "targetStatus": "SCHEDULER_REVIEW", "comment": "Ready for review" }
```

**AmendScheduleRequest:**
```json
{ "changeReason": "Date conflict with national holiday", "scheduleName": "Phase 1 (Revised)",
  "notificationNumber": "F.No.12/2027-Rev1", "examDate": "2027-02-05",
  "reserveDate": null, "effectiveFrom": "2027-02-01", "timeZone": "Asia/Kolkata" }
```

**ScheduleResponse:**
```typescript
interface ScheduleResponse {
  id: string; examinationId: string; scheduleName: string; scheduleVersion: number;
  notificationNumber?: string; examDate: string; reserveDate?: string;
  timeZone: string; status: string; changeReason?: string;
  effectiveFrom?: string; previousVersionId?: string;
  createdBy?: string; modifiedBy?: string; approvedBy?: string;
  approvedAt?: string; createdAt: string; updatedAt: string;
}
```

### 8.2 Shifts

| Method | Path | Description |
|---|---|---|
| `POST` | `/{examId}/schedules/{scheduleId}/shifts` | Add shift |
| `PUT` | `/{examId}/schedules/{scheduleId}/shifts/{shiftId}` | Update shift |
| `GET` | `/{examId}/schedules/{scheduleId}/shifts` | List shifts |

**CreateShiftRequest:**
```json
{ "shiftNumber": 1, "shiftName": "Morning",
  "reportingTime": "07:30", "gateClosingTime": "08:30",
  "loginStartTime": "08:45", "examStartTime": "09:00",
  "examEndTime": "12:00", "exitTime": "12:15",
  "durationMinutes": 180, "bufferMinutes": 30 }
```

**Timing invariants (validate client-side + backend enforces):**
- reportingTime < gateClosingTime
- gateClosingTime < loginStartTime
- loginStartTime < examStartTime
- examStartTime < examEndTime
- durationMinutes == (examEndTime − examStartTime) in minutes

**ShiftResponse:**
```typescript
interface ShiftResponse {
  id: string; scheduleId: string; shiftNumber: number; shiftName?: string;
  reportingTime: string; gateClosingTime: string; loginStartTime: string;
  examStartTime: string; examEndTime: string; exitTime?: string;
  durationMinutes: number; bufferMinutes: number;
  createdAt: string; updatedAt: string;
}
```

### 8.3 Seat Allocation

| Method | Path | Description |
|---|---|---|
| `POST` | `/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations` | Upsert |
| `GET` | `/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations` | List |

**SeatAllocationRequest:**
```json
{ "centreId": "uuid", "totalSeats": 100, "availableSeats": 80,
  "reservedSeats": 10, "pwdSeats": 5, "emergencyBufferSeats": 5,
  "femaleReservedSeats": 0, "specialCategorySeats": 10 }
```

**SeatAllocationResponse:**
```typescript
interface SeatAllocationResponse {
  id: string; shiftId: string; centreId: string;
  totalSeats: number; availableSeats: number; reservedSeats: number;
  pwdSeats: number; emergencyBufferSeats: number;
  femaleReservedSeats: number; specialCategorySeats: number;
  createdAt: string; updatedAt: string;
}
```

---

## 9. Workflow Transition UI Logic

Valid next targets per current status:

| Current | Valid targets (show in select) |
|---|---|
| DRAFT | SCHEDULER_REVIEW, CANCELLED |
| SCHEDULER_REVIEW | CONTROLLER_APPROVED, CANCELLED |
| CONTROLLER_APPROVED | SECURITY_REVIEW, CANCELLED |
| SECURITY_REVIEW | CHAIRMAN_APPROVED, CANCELLED |
| CHAIRMAN_APPROVED | PUBLISHED, CANCELLED |
| PUBLISHED | CANCELLED (use Amend button for content changes) |
| CANCELLED | — (no actions) |

---

## 10. Status Chip CSS

```scss
::ng-deep .status-draft               { background: #fff3e0 !important; color: #e65100 !important; }
::ng-deep .status-scheduler_review     { background: #e3f2fd !important; color: #1565c0 !important; }
::ng-deep .status-controller_approved  { background: #e0f7fa !important; color: #00695c !important; }
::ng-deep .status-security_review      { background: #f3e5f5 !important; color: #6a1b9a !important; }
::ng-deep .status-chairman_approved    { background: #e8f5e9 !important; color: #2e7d32 !important; }
::ng-deep .status-published            { background: #e8f5e9 !important; color: #1b5e20 !important; }
::ng-deep .status-cancelled            { background: #ffebee !important; color: #b71c1c !important; }
```

---

## 11. Angular Material Modules Required

Import per component as needed:
```
CommonModule, FormsModule, ReactiveFormsModule, RouterModule,
MatCardModule, MatButtonModule, MatIconModule, MatTabsModule,
MatTableModule, MatPaginatorModule, MatSortModule,
MatFormFieldModule, MatInputModule, MatSelectModule,
MatDialogModule, MatSnackBarModule, MatChipsModule,
MatTooltipModule, MatProgressSpinnerModule, MatDividerModule,
MatSlideToggleModule, MatDatepickerModule, MatNativeDateModule,
PaginatedTableComponent
```

---

## 12. DatePicker Notes

```typescript
// Import in component:
MatDatepickerModule, MatNativeDateModule

// Template:
<mat-form-field appearance="outline">
  <mat-label>Exam Date</mat-label>
  <input matInput [matDatepicker]="dp" formControlName="examDate" />
  <mat-datepicker-toggle matIconSuffix [for]="dp"></mat-datepicker-toggle>
  <mat-datepicker #dp></mat-datepicker>
</mat-form-field>

// Convert to ISO before sending:
const isoDate = date instanceof Date ? date.toISOString().split('T')[0] : date;
```

---

## 13. ExamManagementService — Addition Needed

Add `getExam(id)` if not already present:
```typescript
getExam(id: string): Observable<ExaminationResponse> {
  return this.http.get<ApiResponse<ExaminationResponse>>(`${this.baseUrl}/${id}`)
    .pipe(map(res => res.data));
}
```

---

## 14. Seed Data Available for Testing

The backend has seeded data for dev:
- **Country:** India (id=101)
- **States/UTs:** 36 entries (ids 4023–4058), including Delhi, Maharashtra, Karnataka, Tamil Nadu, UP, Rajasthan, etc.
- **Cities:** 47 entries (ids 50001–50047), including New Delhi, Mumbai, Pune, Bengaluru, Chennai, Hyderabad, Lucknow, Jaipur, Kolkata, etc.

Cascade: Select India → shows 36 states → select Maharashtra → shows Mumbai, Pune, Nagpur, Nashik.

---

## 15. Accessibility

- All form controls must have `<mat-label>` or `aria-label`
- All icon buttons must have `matTooltip` and `aria-label`
- Status chips must have visible text (not icon-only)
- Cascading selects must show "Select country first" placeholder when disabled

---

## 16. Do NOT Do

- Do not create NgModules
- Do not use signals or state management libraries
- Do not add npm dependencies
- Do not modify `app.routes.ts` — only modify `exam.routes.ts` and `app.component.ts`
- Do not modify backend code
- Do not create mock/stub data in the frontend — always call the real API
- Do not use `::ng-deep` outside component `styles` arrays
