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
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatDividerModule } from '@angular/material/divider';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationPanelComponent } from '../notification-panel/notification-panel.component';

interface ResultData {
  examName: string;
  candidateName: string;
  totalScore: number;
  maxScore: number;
  overallRank: number;
  overallPercentile: number;
  sections: SectionScore[];
  pdfDownloadUrl: string;
}

interface SectionScore {
  sectionName: string;
  score: number;
  maxScore: number;
  percentage: number;
  topics: TopicScore[];
}

interface TopicScore {
  topicName: string;
  score: number;
  maxScore: number;
}

@Component({
  selector: 'app-result-page',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatDividerModule,
    NotificationPanelComponent
  ],
  templateUrl: './result-page.component.html',
  styleUrls: ['./result-page.component.scss']
})
export class ResultPageComponent implements OnInit {
  result: ResultData | null = null;
  isLoading = true;
  errorMessage = '';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const candidateId = this.authService.getUserId();
    if (!candidateId) {
      this.isLoading = false;
      this.errorMessage = 'Unable to identify candidate.';
      return;
    }

    this.http.get<ResultData>(`/api/v1/results/${candidateId}`).subscribe({
      next: (data) => {
        this.result = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Failed to load results.';
      }
    });
  }
}
