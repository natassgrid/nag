import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterModule } from '@angular/router';
import { PracticeService } from '../../services/practice.service';
import { PracticeHistoryItem } from '../../models';

@Component({
  selector: 'app-practice-history',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatProgressSpinnerModule
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
  
  readonly displayedColumns = ['setName', 'date', 'score', 'accuracy', 'action'];

  ngOnInit(): void {
    this.loadHistory(0);
  }

  loadHistory(page: number): void {
    this.isLoading.set(true);
    this.practiceService.getHistory(page, this.pageSize()).subscribe({
      next: (res) => {
        this.history.set(res.content);
        this.totalElements.set(res.totalElements);
        this.currentPage.set(res.number);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.loadHistory(event.pageIndex);
  }
}
