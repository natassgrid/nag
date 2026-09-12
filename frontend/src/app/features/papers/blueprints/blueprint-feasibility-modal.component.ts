/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
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

import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnChanges,
  SimpleChanges,
  ChangeDetectorRef,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import {
  PaperService,
  BlueprintFeasibilityResponse,
  BlueprintTemplateResponse,
  BlueprintRule,
  BlueprintFeasibilityRequest
} from '../paper.service';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-blueprint-feasibility-modal',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    RightDrawerComponent
  ],
  templateUrl: './blueprint-feasibility-modal.component.html',
  styleUrls: ['./blueprint-feasibility-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BlueprintFeasibilityModalComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() template?: BlueprintTemplateResponse;
  @Input() customRules?: BlueprintRule[];
  @Input() examId?: string;
  @Input() shiftId?: string;
  @Input() examName?: string;
  @Output() close = new EventEmitter<void>();

  loading = false;
  notifying = false;
  feasibility: BlueprintFeasibilityResponse | null = null;
  error: string | null = null;

  constructor(
    private paperService: PaperService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    if (this.isOpen) {
      this.runAnalysis(false);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.runAnalysis(false);
    }
  }

  get title(): string {
    if (this.template?.name) {
      return `Feasibility Analysis: ${this.template.name}`;
    }
    return 'Blueprint Sufficiency & Feasibility Analysis';
  }

  runAnalysis(notifyAdmin: boolean = false): void {
    this.loading = true;
    this.error = null;
    this.cdr.markForCheck();

    if (this.template?.id) {
      this.paperService
        .checkTemplateSufficiency(this.template.id, notifyAdmin)
        .pipe(
          catchError((err) => {
            const msg = err?.error?.detail ?? err?.error?.message ?? 'Failed to audit template sufficiency';
            this.error = msg;
            this.snackBar.open(msg, 'Dismiss', { duration: 4000 });
            return of(null);
          }),
          finalize(() => {
            this.loading = false;
            this.cdr.markForCheck();
          })
        )
        .subscribe((res) => {
          if (res) {
            this.feasibility = res;
            if (notifyAdmin && res.notificationDispatched) {
              this.snackBar.open(
                'Deficit alert dispatched to Exam Administrators and Audit Topic.',
                'OK',
                { duration: 4000 }
              );
            }
          }
        });
    } else {
      const rules = this.customRules || this.template?.rules || [];
      if (rules.length === 0) {
        this.loading = false;
        this.error = 'No blueprint rules defined to check.';
        this.cdr.markForCheck();
        return;
      }

      const request: BlueprintFeasibilityRequest = {
        examId: this.examId || this.template?.examId,
        shiftId: this.shiftId,
        blueprintRules: rules,
        notifyAdminOnDeficit: notifyAdmin
      };

      this.paperService
        .checkBlueprintSufficiency(request)
        .pipe(
          catchError((err) => {
            const msg = err?.error?.detail ?? err?.error?.message ?? 'Failed to verify blueprint sufficiency';
            this.error = msg;
            this.snackBar.open(msg, 'Dismiss', { duration: 4000 });
            return of(null);
          }),
          finalize(() => {
            this.loading = false;
            this.cdr.markForCheck();
          })
        )
        .subscribe((res) => {
          if (res) {
            this.feasibility = res;
            if (notifyAdmin && res.notificationDispatched) {
              this.snackBar.open(
                'Deficit alert dispatched to Exam Administrators and Audit Topic.',
                'OK',
                { duration: 4000 }
              );
            }
          }
        });
    }
  }

  notifyAdmin(): void {
    this.runAnalysis(true);
  }

  onClose(): void {
    this.close.emit();
  }
}
