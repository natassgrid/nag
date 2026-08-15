/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTableModule } from '@angular/material/table';
import { HttpClient } from '@angular/common/http';

interface ExamAnalytics {
  examId: string;
  examName: string;
  totalRegistered: number;
  totalAppeared: number;
  scoreDistribution: HistogramBin[];
  sectionAverages: SectionAverage[];
  top10Percentile: number;
  bottom10Percentile: number;
}

interface HistogramBin {
  rangeLabel: string;
  count: number;
  percentage: number;
}

interface SectionAverage {
  sectionName: string;
  averageScore: number;
  maxScore: number;
}

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatSelectModule,
    MatFormFieldModule,
    MatTableModule
  ],
  templateUrl: './analytics-dashboard.component.html',
  styleUrls: ['./analytics-dashboard.component.scss']
})
export class AnalyticsDashboardComponent implements OnInit {
  examList: { id: string; name: string }[] = [];
  analytics: ExamAnalytics | null = null;
  private maxCount = 1;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<{ id: string; name: string }[]>('/api/v1/admin/exams').subscribe({
      next: (exams) => this.examList = exams
    });
  }

  loadAnalytics(examId: string): void {
    this.http.get<ExamAnalytics>(`/api/v1/analytics/exams/${examId}`).subscribe({
      next: (data) => {
        this.analytics = data;
        this.maxCount = Math.max(...data.scoreDistribution.map(b => b.count), 1);
      }
    });
  }

  getBarHeight(count: number): number {
    return (count / this.maxCount) * 100;
  }
}
