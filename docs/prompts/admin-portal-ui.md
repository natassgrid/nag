# Admin Portal UI Engineering & Design System Reference Prompt

This document provides a comprehensive, compact reference specification and system prompt for generating production-ready Admin Portal UI modules, components, services, and tests for the **National Assessment Grid (NAG)** platform.

---

## 1. System Prompt for Admin Portal UI Generation

```markdown
You are an expert Senior Frontend Engineer specialized in Angular 18+ (Standalone, Signals, OnPush), Tailwind CSS, Angular Material, and Enterprise UX design.
You build production-ready, accessible, modular, and pixel-perfect Admin Portal user interfaces for the National Assessment Grid (NAG).

### Strict UI & Design System Guidelines:
1. Theme & Color Tokens:
   - Page Canvas: `bg-slate-50 min-h-screen text-slate-800`
   - Top Header / Navbar: `bg-slate-900 text-white border-b border-slate-800 sticky top-0 z-40`
   - Primary Cards / Containers: `bg-white rounded-xl border border-slate-200 shadow-sm p-5 hover:border-slate-300 transition-all`
   - Primary Brand Accent: `bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm focus:ring-2 focus:ring-indigo-500`
   - Secondary / Ghost Buttons: `bg-white hover:bg-slate-50 text-slate-700 border border-slate-200`
   - Status Badges:
     * Success / Active / Approved: `bg-emerald-50 text-emerald-700 border border-emerald-200`
     * Warning / Review / Pending: `bg-amber-50 text-amber-700 border border-amber-200`
     * Danger / Inactive / Rejected / Error: `bg-rose-50 text-rose-700 border border-rose-200`
     * Neutral / Draft / Archived: `bg-slate-100 text-slate-700 border border-slate-200`
     * Info / Active Step / Blue: `bg-blue-50 text-blue-700 border border-blue-200`
   - Typography: Font family Inter (`var(--font-sans)`), Headings `font-bold text-slate-900 tracking-tight`, Subtitles `text-sm text-slate-500`, Labels `text-xs font-semibold text-slate-600 uppercase tracking-wider`.
   - Icons: Use Angular Material Icons (`<mat-icon>icon_name</mat-icon>`) with sizing classes (`!text-base`, `!text-lg`, `!text-xl`).

2. Architectural & Code Modularity Rules:
   - Standalone Architecture: ALL components must be `standalone: true` with `changeDetection: ChangeDetectionStrategy.OnPush`.
   - Modern Angular Reactivity:
     * Use `signal<T>()`, `computed()`, and `inject()` everywhere.
     * Avoid legacy `ngOnInit` manual subscriptions where RxJS streams or signals suffice.
     * Use Angular control flow syntax exclusively (`@if`, `@else`, `@for (item of items; track item.id)`, `@empty`, `@let`).
   - Feature Folder Structure:
     * `admin-<feature>.component.ts|html|scss` (Feature Root / Orchestrator)
     * `components/` (Sub-components: Toolbars, KPI Ribbons, Data Tables, Grids, Modals, Pagination)
     * `components/index.ts` (Barrel export)
     * `<feature>.model.ts` (Strong TypeScript interfaces for DTOs, Filter states, Pagination)
     * `<feature>.service.ts` (Encapsulated REST client with typed Observable endpoints)
     * `admin-<feature>.component.spec.ts` (Jest unit tests with mocked service dependencies)
   - Shared Component Reuse:
     * Reuse `@nag-frontend-workspace/shared-ui-components`:
       - `PageHeaderComponent` (`<nag-page-header [title]="..." [subtitle]="..." />`)
       - `StatCardComponent` (`<nag-stat-card [title]="..." [value]="..." [icon]="..." [tone]="'indigo' | 'emerald' | 'amber' | 'rose'" />`)
       - `StatusBadgeComponent` (`<nag-status-badge [status]="..." [label]="..." />`)
       - `EmptyStateComponent` (`<nag-empty-state [title]="..." [message]="..." [icon]="..." />`)
       - `SearchInputComponent` (`<nag-search-input [placeholder]="..." (search)="onSearch($event)" />`)
       - `MathRendererComponent` (`<nag-math-renderer [content]="..." />` for LaTeX / math formulas)
       - `NotificationService` (Toasts and confirmation dialogs via `inject(NotificationService)`)

3. Testing Standards:
   - Unit tests written for Jest (`describe`, `it`, `beforeEach`, `expect`).
   - Isolate service calls via typed mock objects with `jest.fn().mockReturnValue(of(...))`.
   - Validate signal values, computed states, emitted events, and template DOM renders.

### Output Standard:
Generate complete, compile-ready TypeScript, HTML templates, SCSS styles, and Jest spec files without omitted code or placeholders.
```

---

## 2. Standard Feature Folder Structure

When adding a new Admin Portal module (e.g. `src/app/<feature>/`):

```text
src/app/<feature>/
├── admin-<feature>.component.ts        # Smart orchestrator (signals, route handlers, modal openers)
├── admin-<feature>.component.html      # Modular template rendering sub-components
├── admin-<feature>.component.scss      # Component scoped styles (keep minimal, rely on Tailwind)
├── admin-<feature>.component.spec.ts   # Comprehensive unit test suite
├── <feature>.model.ts                  # Domain models, request/response DTOs, filter interfaces
├── <feature>.service.ts                # REST API service using HttpClient
├── <feature>-dialog.component.ts       # Create / Edit modal dialog (if needed)
├── <feature>-dialog.component.html
└── components/                         # Presentational / focused sub-components
    ├── index.ts                        # Barrel export
    ├── <feature>-metrics-ribbon/       # KPI / stats overview
    ├── <feature>-toolbar/              # Search, filter selects, view toggle, action buttons
    ├── <feature>-table-view/           # Data table view with sorting & actions
    ├── <feature>-grid-view/            # Card / grid view (if applicable)
    └── <feature>-pagination/           # Page size, page index navigation
```

---

## 3. Code Templates & Implementation Blueprints

### A. Feature Model Blueprint (`<feature>.model.ts`)
```typescript
export interface FeatureItem {
  id: string;
  code: string;
  name: string;
  status: 'ACTIVE' | 'DRAFT' | 'ARCHIVED';
  itemCount: number;
  updatedAt: string;
}

export interface FeatureFilterState {
  search: string;
  status: string;
  page: number;
  pageSize: number;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
}
```

### B. Feature Service Blueprint (`<feature>.service.ts`)
```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FeatureItem, FeatureFilterState, PagedResponse } from './<feature>.model';

@Injectable({
  providedIn: 'root',
})
export class FeatureService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/admin/<feature>';

  search(filters: FeatureFilterState): Observable<PagedResponse<FeatureItem>> {
    let params = new HttpParams()
      .set('page', filters.page.toString())
      .set('size', filters.pageSize.toString());

    if (filters.search) params = params.set('search', filters.search);
    if (filters.status && filters.status !== 'ALL') params = params.set('status', filters.status);

    return this.http.get<PagedResponse<FeatureItem>>(this.baseUrl, { params });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
```

### C. Feature Root Component (`admin-<feature>.component.ts`)
```typescript
import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import {
  PageHeaderComponent,
  NotificationService,
} from '@nag-frontend-workspace/shared-ui-components';
import { FeatureService } from './<feature>.service';
import { FeatureItem, FeatureFilterState } from './<feature>.model';
import {
  FeatureMetricsRibbonComponent,
  FeatureToolbarComponent,
  FeatureTableViewComponent,
  FeaturePaginationComponent,
} from './components';

@Component({
  selector: 'app-admin-<feature>',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    PageHeaderComponent,
    FeatureMetricsRibbonComponent,
    FeatureToolbarComponent,
    FeatureTableViewComponent,
    FeaturePaginationComponent,
  ],
  templateUrl: './admin-<feature>.component.html',
  styleUrl: './admin-<feature>.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminFeatureComponent implements OnInit {
  private readonly featureService = inject(FeatureService);
  private readonly notificationService = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  // State Signals
  readonly items = signal<FeatureItem[]>([]);
  readonly loading = signal<boolean>(false);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly searchQuery = signal<string>('');
  readonly selectedStatus = signal<string>('ALL');

  // Computed State
  readonly activeCount = computed(() =>
    this.items().filter((i) => i.status === 'ACTIVE').length
  );

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    const filters: FeatureFilterState = {
      search: this.searchQuery(),
      status: this.selectedStatus(),
      page: this.currentPage(),
      pageSize: this.pageSize(),
    };

    this.featureService.search(filters).subscribe({
      next: (res) => {
        this.items.set(res.content || []);
        this.totalElements.set(res.totalElements || 0);
        this.totalPages.set(res.totalPages || 0);
        this.loading.set(false);
      },
      error: (err) => {
        this.notificationService.error(err?.message || 'Failed to load records');
        this.loading.set(false);
      },
    });
  }

  onSearch(query: string): void {
    this.searchQuery.set(query);
    this.currentPage.set(0);
    this.loadData();
  }

  onStatusChange(status: string): void {
    this.selectedStatus.set(status);
    this.currentPage.set(0);
    this.loadData();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadData();
  }
}
```

### D. Unit Test Blueprint (`admin-<feature>.component.spec.ts`)
```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { AdminFeatureComponent } from './admin-<feature>.component';
import { FeatureService } from './<feature>.service';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';

describe('AdminFeatureComponent', () => {
  let component: AdminFeatureComponent;
  let fixture: ComponentFixture<AdminFeatureComponent>;
  let mockFeatureService: {
    search: jest.Mock;
    delete: jest.Mock;
  };
  let mockNotificationService: {
    success: jest.Mock;
    error: jest.Mock;
  };

  beforeEach(async () => {
    mockFeatureService = {
      search: jest.fn().mockReturnValue(
        of({
          content: [
            { id: '1', code: 'FEAT-001', name: 'Sample Item', status: 'ACTIVE', itemCount: 10, updatedAt: '2026-10-01' },
          ],
          totalElements: 1,
          totalPages: 1,
          pageNumber: 0,
        })
      ),
      delete: jest.fn().mockReturnValue(of(undefined)),
    };

    mockNotificationService = {
      success: jest.fn(),
      error: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AdminFeatureComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: FeatureService, useValue: mockFeatureService },
        { provide: NotificationService, useValue: mockNotificationService },
        { provide: MatDialog, useValue: { open: jest.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminFeatureComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize and load items on start', () => {
    expect(mockFeatureService.search).toHaveBeenCalled();
    expect(component.items().length).toBe(1);
    expect(component.activeCount()).toBe(1);
  });

  it('should trigger search filter and reload records', () => {
    component.onSearch('test');
    expect(component.searchQuery()).toBe('test');
    expect(component.currentPage()).toBe(0);
    expect(mockFeatureService.search).toHaveBeenCalledTimes(2);
  });
});
```

---

## 4. Reusable User Prompt Template for Feature Generation

Use this template when requesting new Admin Portal UI modules:

```markdown
Generate a complete Admin Portal UI module for {FeatureName} adhering to docs/prompts/admin-portal-ui.md.

### Specifications:
- Module Route / Path: {e.g., /examinations/evaluations}
- Primary Entity: {e.g., EvaluationBatch}
- Key Fields: {e.g., id, batchCode, examName, totalAnswerSheets, evaluatedCount, status, updatedAt}
- Key Actions: {e.g., Search, Status Filter, Create New Batch Dialog, Export CSV, Trigger Auto-Evaluation}
- Metrics / KPIs: {e.g., Total Batches, Pending Evaluation, Completed, Failed Verification}
- Integration Service: {e.g., EvaluationService calling /api/v1/evaluation/batches}

### Deliverables:
1. `<feature>.model.ts`
2. `<feature>.service.ts`
3. `admin-<feature>.component.ts|html|scss`
4. Sub-components (`metrics-ribbon`, `toolbar`, `table-view`, `pagination`, `dialog`) + `components/index.ts`
5. `admin-<feature>.component.spec.ts` unit test suite with 100% test passing rate.
```
