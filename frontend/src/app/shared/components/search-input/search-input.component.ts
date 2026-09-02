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

import { Component, ChangeDetectionStrategy, input, output, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-search-input',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="search-input-wrapper">
      <mat-form-field appearance="outline" class="search-field">
        <mat-icon matPrefix class="search-icon">search</mat-icon>
        <input
          matInput
          type="text"
          [placeholder]="placeholder()"
          [ngModel]="query()"
          (ngModelChange)="onModelChange($event)"
          [attr.aria-label]="placeholder()"
        />
        @if (query()) {
          <button
            mat-icon-button
            matSuffix
            type="button"
            class="clear-btn"
            aria-label="Clear search"
            (click)="clearSearch()"
          >
            <mat-icon class="clear-icon">close</mat-icon>
          </button>
        }
      </mat-form-field>
    </div>
  `,
  styles: [`
    .search-input-wrapper {
      display: inline-flex;
      width: 100%;
      max-width: 360px;
    }

    .search-field {
      width: 100%;
    }

    .search-icon {
      color: #94A3B8;
      font-size: 20px;
      width: 20px;
      height: 20px;
      margin-right: 4px;
    }

    .clear-btn {
      width: 24px !important;
      height: 24px !important;
      line-height: 24px !important;
      padding: 0 !important;
      margin-right: 4px;

      .clear-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
        line-height: 16px;
        color: #94A3B8;
      }

      &:hover .clear-icon {
        color: #475569;
      }
    }
  `]
})
export class SearchInputComponent implements OnDestroy {
  placeholder = input<string>('Search...');
  debounce = input<number>(300);

  searchChange = output<string>();

  query = signal<string>('');

  private searchSubject = new Subject<string>();
  private searchSub: Subscription;

  constructor() {
    this.searchSub = this.searchSubject
      .pipe(
        debounceTime(this.debounce()),
        distinctUntilChanged()
      )
      .subscribe((val) => {
        this.searchChange.emit(val);
      });
  }

  onModelChange(val: string): void {
    this.query.set(val);
    this.searchSubject.next(val);
  }

  clearSearch(): void {
    this.query.set('');
    this.searchSubject.next('');
    this.searchChange.emit('');
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }
}
