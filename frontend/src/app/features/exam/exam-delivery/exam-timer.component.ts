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

import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-exam-timer',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="timer-container"
         [class.warning]="remainingSeconds <= 300 && remainingSeconds > 0"
         role="timer"
         aria-live="polite"
         [attr.aria-label]="'Time remaining: ' + formatTime()">
      <mat-icon>timer</mat-icon>
      <span class="timer-display">{{ formatTime() }}</span>
    </div>
  `,
  styles: [`
    .timer-container {
      display: flex;
      align-items: center;
      gap: 8px;
      font-family: monospace;
      font-size: 1.3rem;
      font-weight: 500;
      padding: 8px 16px;
      border-radius: 8px;
      background: #f5f5f5;
    }
    .timer-container.warning {
      color: #d32f2f;
      background: #ffebee;
      font-weight: 700;
    }
    .timer-display {
      min-width: 60px;
      text-align: center;
    }
  `]
})
export class ExamTimerComponent implements OnInit, OnDestroy {
  @Input() durationSeconds = 0;
  @Output() timeUp = new EventEmitter<void>();
  @Output() tick = new EventEmitter<number>();

  remainingSeconds = 0;
  private intervalId: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.remainingSeconds = this.durationSeconds;
    this.startTimer();
  }

  ngOnDestroy(): void {
    this.stopTimer();
  }

  private startTimer(): void {
    this.intervalId = setInterval(() => {
      this.remainingSeconds--;
      this.tick.emit(this.remainingSeconds);

      if (this.remainingSeconds <= 0) {
        this.stopTimer();
        this.timeUp.emit();
      }
    }, 1000);
  }

  private stopTimer(): void {
    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  formatTime(): string {
    const mins = Math.max(0, Math.floor(this.remainingSeconds / 60));
    const secs = Math.max(0, this.remainingSeconds % 60);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }
}
