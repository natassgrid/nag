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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  NgZone
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { of, catchError, finalize } from 'rxjs';
import { PaperService, PaperDetail } from './paper.service';
import { RightDrawerComponent } from '../../shared/components/right-drawer/right-drawer.component';

export interface TopicStat {
  topic: string;
  count: number;
  percentage: number;
}

@Component({
  selector: 'app-paper-summary-drawer',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTabsModule,
    MatDividerModule,
    RightDrawerComponent
  ],
  templateUrl: './paper-summary-drawer.component.html',
  changeDetection: ChangeDetectionStrategy.Default,
  styleUrls: ['./paper-summary-drawer.component.scss']
})
export class PaperSummaryDrawerComponent implements OnChanges {
  @Input() isOpen = false;
  @Input() paperId: string | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() approved = new EventEmitter<PaperDetail>();

  paper: PaperDetail | null = null;
  loading = false;
  approving = false;
  errorMsg: string | null = null;

  constructor(
    private paperService: PaperService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen']) {
      if (this.isOpen && this.paperId) {
        this.loadPaper();
      } else if (!this.isOpen) {
        this.paper = null;
        this.errorMsg = null;
        this.loading = false;
        this.cdr.detectChanges();
      }
    } else if (changes['paperId'] && this.paperId && this.isOpen) {
      this.loadPaper();
    }
  }

  loadPaper(): void {
    if (!this.paperId) return;

    this.loading = true;
    this.paper = null;
    this.errorMsg = null;
    this.cdr.detectChanges();

    this.paperService.getPaper(this.paperId)
      .pipe(
        catchError(err => {
          this.errorMsg = err?.error?.detail ?? err?.error?.message ?? 'Failed to load paper summary';
          return of(null);
        }),
        finalize(() => {
          this.ngZone.run(() => {
            this.loading = false;
            this.cdr.markForCheck();
            this.cdr.detectChanges();
          });
        })
      )
      .subscribe(res => {
        this.ngZone.run(() => {
          if (res) {
            this.paper = res;
          }
          this.loading = false;
          this.cdr.markForCheck();
          this.cdr.detectChanges();
        });
      });
  }

  get topicStats(): TopicStat[] {
    if (!this.paper?.topicDistribution) return [];
    const total = this.paper.totalQuestions || 1;
    return Object.entries(this.paper.topicDistribution)
      .map(([topic, count]) => ({
        topic,
        count,
        percentage: Math.round((count / total) * 100)
      }))
      .sort((a, b) => b.count - a.count);
  }

  get distinctSubjectCount(): number {
    if (!this.paper?.questions?.length) return 0;
    const subjects = new Set(this.paper.questions.map(q => q.subject).filter(Boolean));
    return subjects.size;
  }

  getDifficultyClass(diff?: string): string {
    const d = (diff || '').toUpperCase();
    if (d === 'EASY') return 'diff-easy';
    if (d === 'HARD') return 'diff-hard';
    return 'diff-medium';
  }

  onApprove(): void {
    if (!this.paper?.id || this.approving) return;

    this.approving = true;
    this.cdr.detectChanges();

    this.paperService.approvePaper(this.paper.id)
      .pipe(
        catchError(err => {
          const msg = err?.error?.detail ?? err?.error?.message ?? 'Approval and encryption failed';
          this.snackBar.open(msg, 'Dismiss', { duration: 5000, panelClass: 'snack-error' });
          return of(null);
        }),
        finalize(() => {
          this.ngZone.run(() => {
            this.approving = false;
            this.cdr.markForCheck();
            this.cdr.detectChanges();
          });
        })
      )
      .subscribe(res => {
        this.ngZone.run(() => {
          if (res) {
            this.snackBar.open(
              `Paper approved & encrypted successfully! Key: ${res.encryptionKeyId}`,
              'OK',
              { duration: 5000 }
            );
            if (this.paper) {
              this.paper.status = 'ENCRYPTED';
              this.paper.encryptionKeyId = res.encryptionKeyId;
              this.approved.emit(this.paper);
            }
          }
          this.cdr.markForCheck();
          this.cdr.detectChanges();
        });
      });
  }

  copyText(text?: string, label?: string): void {
    if (!text) return;
    navigator.clipboard.writeText(text);
    this.snackBar.open(`${label ?? 'Text'} copied to clipboard`, 'OK', { duration: 2500 });
  }

  onClose(): void {
    this.isOpen = false;
    this.paper = null;
    this.errorMsg = null;
    this.loading = false;
    this.close.emit();
    this.cdr.detectChanges();
  }
}
