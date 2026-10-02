import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterModule } from '@angular/router';
import { PracticeService } from '../../services/practice.service';
import { PracticeHistoryItem } from '../../models';

@Component({
  selector: 'app-practice-history',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './practice-history.component.html',
  styleUrl: './practice-history.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeHistoryComponent implements OnInit {
  private readonly practiceService = inject(PracticeService);

  readonly history = signal<PracticeHistoryItem[]>([]);
  readonly isLoading = signal(true);
  readonly currentPage = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = signal(10);
  readonly searchQuery = signal('');

  readonly totalAttempts = computed(() => this.totalElements());

  readonly avgAccuracy = computed(() => {
    const list = this.history();
    if (!list.length) return 0;
    const sum = list.reduce((acc, item) => acc + (item.accuracyPercent || 0), 0);
    return Math.round((sum / list.length) * 10) / 10;
  });

  readonly highestScore = computed(() => {
    const list = this.history();
    if (!list.length) return 0;
    return Math.max(...list.map((i) => i.obtainedMarks || 0));
  });

  readonly totalSolved = computed(() => {
    const list = this.history();
    return list.reduce((acc, item) => acc + (item.totalQuestions || 0), 0);
  });

  readonly filteredHistory = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const items = this.history();
    if (!q) return items;
    return items.filter(
      (item) =>
        (item.practiceSetName && item.practiceSetName.toLowerCase().includes(q)) ||
        item.sessionId.toLowerCase().includes(q)
    );
  });

  ngOnInit(): void {
    this.loadHistory(0);
  }

  loadHistory(page: number): void {
    this.isLoading.set(true);
    this.practiceService.getHistory(page, this.pageSize()).subscribe({
      next: (res) => {
        this.history.set(res.content || []);
        this.totalElements.set(res.totalElements || 0);
        this.currentPage.set(res.number || 0);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.loadHistory(event.pageIndex);
  }

  onSearchChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
  }

  clearSearch(): void {
    this.searchQuery.set('');
  }
}
