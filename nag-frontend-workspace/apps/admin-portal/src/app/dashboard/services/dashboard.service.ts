import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import {
  DashboardSummary,
  DEFAULT_DASHBOARD_SUMMARY,
} from '../models/dashboard.model';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/admin/dashboard';

  /**
   * Retrieves aggregated operational dashboard metrics, KPI stats,
   * status breakdowns, system health, and audit trail for the tenant.
   */
  getDashboardSummary(tenantId?: string): Observable<DashboardSummary> {
    let params = new HttpParams();
    if (tenantId && tenantId.trim()) {
      params = params.set('tenantId', tenantId.trim());
    }

    return this.http.get<DashboardSummary>(`${this.baseUrl}/summary`, { params }).pipe(
      map((res) => ({
        ...DEFAULT_DASHBOARD_SUMMARY,
        ...res,
        kpis: { ...DEFAULT_DASHBOARD_SUMMARY.kpis, ...(res?.kpis || {}) },
        examBreakdown: { ...DEFAULT_DASHBOARD_SUMMARY.examBreakdown, ...(res?.examBreakdown || {}) },
        questionBreakdown: { ...DEFAULT_DASHBOARD_SUMMARY.questionBreakdown, ...(res?.questionBreakdown || {}) },
        evaluationBreakdown: { ...DEFAULT_DASHBOARD_SUMMARY.evaluationBreakdown, ...(res?.evaluationBreakdown || {}) },
        systemServices: res?.systemServices?.length ? res.systemServices : DEFAULT_DASHBOARD_SUMMARY.systemServices,
        recentAuditEvents: res?.recentAuditEvents?.length ? res.recentAuditEvents : DEFAULT_DASHBOARD_SUMMARY.recentAuditEvents,
      })),
      catchError((error) => {
        console.warn('DashboardService: Failed to fetch live summary from backend, using fallback metrics', error);
        return of(DEFAULT_DASHBOARD_SUMMARY);
      })
    );
  }
}
